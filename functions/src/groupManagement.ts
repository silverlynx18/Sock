import * as functions from "firebase-functions";
import *.admin from "firebase-admin";

// Ensure Firebase Admin is initialized (typically in index.ts or a shared config)
// if (admin.apps.length === 0) {
//   admin.initializeApp();
// }
const db = admin.firestore();
const auth = admin.auth();

// --- Helper Functions ---

/**
 * Transfers ownership to the oldest admin or, if no admins, the oldest member.
 * Returns the UID of the new owner, or null if no suitable member is found.
 */
async function transferOwnershipInternal(
  groupId: string,
  currentOwnerId: string,
  transaction: admin.firestore.Transaction,
): Promise<string | null> {
  const membersRef = db.collection("groups").doc(groupId).collection("members");

  // Try to find the oldest admin (excluding current owner)
  let newOwnerQuery = membersRef
    .where("role", "==", "ADMIN")
    .orderBy("joinedAt", "asc")
    .limit(1);
  let newOwnerSnapshot = await transaction.get(newOwnerQuery);

  if (newOwnerSnapshot.empty) {
    // If no admins, find the oldest member (excluding current owner)
    newOwnerQuery = membersRef
      .orderBy("joinedAt", "asc")
      .limit(2); // Fetch 2 in case the oldest is the current owner
    newOwnerSnapshot = await transaction.get(newOwnerQuery);

    const potentialNewOwners = newOwnerSnapshot.docs.filter(
      (doc) => doc.id !== currentOwnerId,
    );
    if (potentialNewOwners.length > 0) {
      const newOwnerRef = potentialNewOwners[0].ref;
      transaction.update(newOwnerRef, {role: "OWNER"});
      functions.logger.info(
        `Ownership of group ${groupId} transferred to member ${newOwnerRef.id} (was oldest member).`,
      );
      return newOwnerRef.id;
    }
  } else {
    const newOwnerRef = newOwnerSnapshot.docs[0].ref;
    transaction.update(newOwnerRef, {role: "OWNER"});
    functions.logger.info(
      `Ownership of group ${groupId} transferred to admin ${newOwnerRef.id}.`,
    );
    return newOwnerRef.id;
  }

  functions.logger.warn(
    `Group ${groupId}: Could not find a suitable member to transfer ownership from ${currentOwnerId}.`,
  );
  return null; // No suitable member found to transfer ownership
}

/**
 * Deletes a group if it becomes empty.
 * This should be called within the same transaction as member removal.
 */
async function deleteEmptyGroupInternal(
  groupId: string,
  groupRef: admin.firestore.DocumentReference,
  transaction: admin.firestore.Transaction,
  skipMemberCountCheck = false, // Use if member count is not yet updated in this transaction
): Promise<boolean> {
  if (!skipMemberCountCheck) {
    const groupDoc = await transaction.get(groupRef);
    const memberCount = groupDoc.data()?.memberCount || 0;
    if (memberCount > 0) {
      return false; // Group is not empty
    }
  }

  // If member count is 0 (or check is skipped and we assume it will be 0)
  // It's generally safer to delete subcollections with a dedicated recursive delete function
  // as part of the deleteGroup callable function rather than here transactionally for large subcollections.
  // However, for a member subcollection that's just been emptied, this might be okay.
  // For now, we'll just delete the group doc. Recursive deletion of members should be in `deleteGroup`.

  transaction.delete(groupRef);
  functions.logger.info(
    `Group ${groupId} was empty or last member left, and has been deleted.`,
  );
  return true;
}

// --- Callable Cloud Functions ---

