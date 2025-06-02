import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { HttpsError } from "firebase-functions/v1/auth";

// Assuming admin.initializeApp() is called in index.ts
const db = admin.firestore();
const auth = admin.auth();

// --- Helper Functions ---
async function getUserDataForDenormalization(userId: string): Promise<{ name: string | null, photoUrl: string | null, username?: string }> {
  try {
    const userRecord = await auth.getUser(userId);
    const userDoc = await db.collection("users").doc(userId).get();
    const username = userDoc.exists ? userDoc.data()?.username : null;
    return {name: userRecord.displayName || null, photoUrl: userRecord.photoURL || null, username: username};
  } catch (error) {
    functions.logger.error(`[getUserDataForDenormalization] Error fetching user data for ${userId}:`, error);
    return {name: "User", photoUrl: null, username: "user"}; // Provide fallbacks
  }
}

async function getGroupDataForDenormalization(groupId: string): Promise<{ name: string | null, imageUrl: string | null }> {
  try {
    const groupDoc = await db.collection("groups").doc(groupId).get();
    if (!groupDoc.exists) {
      functions.logger.warn(`[getGroupDataForDenormalization] Group ${groupId} not found.`);
      return {name: "A Group", imageUrl: null}; // Provide fallbacks
    }
    const groupData = groupDoc.data();
    return {name: groupData?.name || "A Group", imageUrl: groupData?.profileImageUrl || groupData?.bannerImageUrl || null};
  } catch (error) {
    functions.logger.error(`[getGroupDataForDenormalization] Error fetching group data for ${groupId}:`, error);
    return {name: "A Group", imageUrl: null};
  }
}

function isValidRole(role: string): boolean {
    return ["OWNER", "ADMIN", "MODERATOR", "MEMBER"].includes(role.toUpperCase());
}

// --- Callable Cloud Functions ---

