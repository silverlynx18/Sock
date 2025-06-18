import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v1/auth"; // Correct import for HttpsError

// Assuming admin.initializeApp() is called in index.ts
const db = admin.firestore();
const auth = admin.auth();

// --- Helper Functions ---

/**
 * Transfers group ownership to the oldest admin, or if no other admins, the oldest member.
 * This function is intended to be called within a Firestore transaction.
 * @param {string} groupId The ID of the group.
 * @param {string} currentOwnerId The UID of the current owner who is leaving/being removed.
 * @param {admin.firestore.Transaction} transaction The Firestore transaction object.
 * @return {Promise<string | null>} The UID of the new owner, or null if no suitable replacement found.
 */
async function transferOwnershipInternal(
  groupId: string,
  currentOwnerId: string,
  transaction: admin.firestore.Transaction,
): Promise<string | null> {
  const membersRef = db.collection("groups").doc(groupId).collection("members");
  functions.logger.info(`[transferOwnershipInternal] Group ${groupId}: Attempting to transfer ownership from ${currentOwnerId}.`);

  // Try to find the oldest admin (excluding current owner)
  const adminQuery = membersRef
    .where("role", "==", "ADMIN")
    .orderBy("joinedAt", "asc");
  const adminSnapshot = await transaction.get(adminQuery);
  const potentialAdminOwners = adminSnapshot.docs.filter((doc) => doc.id !== currentOwnerId);

  if (potentialAdminOwners.length > 0) {
    const newOwnerRef = potentialAdminOwners[0].ref;
    transaction.update(newOwnerRef, {role: "OWNER"});
    functions.logger.info(`[transferOwnershipInternal] Group ${groupId}: Ownership transferred to admin ${newOwnerRef.id}.`);
    return newOwnerRef.id;
  }

  // If no other admins, find the oldest member (excluding current owner)
  const memberQuery = membersRef
    .orderBy("joinedAt", "asc")
    .limit(2); // Fetch 2 in case the oldest is the current owner or another potential candidate
  const memberSnapshot = await transaction.get(memberQuery);
  const potentialMemberOwners = memberSnapshot.docs.filter((doc) => doc.id !== currentOwnerId);

  if (potentialMemberOwners.length > 0) {
    const newOwnerRef = potentialMemberOwners[0].ref;
    transaction.update(newOwnerRef, {role: "OWNER"});
    functions.logger.info(`[transferOwnershipInternal] Group ${groupId}: No other admins. Ownership transferred to member ${newOwnerRef.id} (oldest member).`);
    return newOwnerRef.id;
  }

  functions.logger.warn(`[transferOwnershipInternal] Group ${groupId}: No suitable member found to transfer ownership from ${currentOwnerId}.`);
  return null;
}

/**
 * Deletes a group document if its member count implies it's empty or will be empty.
 * Intended to be called within a transaction.
 * @param {string} groupId The ID of the group.
 * @param {admin.firestore.DocumentReference} groupRef Reference to the group document.
 * @param {admin.firestore.Transaction} transaction The Firestore transaction.
 * @param {boolean} [skipMemberCountCheck=false] If true, deletes the group regardless of current memberCount (e.g., if count is about to become zero).
 * @return {Promise<boolean>} True if group was deleted, false otherwise.
 */
async function deleteEmptyGroupInternal(
  groupId: string,
  groupRef: admin.firestore.DocumentReference,
  transaction: admin.firestore.Transaction,
  skipMemberCountCheck = false,
): Promise<boolean> {
  functions.logger.info(`[deleteEmptyGroupInternal] Group ${groupId}: Checking if group should be deleted.`);
  if (!skipMemberCountCheck) {
    const groupDoc = await transaction.get(groupRef);
    if (!groupDoc.exists) { // Group already deleted
        functions.logger.warn(`[deleteEmptyGroupInternal] Group ${groupId}: Document does not exist, cannot check member count.`);
        return false;
    }
    const memberCount = groupDoc.data()?.memberCount || 0;
    if (memberCount > 0) {
      functions.logger.info(`[deleteEmptyGroupInternal] Group ${groupId}: Not empty (memberCount: ${memberCount}). Not deleting.`);
      return false;
    }
  }

  functions.logger.info(`[deleteEmptyGroupInternal] Group ${groupId}: Deleting group document as it is considered empty.`);
  transaction.delete(groupRef);
  // Note: Recursive deletion of subcollections (like members) should be handled by the calling function (`deleteGroup`)
  // as it's complex to do robustly and fully transactionally here for large subcollections.
  return true;
}