export const createGroup = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to create a group.",
    );
  }
  const {uid} = context.auth;
  const {
    name,
    description,
    isPublic,
    profileImageUrl,
    bannerImageUrl,
    creatorDisplayName, // Passed from client, from auth.currentUser.displayName
    creatorPhotoUrl, // Passed from client, from auth.currentUser.photoUrl
  } = data;

  if (!name || typeof name !== "string" || name.trim().length === 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Group name is required.",
    );
  }
  if (name.length > 100) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Group name must be 100 characters or less.",
    );
  }
  if (description && description.length > 1000) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Group description must be 1000 characters or less.",
    );
  }

  const newGroupRef = db.collection("groups").doc();
  const ownerMemberRef = newGroupRef.collection("members").doc(uid);
  const userDocRef = db.collection("users").doc(uid);

  await db.runTransaction(async (transaction) => {
    // 1. Create the Group document
    transaction.set(newGroupRef, {
      groupId: newGroupRef.id,
      name: name.trim(),
      description: description?.trim() || null,
      isPublic: !!isPublic,
      profileImageUrl: profileImageUrl || null,
      bannerImageUrl: bannerImageUrl || null,
      creatorId: uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      memberCount: 1, // Starts with the owner
    });

    // 2. Add the creator as the OWNER in the members subcollection
    transaction.set(ownerMemberRef, {
      userId: uid,
      role: "OWNER",
      joinedAt: admin.firestore.FieldValue.serverTimestamp(),
      displayName: creatorDisplayName || "Group Owner",
      photoUrl: creatorPhotoUrl || null,
    });

    // 3. Add groupId to the user's list of joined groups (denormalization)
    transaction.update(userDocRef, {
      joinedGroupIds: admin.firestore.FieldValue.arrayUnion(newGroupRef.id),
    });
  });

  return {groupId: newGroupRef.id, message: "Group created successfully."};
});


export const handleUserLeaveOrRemove = functions.https.onCall(
  async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated.",
      );
    }
    const currentUserId = context.auth.uid;
    const {groupId, userIdToRemove, action} = data; // action: 'leave' or 'remove'

    if (!groupId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "groupId is required.",
      );
    }

    const groupRef = db.collection("groups").doc(groupId);
    const memberToRemoveId = action === "leave" ? currentUserId : userIdToRemove;

    if (!memberToRemoveId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "User ID to remove/leave is required.",
      );
    }

    const memberToRemoveRef = groupRef.collection("members").doc(memberToRemoveId);
    const userDocRefForRemovedMember = db.collection("users").doc(memberToRemoveId);

    await db.runTransaction(async (transaction) => {
      const groupDoc = await transaction.get(groupRef);
      const memberToRemoveDoc = await transaction.get(memberToRemoveRef);

      if (!groupDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Group not found.");
      }
      if (!memberToRemoveDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "Member to remove not found in group.",
        );
      }

      const memberToRemoveRole = memberToRemoveDoc.data()?.role as string;
      const currentMemberCount = groupDoc.data()?.memberCount || 0;

      if (action === "remove") {
        if (currentUserId === memberToRemoveId) {
          throw new functions.https.HttpsError(
            "invalid-argument",
            "Cannot use 'remove' action for self; use 'leave'.",
          );
        }
        const currentUserMemberRef = groupRef.collection("members").doc(currentUserId);
        const currentUserMemberDoc = await transaction.get(currentUserMemberRef);
        if (!currentUserMemberDoc.exists) {
          throw new functions.https.HttpsError(
            "permission-denied",
            "Current user is not a member of this group.",
          );
        }
        const currentUserRole = currentUserMemberDoc.data()?.role as string;
        // Basic permission check (Owner > Admin > Moderator > Member)
        const roles = {OWNER: 3, ADMIN: 2, MODERATOR: 1, MEMBER: 0};
        if (
          roles[currentUserRole as keyof typeof roles] <=
          roles[memberToRemoveRole as keyof typeof roles] &&
          currentUserRole !== "OWNER" // Owner can remove anyone (except other owners if multi-owner was a thing)
        ) {
          throw new functions.https.HttpsError(
            "permission-denied",
            "Insufficient permissions to remove this member.",
          );
        }
        if (memberToRemoveRole === "OWNER" && currentUserRole !== "OWNER") {
             throw new functions.https.HttpsError(
            "permission-denied",
            "Only an Owner can remove another Owner (requires ownership transfer first).",
          );
        }
      }

      // Handle Owner leaving or being removed
      if (memberToRemoveRole === "OWNER") {
        if (currentMemberCount <= 1) {
          // This is the last member (and they are owner), so delete the group.
          functions.logger.info(`Owner ${memberToRemoveId} is the last member of group ${groupId}. Deleting group.`);
          // The deleteGroup function should handle recursive deletion of members subcollection.
          // For now, we just delete the group doc and member doc.
          // Proper cleanup of member subcollection should be ensured.
          // For simplicity here, assuming deleteEmptyGroupInternal can handle it for 1 member.
          await deleteEmptyGroupInternal(groupId, groupRef, transaction, true); // Skip member count check as it's about to be 0
        } else {
          // Transfer ownership
          const newOwnerId = await transferOwnershipInternal(groupId, memberToRemoveId, transaction);
          if (!newOwnerId) {
            // This case should be rare if there are other members. Could imply data inconsistency.
            // Or if all other members are somehow ineligible.
            // Fallback: delete the group if no owner can be assigned.
            functions.logger.error(`Critical: Could not transfer ownership for group ${groupId} after owner ${memberToRemoveId} left/was removed. Attempting to delete group.`);
            // This would ideally call the full deleteGroup logic, but that's hard transactionally.
            // For now, we'll proceed with member removal and leave group as ownerless (problematic) or delete.
            // Let's choose to delete if no new owner found.
            transaction.delete(groupRef); // Delete group doc. Subcollection cleanup needed.
            functions.logger.warn(`Group ${groupId} deleted as no new owner could be assigned.`);
          } else {
             functions.logger.info(`Ownership transferred in group ${groupId} to ${newOwnerId}.`);
          }
        }
      }

      // Common logic for both leave and remove if group is not being deleted with the owner
      if (groupDoc.exists) { // Check if group wasn't deleted in owner-leaving scenario
          transaction.delete(memberToRemoveRef);
          transaction.update(groupRef, {
            memberCount: admin.firestore.FieldValue.increment(-1),
          });
      }

      // Update user's joinedGroupIds list
      transaction.update(userDocRefForRemovedMember, {
        joinedGroupIds: admin.firestore.FieldValue.arrayRemove(groupId),
      });

      // If the group becomes empty after this removal (and wasn't the owner leaving and deleting the group)
      if (groupDoc.exists() && currentMemberCount - 1 === 0 && memberToRemoveRole !== "OWNER") {
          await deleteEmptyGroupInternal(groupId, groupRef, transaction);
      }
    });

    return {
      success: true,
      message: `User ${memberToRemoveId} has been ${action === "leave" ? "left" : "removed from"} the group ${groupId}.`,
    };
  },
);