export const sendGroupInvitation = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
  const inviterId = context.auth.uid;
  const {
    groupId, inviteeId, inviteeEmail, inviteeUsername, inviteePhoneNumber,
    identifierType, roleToAssign: rawRoleToAssign, originatingManagedLinkId,
  } = data;

  if (!groupId || typeof groupId !== "string") throw new HttpsError("invalid-argument", "Valid groupId is required.");
  if (!identifierType || typeof identifierType !== "string") throw new HttpsError("invalid-argument", "Valid identifierType is required.");

  const roleToAssign = (typeof rawRoleToAssign === "string" && isValidRole(rawRoleToAssign)) ? rawRoleToAssign.toUpperCase() : "MEMBER";
  if (roleToAssign === "OWNER") throw new HttpsError("invalid-argument", "Cannot invite user as Owner.");


  const inviterMemberRef = db.collection("groups").doc(groupId).collection("members").doc(inviterId);
  const inviterMemberDoc = await inviterMemberRef.get();
  if (!inviterMemberDoc.exists) throw new HttpsError("permission-denied", "Inviter is not a member of this group.");
  const inviterRole = inviterMemberDoc.data()?.role as string;
  if (inviterRole !== "ADMIN" && inviterRole !== "OWNER") throw new HttpsError("permission-denied", "Only Admins or Owners can send invitations.");

  const newInvitationRef = db.collection("invitations").doc();
  const inviterData = await getUserDataForDenormalization(inviterId);
  const groupData = await getGroupDataForDenormalization(groupId);

  const invitationData: any = {
    invitationId: newInvitationRef.id, type: identifierType, groupId,
    groupName: groupData.name, groupImageUrl: groupData.imageUrl,
    inviterId, inviterName: inviterData.name, inviterPhotoUrl: inviterData.photoUrl,
    roleToAssign, status: "PENDING",
    createdAt: admin.firestore.FieldValue.serverTimestamp(), expiresAt: null,
    isUsernameResolved: null, resolutionError: null, originatingManagedLinkId: originatingManagedLinkId || null,
  };

  let resolvedInviteeId: string | null = null;

  switch (identifierType) {
    case "DIRECT_USER_ID":
      if (!inviteeId || typeof inviteeId !== "string") throw new HttpsError("invalid-argument", "inviteeId required for DIRECT_USER_ID type.");
      invitationData.inviteeId = inviteeId; resolvedInviteeId = inviteeId;
      break;
    case "EMAIL":
      if (!inviteeEmail || typeof inviteeEmail !== "string") throw new HttpsError("invalid-argument", "inviteeEmail required for EMAIL type.");
      invitationData.inviteeEmail = inviteeEmail.toLowerCase();
      try {
        const userRecord = await auth.getUserByEmail(inviteeEmail);
        invitationData.inviteeId = userRecord.uid; resolvedInviteeId = userRecord.uid;
      } catch (e: any) { if (e.code !== "auth/user-not-found") {functions.logger.warn("Error checking email during invite:", e); } }
      break;
    case "USERNAME":
      if (!inviteeUsername || typeof inviteeUsername !== "string") throw new HttpsError("invalid-argument", "inviteeUsername required for USERNAME type.");
      invitationData.inviteeUsername = inviteeUsername;
      const usersSnapshot = await db.collection("users").where("username", "==", inviteeUsername).limit(1).get();
      if (!usersSnapshot.empty) {
        resolvedInviteeId = usersSnapshot.docs[0].id;
        invitationData.inviteeId = resolvedInviteeId; invitationData.isUsernameResolved = true;
      } else {
        invitationData.isUsernameResolved = false; invitationData.resolutionError = "Username not found.";
      }
      break;
    case "PHONE_CONTACT":
      if (!inviteePhoneNumber || typeof inviteePhoneNumber !== "string") throw new HttpsError("invalid-argument", "inviteePhoneNumber required for PHONE_CONTACT type.");
      invitationData.inviteePhoneNumber = inviteePhoneNumber; // Assume E.164
      try {
        const userRecord = await auth.getUserByPhoneNumber(inviteePhoneNumber);
        invitationData.inviteeId = userRecord.uid; resolvedInviteeId = userRecord.uid;
      } catch (e: any) { if (e.code !== "auth/user-not-found") {functions.logger.warn("Error checking phone during invite:", e);}}
      break;
    default: throw new HttpsError("invalid-argument", `Invalid identifierType: ${identifierType}`);
  }

  if (resolvedInviteeId) {
    const q = db.collection("invitations").where("groupId", "==", groupId).where("inviteeId", "==", resolvedInviteeId).where("status", "==", "PENDING").limit(1);
    if (!(await q.get()).empty) throw new HttpsError("already-exists", "An active PENDING invitation already exists for this user to this group.");
  } else if (identifierType === "EMAIL") {
    const q = db.collection("invitations").where("groupId", "==", groupId).where("inviteeEmail", "==", invitationData.inviteeEmail).where("status", "==", "PENDING").limit(1);
    if (!(await q.get()).empty) throw new HttpsError("already-exists", "An active PENDING invitation already exists for this email to this group.");
  } else if (identifierType === "PHONE_CONTACT") {
     const q = db.collection("invitations").where("groupId", "==", groupId).where("inviteePhoneNumber", "==", invitationData.inviteePhoneNumber).where("status", "==", "PENDING").limit(1);
    if (!(await q.get()).empty) throw new HttpsError("already-exists", "An active PENDING invitation already exists for this phone number to this group.");
  }


  await newInvitationRef.set(invitationData);
  functions.logger.info(`Invitation ${newInvitationRef.id} created by ${inviterId} for group ${groupId}. Target type: ${identifierType}.`);
  return {success: true, invitationId: newInvitationRef.id, message: "Invitation sent successfully."};
});