// --- Callable Cloud Functions ---

/**
 * Creates a new group, setting the caller as the OWNER.
 * Updates the user's `joinedGroupIds` list.
 * @param data.name Name of the group.
 * @param data.description Optional description.
 * @param data.isPublic Boolean visibility.
 * @param data.profileImageUrl Optional profile image URL.
 * @param data.bannerImageUrl Optional banner image URL.
 * @param data.creatorDisplayName Display name of the creator (from client).
 * @param data.creatorPhotoUrl Photo URL of the creator (from client).
 * @return {Promise<{groupId: string, message: string}>} The new group's ID.
 */
export const createGroup = functions.https.onCall(async (data, context) => { /* ... (existing code with refined HttpsError messages) ... */ });

/**
 * Handles a user leaving a group or an admin/owner removing a user from a group.
 * Manages member count, ownership transfer if an owner leaves/is removed,
 * and potential group deletion if it becomes empty.
 * @param data.groupId ID of the group.
 * @param data.action "leave" or "remove".
 * @param data.userIdToRemove UID of the user to remove (if action is "remove").
 * @return {Promise<{success: boolean, message: string}>}
 */
export const handleUserLeaveOrRemove = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
    const currentUserId = context.auth.uid;
    const {groupId, userIdToRemove, action} = data;

    if (!groupId || typeof groupId !== "string") throw new HttpsError("invalid-argument", "Valid groupId is required.");
    if (!action || (action !== "leave" && action !== "remove")) throw new HttpsError("invalid-argument", "Action must be 'leave' or 'remove'.");

    const memberToRemoveId = action === "leave" ? currentUserId : userIdToRemove;
    if (!memberToRemoveId || typeof memberToRemoveId !== "string") throw new HttpsError("invalid-argument", "Valid user ID to remove/leave is required.");

    const groupRef = db.collection("groups").doc(groupId);
    const memberToRemoveRef = groupRef.collection("members").doc(memberToRemoveId);
    const userDocRefForRemovedMember = db.collection("users").doc(memberToRemoveId);

    functions.logger.info(`[handleUserLeaveOrRemove] User ${currentUserId} attempting action '${action}' on user ${memberToRemoveId} in group ${groupId}.`);

    try {
        await db.runTransaction(async (transaction) => {
            const groupDoc = await transaction.get(groupRef);
            const memberToRemoveDoc = await transaction.get(memberToRemoveRef);

            if (!groupDoc.exists) throw new HttpsError("not-found", `Group ${groupId} not found.`);
            if (!memberToRemoveDoc.exists) throw new HttpsError("not-found", `User ${memberToRemoveId} is not a member of group ${groupId}.`);

            const memberToRemoveRole = memberToRemoveDoc.data()?.role as string;
            const currentMemberCount = groupDoc.data()?.memberCount || 0;

            if (action === "remove") {
                if (currentUserId === memberToRemoveId) throw new HttpsError("invalid-argument", "Cannot use 'remove' on self; use 'leave'.");

                const currentUserMemberDoc = await transaction.get(groupRef.collection("members").doc(currentUserId));
                if (!currentUserMemberDoc.exists) throw new HttpsError("permission-denied", "Requesting user is not a member of this group.");

                const currentUserRole = currentUserMemberDoc.data()?.role as string;
                const roles = {OWNER: 3, ADMIN: 2, MODERATOR: 1, MEMBER: 0} as const;

                if (roles[currentUserRole as keyof typeof roles] <= roles[memberToRemoveRole as keyof typeof roles] && currentUserRole !== "OWNER") {
                    throw new HttpsError("permission-denied", "Insufficient permissions to remove this member.");
                }
                if (memberToRemoveRole === "OWNER" && currentUserRole !== "OWNER") { // Only owner can remove another owner (conceptual, usually transfer first)
                    throw new HttpsError("permission-denied", "Only an Owner can remove another Owner.");
                }
            }

            let groupDeletedDuringOperation = false;
            if (memberToRemoveRole === "OWNER") {
                if (currentMemberCount <= 1) {
                    functions.logger.info(`[handleUserLeaveOrRemove] Owner ${memberToRemoveId} is last member of group ${groupId}. Group will be deleted.`);
                    // Transactionally delete group doc. Subcollection cleanup handled by deleteGroup CF.
                    transaction.delete(groupRef);
                    groupDeletedDuringOperation = true;
                } else {
                    const newOwnerId = await transferOwnershipInternal(groupId, memberToRemoveId, transaction);
                    if (!newOwnerId) {
                        functions.logger.error(`[handleUserLeaveOrRemove] CRITICAL: Could not transfer ownership for group ${groupId}. Deleting group as fallback.`);
                        transaction.delete(groupRef); // Fallback: delete group if no owner
                        groupDeletedDuringOperation = true;
                    }
                }
            }

            if (!groupDeletedDuringOperation) {
                transaction.delete(memberToRemoveRef);
                transaction.update(groupRef, {memberCount: admin.firestore.FieldValue.increment(-1)});
                 // If group becomes empty *after this removal* and wasn't deleted due to owner leaving
                if (currentMemberCount - 1 === 0) {
                   functions.logger.info(`[handleUserLeaveOrRemove] Group ${groupId} became empty after removing ${memberToRemoveId}. Deleting group.`);
                   await deleteEmptyGroupInternal(groupId, groupRef, transaction, true); // skip check as count is now 0
                   groupDeletedDuringOperation = true;
                }
            }

            // Always remove group from user's list, even if group is deleted
            transaction.update(userDocRefForRemovedMember, {joinedGroupIds: admin.firestore.FieldValue.arrayRemove(groupId)});

            // If group was deleted, ensure other members also have it removed from their joinedGroupIds.
            // This is complex for a single transaction. Usually handled by deleteGroup's broader cleanup.
            // For now, this function primarily handles the specific user's departure/removal.
        });
    } catch (error) {
        functions.logger.error(`[handleUserLeaveOrRemove] Failed action '${action}' on user ${memberToRemoveId} in group ${groupId} by ${currentUserId}:`, error);
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", "An error occurred. Please try again.");
    }

    return {success: true, message: `User ${memberToRemoveId} successfully ${action === "leave" ? "left" : "removed from"} the group.`};
});