export const deleteGroup = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to delete a group.",
    );
  }
  const {uid} = context.auth;
  const {groupId} = data;

  if (!groupId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "groupId is required.",
    );
  }

  const groupRef = db.collection("groups").doc(groupId);
  const memberRef = groupRef.collection("members").doc(uid);

  await db.runTransaction(async (transaction) => {
    const groupDoc = await transaction.get(groupRef);
    const memberDoc = await transaction.get(memberRef);

    if (!groupDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Group not found.");
    }
    if (!memberDoc.exists || memberDoc.data()?.role !== "OWNER") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Only the group owner can delete the group.",
      );
    }

    // 1. Delete the group document (will be part of the transaction)
    transaction.delete(groupRef);
    // Note: Actual deletion of subcollections transactionally is not directly supported for large subcollections.
    // The members subcollection will be deleted recursively outside the transaction for safety.
  });

  // 2. Recursively delete the 'members' subcollection (outside the transaction)
  // This is crucial. Firebase CLI has a command for this, or use a helper.
  // For simplicity, we'll use the admin SDK's recursive delete (available in recent versions).
  // This is an async operation and not part of the atomic transaction above.
  // Consider implications if this part fails after transaction commits.
  // A more robust solution might involve a two-step process or background task for cleanup.
  const membersCollectionRef = groupRef.collection("members");
  await admin.firestore().recursiveDelete(membersCollectionRef);
  functions.logger.info(`Recursively deleted members subcollection for group ${groupId}.`);


  // 3. Update users' joinedGroupIds (best effort, can be slow for many users)
  // This part is often handled differently:
  // - Client-side: When a user tries to access a group and it's not found, remove from local list.
  // - Background task: A separate function that cleans up stale group IDs from user profiles.
  // For this example, we'll skip direct update of all users' joinedGroupIds due to complexity and performance.
  // It's assumed clients or other mechanisms will handle stale references.

  return {message: `Group ${groupId} and its members deleted successfully.`};
});