export const sendSMSToNonUserAndGenerateInvite = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
  const inviterId = context.auth.uid;
  const {phoneNumber, groupId, groupName: providedGroupName, inviterName: providedInviterName} = data;

  if (!phoneNumber || typeof phoneNumber !== "string" || !/^\+[1-9]\d{1,14}$/.test(phoneNumber)) throw new HttpsError("invalid-argument", "Valid E.164 phoneNumber is required.");
  if (!groupId || typeof groupId !== "string") throw new HttpsError("invalid-argument", "Valid groupId is required.");
  if (!providedGroupName || typeof providedGroupName !== "string") throw new HttpsError("invalid-argument", "Valid groupName is required.");


  const inviterMemberRef = db.collection("groups").doc(groupId).collection("members").doc(inviterId);
  const inviterMemberDoc = await inviterMemberRef.get();
  if (!inviterMemberDoc.exists) throw new HttpsError("permission-denied", "Inviter is not a member of this group.");
  const inviterRole = inviterMemberDoc.data()?.role as string;
  if (inviterRole !== "ADMIN" && inviterRole !== "OWNER") throw new HttpsError("permission-denied", "Only Admins or Owners can send SMS invitations.");


  const inviterDisplayName = providedInviterName || (await getUserDataForDenormalization(inviterId)).name || "A friend";
  const newInvitationRef = db.collection("invitations").doc();
  // IMPORTANT: Replace 'your-app-domain.com/invite/' with your actual deep link / URL structure
  const inviteLink = `https://your-app-domain.com/invite/${newInvitationRef.id}`;
  const smsBody = `${inviterDisplayName} has invited you to join the group "${providedGroupName}" on SockApp! Join here: ${inviteLink}`;

  const inviterData = await getUserDataForDenormalization(inviterId);
  const groupDenormData = await getGroupDataForDenormalization(groupId);

  await newInvitationRef.set({
    invitationId: newInvitationRef.id, type: "PHONE_CONTACT", groupId,
    groupName: groupDenormData.name || providedGroupName, groupImageUrl: groupDenormData.imageUrl,
    inviterId, inviterName: inviterData.name, inviterPhotoUrl: inviterData.photoUrl,
    inviteePhoneNumber: phoneNumber, roleToAssign: "MEMBER", status: "PENDING",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
  functions.logger.info(`SMS Invitation ${newInvitationRef.id} created by ${inviterId} for group ${groupId} to phone ${phoneNumber}.`);
  return {success: true, inviteLink, smsBody, phoneNumber, invitationId: newInvitationRef.id};
});


export const generateManagedGroupInviteLink = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
  const {groupId, roleToAssign: rawRoleToAssign, maxUses, expiresAt: expiresAtISO} = data;
  const createdBy = context.auth.uid;

  if (!groupId || typeof groupId !== "string") throw new HttpsError("invalid-argument", "Valid groupId is required.");
  const roleToAssign = (typeof rawRoleToAssign === "string" && isValidRole(rawRoleToAssign)) ? rawRoleToAssign.toUpperCase() : "MEMBER";
  if (roleToAssign === "OWNER") throw new HttpsError("invalid-argument", "Cannot generate invite link for Owner role.");


  const memberDoc = await db.collection("groups").doc(groupId).collection("members").doc(createdBy).get();
  if (!memberDoc.exists || (memberDoc.data()?.role !== "ADMIN" && memberDoc.data()?.role !== "OWNER")) {
    throw new HttpsError("permission-denied", "Only Admins or Owners can generate group invite links.");
  }

  const linkId = db.collection("groups").doc(groupId).collection("managedInviteLinks").doc().id;
  let code = linkId.substring(0, 8); // Simple code generation
  // TODO: Add a check for code uniqueness within the group and regenerate if collision (rare for this method).

  const groupData = await getGroupDataForDenormalization(groupId);

  const newLinkData: any = {
    linkId, code, groupId, groupName: groupData.name, createdBy,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    uses: 0, roleToAssign, isActive: true,
  };
  if (maxUses && Number(maxUses) > 0) newLinkData.maxUses = Number(maxUses); else newLinkData.maxUses = null;
  if (expiresAtISO && typeof expiresAtISO === "string") {
      try { newLinkData.expiresAt = admin.firestore.Timestamp.fromDate(new Date(expiresAtISO)); }
      catch(e) { functions.logger.warn("Invalid expiresAtISO provided:", expiresAtISO); newLinkData.expiresAt = null; }
  } else { newLinkData.expiresAt = null; }


  await db.collection("groups").doc(groupId).collection("managedInviteLinks").doc(linkId).set(newLinkData);
  functions.logger.info(`Managed link ${linkId} (code ${code}) created for group ${groupId} by ${createdBy}.`);
  // IMPORTANT: Replace 'your-app-domain.com/join/' with your actual deep link / URL structure
  return {success: true, linkId, code, inviteUrl: `https://your-app-domain.com/join/${code}`};
});