/**
 * Deletes a group and its members subcollection.
 * Caller must be the group OWNER.
 * @param data.groupId ID of the group to delete.
 * @return {Promise<{message: string}>} Success message.
 */
export const deleteGroup = functions.https.onCall(async (data, context) => { /* ... (existing code with refined HttpsError messages, added logging) ... */ });

/**
 * Updates a group's details (name, description, visibility, image URLs).
 * Caller must be an ADMIN or OWNER of the group.
 * @param data.groupId ID of the group to update.
 * @param data.name New name.
 * @param data.description New description.
 * @param data.isPublic New visibility.
 * @param data.profileImageUrl New profile image URL.
 * @param data.bannerImageUrl New banner image URL.
 * @return {Promise<{success: boolean, message: string}>}
 */
export const updateGroupDetails = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
    const {groupId, name, description, isPublic, profileImageUrl, bannerImageUrl} = data;
    const requesterId = context.auth.uid;

    if (!groupId || typeof groupId !== "string") throw new HttpsError("invalid-argument", "Valid groupId is required.");
    if (!name || typeof name !== "string" || name.trim().length === 0 || name.length > 100) {
        throw new HttpsError("invalid-argument", "Group name must be between 1 and 100 characters.");
    }
    if (description && typeof description !== "string" || description && description.length > 1000) {
        throw new HttpsError("invalid-argument", "Group description must be a string and 1000 characters or less.");
    }
    if (typeof isPublic !== "boolean") throw new HttpsError("invalid-argument", "isPublic must be a boolean.");

    const groupRef = db.collection("groups").doc(groupId);
    const memberRef = groupRef.collection("members").doc(requesterId);

    try {
        await db.runTransaction(async (transaction) => {
            const memberDoc = await transaction.get(memberRef);
            if (!memberDoc.exists) throw new HttpsError("permission-denied", "Requester is not a member of this group.");
            const userRole = memberDoc.data()?.role as string;
            if (userRole !== "ADMIN" && userRole !== "OWNER") throw new HttpsError("permission-denied", "Only Admins or Owners can update group details.");

            transaction.update(groupRef, {
                name: name.trim(),
                description: description?.trim() || null,
                isPublic: isPublic,
                profileImageUrl: profileImageUrl?.trim() || null,
                bannerImageUrl: bannerImageUrl?.trim() || null,
                // lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(), // Optional
            });
        });
        functions.logger.info(`Group ${groupId} details updated by ${requesterId}.`);
        return {success: true, message: "Group details updated successfully."};
    } catch (error) {
        functions.logger.error(`Error updating group ${groupId} by ${requesterId}:`, error);
        if (error instanceof HttpsError) throw error;
        throw new HttpsError("internal", "Failed to update group details. Please try again.");
    }
});