// TODO: Add updateGroupMemberRole Cloud Function
export const updateGroupMemberRole = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const currentUserId = context.auth.uid;
    const { groupId, memberUserId, newRole } = data; // newRole is a string like "ADMIN"

    if (!groupId || !memberUserId || !newRole) {
        throw new functions.https.HttpsError("invalid-argument", "groupId, memberUserId, and newRole are required.");
    }

    const groupRef = db.collection("groups").doc(groupId);
    const memberToUpdateRef = groupRef.collection("members").doc(memberUserId);
    const currentUserMemberRef = groupRef.collection("members").doc(currentUserId);

    // Validate newRole string
    const validRoles = ["OWNER", "ADMIN", "MODERATOR", "MEMBER"];
    if (!validRoles.includes(newRole.toUpperCase())) {
        throw new functions.https.HttpsError("invalid-argument", "Invalid role specified.");
    }
    const targetRoleEnum = newRole.toUpperCase();


    await db.runTransaction(async (transaction) => {
        const memberToUpdateDoc = await transaction.get(memberToUpdateRef);
        const currentUserMemberDoc = await transaction.get(currentUserMemberRef);

        if (!memberToUpdateDoc.exists) {
            throw new functions.https.HttpsError("not-found", "Member to update not found.");
        }
        if (!currentUserMemberDoc.exists) {
            throw new functions.https.HttpsError("permission-denied", "Current user is not a member of this group.");
        }

        const currentRoleOfUserPerformingAction = currentUserMemberDoc.data()?.role as string;
        const currentRoleOfMemberBeingUpdated = memberToUpdateDoc.data()?.role as string;

        // Permission checks (example logic, refine as needed based on GroupRole enum)
        // Simplified: Owner can change any role (except assign new Owner directly here, use transfer)
        // Admin can change Moderator/Member roles.
        const rolesLevel = { OWNER: 3, ADMIN: 2, MODERATOR: 1, MEMBER: 0 };

        if (currentRoleOfUserPerformingAction === "OWNER") {
            if (targetRoleEnum === "OWNER" && memberUserId !== currentUserId) {
                 throw new functions.https.HttpsError("invalid-argument", "Ownership transfer must be handled differently.");
            }
            // Owner can set any other role
        } else if (currentRoleOfUserPerformingAction === "ADMIN") {
            if (rolesLevel[targetRoleEnum as keyof typeof rolesLevel] >= rolesLevel.ADMIN) {
                throw new functions.https.HttpsError("permission-denied", "Admins cannot promote to Admin or Owner.");
            }
            if (rolesLevel[currentRoleOfMemberBeingUpdated as keyof typeof rolesLevel] >= rolesLevel.ADMIN) {
                 throw new functions.https.HttpsError("permission-denied", "Admins cannot change role of other Admins or Owners.");
            }
        } else { // Moderator or Member
            throw new functions.https.HttpsError("permission-denied", "Insufficient permission to change roles.");
        }

        transaction.update(memberToUpdateRef, { role: targetRoleEnum });
    });

    return { success: true, message: `Role for member ${memberUserId} updated to ${targetRoleEnum}.` };
});

// TODO: Add joinGroup Cloud Function (for public groups, if not handled by client-side rules)
export const joinGroup = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
    }
    const userId = context.auth.uid;
    const { groupId } = data;

    if (!groupId) {
        throw new functions.https.HttpsError("invalid-argument", "groupId is required.");
    }

    const groupRef = db.collection("groups").doc(groupId);
    const memberRef = groupRef.collection("members").doc(userId);
    const userRef = db.collection("users").doc(userId);

    // Fetch user display name and photo URL (optional, but good for denormalization)
    const userProfile = await auth.getUser(userId); // Or from Firestore user profile if more details needed

    await db.runTransaction(async (transaction) => {
        const groupDoc = await transaction.get(groupRef);
        if (!groupDoc.exists) {
            throw new functions.https.HttpsError("not-found", "Group not found.");
        }
        if (groupDoc.data()?.isPublic !== true) {
            throw new functions.https.HttpsError("permission-denied", "This group is not public.");
        }

        const memberDoc = await transaction.get(memberRef);
        if (memberDoc.exists) {
            throw new functions.https.HttpsError("already-exists", "User is already a member of this group.");
        }

        transaction.set(memberRef, {
            userId: userId,
            role: "MEMBER",
            joinedAt: admin.firestore.FieldValue.serverTimestamp(),
            displayName: userProfile.displayName || "New Member",
            photoUrl: userProfile.photoURL || null,
        });
        transaction.update(groupRef, { memberCount: admin.firestore.FieldValue.increment(1) });
        transaction.update(userRef, { joinedGroupIds: admin.firestore.FieldValue.arrayUnion(groupId) });
    });

    return { success: true, message: "Successfully joined the group." };
});