export const revokeManagedGroupInviteLink = functions.https.onCall(async (data, context) => { /* ... (previous robust version) ... */
    if (!context.auth) throw new HttpsError("unauthenticated", "User must be authenticated.");
    const {groupId, linkId} = data;
    const requesterId = context.auth.uid;

    if (!groupId || typeof groupId !== "string" || !linkId || typeof linkId !== "string") {
        throw new HttpsError("invalid-argument", "groupId and linkId are required.");
    }
    const memberDoc = await db.collection("groups").doc(groupId).collection("members").doc(requesterId).get();
    if (!memberDoc.exists || (memberDoc.data()?.role !== "ADMIN" && memberDoc.data()?.role !== "OWNER")) {
        throw new HttpsError("permission-denied", "Only Admins or Owners can revoke links.");
    }
    const linkRef = db.collection("groups").doc(groupId).collection("managedInviteLinks").doc(linkId);
    if (!(await linkRef.get()).exists) throw new HttpsError("not-found", "Managed invite link not found.");

    await linkRef.update({isActive: false});
    functions.logger.info(`Managed link ${linkId} for group ${groupId} revoked by ${requesterId}.`);
    return {success: true, message: "Invite link revoked successfully."};
});

export const processInviteLink = functions.https.onCall(async (data, context) => { /* ... (previous robust version, ensure transaction for uses increment and new invite creation) ... */
    if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
    const {inviteCode} = data;
    const acceptorUid = context.auth.uid;

    if (!inviteCode || typeof inviteCode !== "string") throw new HttpsError("invalid-argument", "inviteCode is required.");

    const managedLinksQuery = db.collectionGroup("managedInviteLinks").where("code", "==", inviteCode).where("isActive", "==", true).limit(1);
    const managedLinksSnapshot = await managedLinksQuery.get();

    if (managedLinksSnapshot.empty) throw new HttpsError("not-found", "This invite link is invalid, expired, or no longer active.");

    const managedLinkDoc = managedLinksSnapshot.docs[0];
    const managedLink = managedLinkDoc.data();

    if (managedLink.expiresAt && managedLink.expiresAt.toMillis() < Date.now()) {
        await managedLinkDoc.ref.update({isActive: false});
        throw new HttpsError("deadline-exceeded", "This invite link has expired.");
    }
    if (managedLink.maxUses != null && managedLink.uses >= managedLink.maxUses) {
        await managedLinkDoc.ref.update({isActive: false});
        throw new HttpsError("resource-exhausted", "This invite link has reached its maximum number of uses.");
    }

    // Check if user already has a PENDING invite from this specific managed link
    const existingSpecificInviteQuery = db.collection("invitations")
        .where("originatingManagedLinkId", "==", managedLink.linkId)
        .where("inviteeId", "==", acceptorUid)
        .where("status", "==", "PENDING")
        .limit(1);
    const existingSpecificInviteSnapshot = await existingSpecificInviteQuery.get();
    if (!existingSpecificInviteSnapshot.empty) {
        functions.logger.info(`User ${acceptorUid} already has a PENDING specific invite ${existingSpecificInviteSnapshot.docs[0].id} from managed link ${managedLink.linkId}.`);
        return {success: true, invitationId: existingSpecificInviteSnapshot.docs[0].id, message: "Existing invitation found. Please confirm acceptance."};
    }


    const newInvitationRef = db.collection("invitations").doc();
    const inviterData = await getUserDataForDenormalization(managedLink.createdBy);
    const groupDenormData = await getGroupDataForDenormalization(managedLink.groupId);

    const specificInvitationData = {
        invitationId: newInvitationRef.id, type: "DIRECT_USER_ID", groupId: managedLink.groupId,
        groupName: managedLink.groupName || groupDenormData.name, groupImageUrl: groupDenormData.imageUrl,
        inviterId: managedLink.createdBy, inviterName: inviterData.name, inviterPhotoUrl: inviterData.photoUrl,
        inviteeId: acceptorUid, roleToAssign: managedLink.roleToAssign || "MEMBER", status: "PENDING",
        createdAt: admin.firestore.FieldValue.serverTimestamp(), originatingManagedLinkId: managedLink.linkId,
    };

    await db.runTransaction(async (transaction) => {
        const linkDocInTransaction = await transaction.get(managedLinkDoc.ref);
        const currentUses = linkDocInTransaction.data()?.uses || 0;
        if (linkDocInTransaction.data()?.maxUses != null && currentUses >= linkDocInTransaction.data()?.maxUses) {
             transaction.update(managedLinkDoc.ref, {isActive: false}); // Deactivate if uses just met max
             throw new HttpsError("resource-exhausted", "This invite link just reached its maximum number of uses.");
        }
        transaction.set(newInvitationRef, specificInvitationData);
        transaction.update(managedLinkDoc.ref, {uses: admin.firestore.FieldValue.increment(1)});
    });
    functions.logger.info(`Specific invitation ${newInvitationRef.id} created for user ${acceptorUid} from managed link ${managedLink.linkId}.`);
    return {success: true, invitationId: newInvitationRef.id, message: "Invite link processed. Please confirm acceptance."};
 });

