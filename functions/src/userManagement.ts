import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Assuming admin.initializeApp() is called in index.ts
const db = admin.firestore();

export const findUsersByUsername = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated to search.");
  }
  const usernameQuery = data.usernameQuery as string;

  if (!usernameQuery || typeof usernameQuery !== "string" || usernameQuery.trim().length < 2) {
    throw new functions.https.HttpsError("invalid-argument", "Username query must be at least 2 characters long.");
  }

  const trimmedQuery = usernameQuery.trim();
  functions.logger.info(`Searching for usernames starting with: '${trimmedQuery}' by user ${context.auth.uid}`);

  try {
    const usersRef = db.collection("users");
    const snapshot = await usersRef
      .where("username", ">=", trimmedQuery)
      .where("username", "<=", trimmedQuery + "\uf8ff")
      .limit(10)
      .get();

    if (snapshot.empty) {
      functions.logger.info(`No users found for query: '${trimmedQuery}'`);
      return [];
    }

    const results = snapshot.docs.map((doc) => {
      const userData = doc.data();
      return {
        userId: doc.id,
        username: userData.username,
        displayName: userData.displayName || userData.username,
        photoUrl: userData.profileImageUrl || null,
      };
    });
    functions.logger.info(`Found ${results.length} users for query: '${trimmedQuery}'`);
    return results;
  } catch (error) {
    functions.logger.error(`Error searching users by username for query '${trimmedQuery}' by user ${context.auth.uid}:`, error);
    throw new functions.https.HttpsError("internal", "An error occurred while searching for users. Please try again.");
  }
});


export const checkPhoneNumbersSockStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }
  const phoneNumbers = data.phoneNumbers as string[];

  if (!phoneNumbers || !Array.isArray(phoneNumbers) || phoneNumbers.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "A list of phone numbers is required.");
  }
  // Reduced limit for iterative lookups; this is still not ideal for very large lists.
  if (phoneNumbers.length > 20) {
    throw new functions.https.HttpsError("invalid-argument", "Too many phone numbers provided; maximum 20 per request for this check.");
  }

  functions.logger.info(`User ${context.auth.uid} checking status for ${phoneNumbers.length} phone numbers.`);

  const results: Record<string, {isUser: boolean, userId?: string, displayName?: string, username?: string, error?: string}> = {};

  // WARNING: Iterative lookups are NOT efficient for large lists.
  // Consider alternative strategies for production systems if this function is heavily used with many numbers.
  for (const phoneNumber of phoneNumbers) {
    // Basic E.164 format validation (very simple check)
    if (!/^\+[1-9]\d{1,14}$/.test(phoneNumber)) {
        functions.logger.warn(`Invalid phone number format received: ${phoneNumber}`);
        results[phoneNumber] = {isUser: false, error: "invalid_format"};
        continue;
    }
    try {
      const userSnapshot = await db.collection("users").where("phoneNumber", "==", phoneNumber).limit(1).get();
      if (!userSnapshot.empty) {
        const userData = userSnapshot.docs[0].data();
        results[phoneNumber] = {
          isUser: true,
          userId: userSnapshot.docs[0].id,
          displayName: userData.displayName || userData.username,
          username: userData.username,
        };
      } else {
        results[phoneNumber] = {isUser: false};
      }
    } catch (error) {
      functions.logger.error(`Error checking phone number ${phoneNumber} for user ${context.auth.uid}:`, error);
      results[phoneNumber] = {isUser: false, error: "lookup_failed"};
    }
  }
  return results;
});