/**
 * Updates a member's role within a group.
 * Caller must be ADMIN/OWNER and follow role hierarchy rules.
 * @param data.groupId ID of the group.
 * @param data.memberUserId UID of the member whose role is to be changed.
 * @param data.newRole The new role string (e.g., "ADMIN", "MEMBER").
 * @return {Promise<{success: boolean, message: string}>}
 */
export const updateGroupMemberRole = functions.https.onCall(async (data, context) => { /* ... (existing code, ensure robust permission checks and logging) ... */ });

/**
 * Allows a user to join a public group.
 * @param data.groupId ID of the public group to join.
 * @return {Promise<{success: boolean, message: string}>}
 */
export const joinGroup = functions.https.onCall(async (data, context) => { /* ... (existing code, ensure robust checks and logging) ... */ });


// --- Re-paste full implementations of createGroup, deleteGroup, updateGroupMemberRole, joinGroup with comments & refined errors ---
// For brevity, I'm showing only the new/modified `updateGroupDetails` and refined `handleUserLeaveOrRemove` structure.
// The other functions would follow similar patterns of input validation, permission checks, try/catch, logging, and HttpsError.
// For example, createGroup:
// export const createGroup = functions.https.onCall(async (data, context) => {
//   if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
//   const {uid} = context.auth;
//   const { name, description, isPublic, profileImageUrl, bannerImageUrl, creatorDisplayName, creatorPhotoUrl } = data;

//   if (!name || typeof name !== "string" || name.trim().length === 0 || name.length > 100) {
//     throw new HttpsError("invalid-argument", "Group name must be between 1 and 100 characters.");
//   }
//   // ... other validations ...
//   functions.logger.info(`User ${uid} attempting to create group: ${name}`);
//   // ... (rest of the logic from previous version) ...
//   try {
//      // ... transaction ...
//      functions.logger.info(`Group ${newGroupRef.id} created successfully by ${uid}.`);
//      return {groupId: newGroupRef.id, message: "Group created successfully."};
//   } catch (error) {
//      functions.logger.error(`Error creating group '${name}' by user ${uid}:`, error);
//      if (error instanceof HttpsError) throw error;
//      throw new HttpsError("internal", "Could not create group. Please try again.");
//   }
// });

// deleteGroup needs to be very careful about cleaning up `joinedGroupIds` on all members.
// This is often best done with a batched approach or a separate cleanup task for large groups.
// export const deleteGroup = functions.https.onCall(async (data, context) => {
//   // ... (permission checks) ...
//   try {
//     // ... transaction to delete groupRef ...
//     // ... admin.firestore().recursiveDelete(membersCollectionRef) ...
//     // TODO: Implement robust cleanup of user.joinedGroupIds for all ex-members. This is critical.
//     // This might involve querying all members, then batch updating each user doc.
//     // Or, client handles stale group IDs gracefully.
//     functions.logger.info(`Group ${groupId} deleted by owner ${uid}. Member subcollection deletion initiated.`);
//     return {message: `Group ${groupId} deletion process started.`};
//   } catch (error) {
//      functions.logger.error(`Error deleting group '${data.groupId}' by user ${context.auth?.uid}:`, error);
//      if (error instanceof HttpsError) throw error;
//      throw new HttpsError("internal", "Could not delete group. Please try again.");
//   }
// });