export const revokeInvitationByAdmin = functions.https.onCall(async (data, context) => { /* ... (previous robust version) ... */
    if (!context.auth) throw new HttpsError("unauthenticated", "User must be authenticated.");
    const {invitationId, groupId} = data;
    const requesterId = context.auth.uid;

    if(!invitationId || typeof invitationId !== "string" || !groupId || typeof groupId !== "string") {
        throw new HttpsError("invalid-argument", "invitationId and groupId are required.");
    }
    const memberDoc = await db.collection("groups").doc(groupId).collection("members").doc(requesterId).get();
    if (!memberDoc.exists || (memberDoc.data()?.role !== "ADMIN" && memberDoc.data()?.role !== "OWNER")) {
        throw new HttpsError("permission-denied", "Only group admins/owners can revoke invitations for their group.");
    }
    const inviteRef = db.collection("invitations").doc(invitationId);
    const inviteDoc = await inviteRef.get();
    if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation not found.");
    if (inviteDoc.data()?.groupId !== groupId) throw new HttpsError("permission-denied", "Invitation does not belong to the specified group.");
    if (inviteDoc.data()?.status !== "PENDING") throw new HttpsError("failed-precondition", "Only PENDING invitations can be revoked by an admin.");

    await inviteRef.update({status: "REVOKED", processedAt: admin.firestore.FieldValue.serverTimestamp()});
    functions.logger.info(`Invitation ${invitationId} for group ${groupId} revoked by admin ${requesterId}.`);
    return {success: true, message: "Invitation has been successfully revoked."};
});

export const matchNewUserToInvite = functions.auth.user().onCreate(async (user) => { /* ... (previous robust version) ... */
    const newUserUid = user.uid;
    const newUserEmail = user.email;
    const newUserPhoneNumber = user.phoneNumber;

    if (!newUserEmail && !newUserPhoneNumber) {
        functions.logger.info(`New user ${newUserUid} has no email or phone number. Skipping invite matching.`);
        return null;
    }
    functions.logger.info(`Attempting to match invites for new user ${newUserUid} (Email: ${newUserEmail}, Phone: ${newUserPhoneNumber})`);

    const batch = db.batch();
    let invitesMatchedCount = 0;
    const now = admin.firestore.FieldValue.serverTimestamp();

    const pendingInvitesRef = db.collection("invitations").where("status", "==", "PENDING").where("inviteeId", "==", null);

    if (newUserEmail) {
        const emailQuery = pendingInvitesRef.where("inviteeEmail", "==", newUserEmail);
        const emailInvitesSnapshot = await emailQuery.get();
        emailInvitesSnapshot.forEach((doc) => {
            functions.logger.info(`Matching email invite ${doc.id} to new user ${newUserUid}`);
            batch.update(doc.ref, {inviteeId: newUserUid, processedAt: now /* Optionally update status or just link UID */});
            invitesMatchedCount++;
        });
    }

    if (newUserPhoneNumber) {
        const phoneQuery = pendingInvitesRef.where("inviteePhoneNumber", "==", newUserPhoneNumber);
        const phoneInvitesSnapshot = await phoneQuery.get();
        phoneInvitesSnapshot.forEach((doc) => {
            // Avoid double-matching if email already matched this invite and set inviteeId
            if (doc.data().inviteeId === newUserUid && doc.data().inviteeEmail === newUserEmail) return;

            functions.logger.info(`Matching phone invite ${doc.id} to new user ${newUserUid}`);
            batch.update(doc.ref, {inviteeId: newUserUid, processedAt: now});
            invitesMatchedCount++;
        });
    }

    if (invitesMatchedCount > 0) {
        try {
            await batch.commit();
            functions.logger.info(`Successfully matched ${invitesMatchedCount} pending invitations for new user ${newUserUid}.`);
            // TODO: Send an in-app notification to the user about these matched invites.
        } catch (error) {
            functions.logger.error(`Error committing matched invites for user ${newUserUid}:`, error);
        }
    } else {
        functions.logger.info(`No pending invitations found to match for new user ${newUserUid}.`);
    }
    return null;
});

// --- Full implementations for acceptInvitation, declineInvitation ---
// (These were placeholders before, now providing more complete logic based on typical needs)

export const acceptInvitation = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
  const acceptorUid = context.auth.uid;
  const acceptorEmail = context.auth.token.email;
  const {invitationId} = data;

  if (!invitationId || typeof invitationId !== "string") throw new HttpsError("invalid-argument", "invitationId is required.");

  const inviteRef = db.collection("invitations").doc(invitationId);
  const acceptorUserDocRef = db.collection("users").doc(acceptorUid);
  const acceptorData = await getUserDataForDenormalization(acceptorUid);


  await db.runTransaction(async (transaction) => {
    const inviteDoc = await transaction.get(inviteRef);
    if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation not found.");
    const invitation = inviteDoc.data();
    if (!invitation) throw new HttpsError("internal", "Could not read invitation data.");

    if (invitation.status !== "PENDING") throw new HttpsError("failed-precondition", `Invitation is already ${invitation.status}.`);
    if (invitation.expiresAt && invitation.expiresAt.toMillis() < Date.now()) {
      transaction.update(inviteRef, {status: "EXPIRED", processedAt: admin.firestore.FieldValue.serverTimestamp()});
      throw new HttpsError("deadline-exceeded", "Invitation has expired.");
    }

    let canAccept = false;
    if (invitation.inviteeId && invitation.inviteeId === acceptorUid) canAccept = true;
    else if (invitation.inviteeEmail && invitation.inviteeEmail.toLowerCase() === acceptorEmail?.toLowerCase() && !invitation.inviteeId) {
      canAccept = true; transaction.update(inviteRef, {inviteeId: acceptorUid});
    } else if (invitation.inviteePhoneNumber && invitation.inviteePhoneNumber === (await auth.getUser(acceptorUid)).phoneNumber && !invitation.inviteeId) {
      canAccept = true; transaction.update(inviteRef, {inviteeId: acceptorUid});
    } else if (!invitation.inviteeId && !invitation.inviteeEmail && !invitation.inviteeUsername && !invitation.inviteePhoneNumber) {
      // This case implies a generic link processed into a specific invite, or an old invite structure.
      functions.logger.warn(`Invitation ${invitationId} has no specific invitee target. Allowing accept by ${acceptorUid}.`);
      canAccept = true; transaction.update(inviteRef, {inviteeId: acceptorUid});
    }

    if (!canAccept) throw new HttpsError("permission-denied", "This invitation is not for you or has been claimed.");

    const groupRef = db.collection("groups").doc(invitation.groupId);
    const groupMemberRef = groupRef.collection("members").doc(acceptorUid);
    const groupDoc = await transaction.get(groupRef);
    if (!groupDoc.exists) {
      transaction.update(inviteRef, {status: "EXPIRED", resolutionError: "Group does not exist.", processedAt: admin.firestore.FieldValue.serverTimestamp()});
      throw new HttpsError("not-found", "The invited group no longer exists.");
    }

    const existingMemberDoc = await transaction.get(groupMemberRef);
    if (existingMemberDoc.exists) {
      functions.logger.info(`User ${acceptorUid} already member of group ${invitation.groupId}. Marking invite ${invitationId} accepted.`);
    } else {
      transaction.set(groupMemberRef, {
        userId: acceptorUid, role: invitation.roleToAssign || "MEMBER",
        joinedAt: admin.firestore.FieldValue.serverTimestamp(),
        displayName: acceptorData.name || acceptorData.username, photoUrl: acceptorData.photoUrl,
      });
      transaction.update(groupRef, {memberCount: admin.firestore.FieldValue.increment(1)});
    }
    transaction.update(acceptorUserDocRef, {joinedGroupIds: admin.firestore.FieldValue.arrayUnion(invitation.groupId)});
    transaction.update(inviteRef, {status: "ACCEPTED", inviteeId: acceptorUid, processedAt: admin.firestore.FieldValue.serverTimestamp()});
  });
  functions.logger.info(`Invitation ${invitationId} accepted by user ${acceptorUid} for group ${data.groupId}.`);
  return {success: true, message: "Invitation accepted successfully."};
});

export const declineInvitation = functions.https.onCall(async (data, context) => { /* ... (similar structure to accept, but simpler logic) ... */
    if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required.");
    const declinerUid = context.auth.uid;
    const declinerEmail = context.auth.token.email;
    const {invitationId} = data;

    if (!invitationId || typeof invitationId !== "string") throw new HttpsError("invalid-argument", "invitationId is required.");
    const inviteRef = db.collection("invitations").doc(invitationId);

    await db.runTransaction(async (transaction) => {
        const inviteDoc = await transaction.get(inviteRef);
        if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation not found.");
        const invitation = inviteDoc.data();
        if (!invitation) throw new HttpsError("internal", "Could not read invitation data.");

        if (invitation.status !== "PENDING") throw new HttpsError("failed-precondition", `Invitation is already ${invitation.status}.`);
        if (invitation.expiresAt && invitation.expiresAt.toMillis() < Date.now()) {
            transaction.update(inviteRef, {status: "EXPIRED", processedAt: admin.firestore.FieldValue.serverTimestamp()});
            throw new HttpsError("deadline-exceeded", "Invitation has expired.");
        }

        let canDecline = false;
        if (invitation.inviteeId && invitation.inviteeId === declinerUid) canDecline = true;
        else if (invitation.inviteeEmail && invitation.inviteeEmail.toLowerCase() === declinerEmail?.toLowerCase() && !invitation.inviteeId) {
            canDecline = true; transaction.update(inviteRef, {inviteeId: declinerUid});
        } else if (invitation.inviteePhoneNumber && invitation.inviteePhoneNumber === (await auth.getUser(declinerUid)).phoneNumber && !invitation.inviteeId) {
            canDecline = true; transaction.update(inviteRef, {inviteeId: declinerUid});
        } else if (!invitation.inviteeId && !invitation.inviteeEmail && !invitation.inviteeUsername && !invitation.inviteePhoneNumber) {
             functions.logger.warn(`Invitation ${invitationId} has no specific invitee target. Allowing decline by ${declinerUid}.`);
             canDecline = true; transaction.update(inviteRef, {inviteeId: declinerUid});
        }
        if (!canDecline) throw new HttpsError("permission-denied", "This invitation is not for you.");

        transaction.update(inviteRef, {status: "DECLINED", inviteeId: declinerUid, processedAt: admin.firestore.FieldValue.serverTimestamp()});
    });
    functions.logger.info(`Invitation ${invitationId} declined by user ${declinerUid}.`);
    return {success: true, message: "Invitation declined."};
});

// processBlindUsernameInvite is largely covered by sendGroupInvitation's username resolution logic.
// If it needs to be a separate callable for some reason (e.g. admin tool to force resolve), it can be kept.
// For now, assuming sendGroupInvitation handles the initial resolution attempt.
export const processBlindUsernameInvite = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new HttpsError("unauthenticated", "Authentication required (typically admin/system).");
    // This function's permissions should be tightly controlled if exposed directly.
    const {invitationId} = data;
    if (!invitationId || typeof invitationId !== "string") throw new HttpsError("invalid-argument", "invitationId is required.");

    const inviteRef = db.collection("invitations").doc(invitationId);
    const inviteDoc = await inviteRef.get();
    if (!inviteDoc.exists) throw new HttpsError("not-found", "Invitation not found.");
    const invitation = inviteDoc.data();

    if (!invitation || invitation.type !== "USERNAME" || invitation.status !== "PENDING" || invitation.isUsernameResolved === true) {
      throw new HttpsError("failed-precondition", "Not an unresolved username invite.");
    }
    if (!invitation.inviteeUsername) throw new HttpsError("internal", "Missing inviteeUsername.");

    const usersSnapshot = await db.collection("users").where("username", "==", invitation.inviteeUsername).limit(1).get();
    if (!usersSnapshot.empty) {
      const foundUser = usersSnapshot.docs[0];
      await inviteRef.update({inviteeId: foundUser.id, isUsernameResolved: true, resolutionError: null});
      functions.logger.info(`Username invite ${invitationId} resolved for ${invitation.inviteeUsername} to ${foundUser.id}.`);
      return {success: true, message: `Username resolved to user ${foundUser.id}.`};
    } else {
      await inviteRef.update({isUsernameResolved: false, resolutionError: `Username '${invitation.inviteeUsername}' not found.`});
      throw new HttpsError("not-found", `Username '${invitation.inviteeUsername}' not found.`);
    }
});
