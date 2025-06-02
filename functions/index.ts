import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize admin SDK only once across all function deployments
if (admin.apps.length === 0) {
  admin.initializeApp();
}
const db = admin.firestore();

/**
 * Checks if a username is available.
 * @param data Contains `username` to check.
 * @param context The function's call context, containing auth information.
 * @returns An object `{ available: boolean, message?: string }`.
 * @throws `invalid-argument` if username is invalid.
 * @throws `internal` if an unexpected error occurs.
 */
export const checkUsernameAvailability = functions.https.onCall(async (data, context) => {
  const username = data.username as string;

  // Basic validation
  if (!username || typeof username !== "string" || username.length < 3 || username.length > 20) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Username must be a string between 3 and 20 characters.",
    );
  }

  // Username character validation (allow alphanumeric and underscores)
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Username can only contain alphanumeric characters and underscores.",
    );
  }

  // Optional: Check against a list of reserved names
  const reservedUsernames = ["admin", "root", "moderator", "support", "firebase"];
  if (reservedUsernames.includes(username.toLowerCase())) {
    return {available: false, message: "This username is reserved."};
  }

  try {
    const usersRef = db.collection("users");
    // Case-insensitive check: Firestore queries are case-sensitive by default.
    // To do a truly case-insensitive check efficiently, you should store a
    // normalized (e.g., lowercase) version of the username and query against that.
    // For this example, we'll stick to a case-sensitive check for simplicity,
    // or rely on client to normalize IF that's the chosen strategy.
    // For a more robust solution, consider querying a `username_lowercase` field.
    const snapshot = await usersRef.where("username", "==", username).limit(1).get();

    if (snapshot.empty) {
      return {available: true};
    } else {
      return {available: false, message: "Username is already taken."};
    }
  } catch (error) {
    console.error("Error checking username availability:", username, error);
    throw new functions.https.HttpsError(
        "internal",
        "An unexpected error occurred while checking username. Please try again.",
        // (error as Error).message // Optionally include original error message for debugging
    );
  }
});

/**
 * Deletes a user's account from Firebase Authentication and their Firestore document.
 * @param data Unused for this function.
 * @param context The function's call context, containing auth information.
 * @returns An object `{ success: boolean, message: string }`.
 * @throws `unauthenticated` if the user is not authenticated.
 * @throws `permission-denied` if re-authentication is required.
 * @throws `internal` if an unexpected error occurs.
 */
export const deleteUserAccount = functions.https.onCall(async (data, context) => {
  // 1. Authentication Check: Ensure the user is authenticated.
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
    );
  }

  const uid = context.auth.uid;
  const userEmail = context.auth.token.email || "unknown_email"; // For logging

  console.log(`Attempting to delete account for UID: ${uid}, Email: ${userEmail}`);

  try {
    // 2. Delete Firestore User Document
    // Consider the order: if Auth deletion fails, Firestore doc is already gone.
    // Some prefer to mark as "to_be_deleted" then have a separate cleanup.
    // For simplicity, direct deletion here.
    const userDocRef = db.collection("users").doc(uid);
    await userDocRef.delete();
    console.log(`Firestore document deleted for UID: ${uid}`);

    // 3. Delete Firebase Auth User
    // This is the critical part that makes the user unable to log in.
    await admin.auth().deleteUser(uid);
    console.log(`Firebase Auth user deleted for UID: ${uid}`);

    // 4. (Important) Delete other user-related data if applicable
    // Example: Firebase Storage files (this is a placeholder, implement actual paths)
    // const bucket = admin.storage().bucket(); // Default bucket
    // try {
    //   await bucket.deleteFiles({prefix: `userUploads/${uid}/`});
    //   console.log(`Storage files potentially deleted for UID: ${uid} under userUploads/${uid}/`);
    // } catch (storageError) {
    //   console.error(`Error deleting storage files for UID: ${uid}`, storageError);
    //   // Non-fatal for this function's success, but should be logged and monitored.
    // }

    // Add deletion for other data stores (Realtime Database, other Firestore collections, etc.)

    return {success: true, message: "User account deleted successfully."};
  } catch (error: any) {
    console.error(`Error deleting user account for UID: ${uid}`, error);

    // Handle specific errors, e.g., if re-authentication is required for sensitive operations
    if (error.code === "auth/requires-recent-login") {
      throw new functions.https.HttpsError(
          "permission-denied",
          "This operation is sensitive and requires recent authentication. Please sign in again and retry.",
      );
    }

    // It's good practice to check if the user document or auth user still exists
    // to understand the state if an error occurs mid-process.
    // For example, if userDocRef.delete() succeeded but admin.auth().deleteUser(uid) failed.

    throw new functions.https.HttpsError(
        "internal",
        "Failed to delete user account. Please try again or contact support if the issue persists.",
        // error.message // Optionally include original error details for client-side debugging if safe
    );
  }
});

// It's good practice to also have a package.json for your functions
// and specify the Node engine, dependencies (firebase-admin, firebase-functions).
// Example (place in functions/package.json):
/*
{
  "name": "functions",
  "description": "Cloud Functions for Firebase",
  "scripts": {
    "lint": "eslint . --ext .js,.ts",
    "build": "tsc",
    "serve": "npm run build && firebase emulators:start --only functions",
    "shell": "npm run build && firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "logs": "firebase functions:log"
  },
  "engines": {
    "node": "18" // Or "20", "16" - choose an LTS version
  },
  "main": "lib/index.js", // tsc output directory
  "dependencies": {
    "firebase-admin": "^11.11.1", // Use latest or appropriate version
    "firebase-functions": "^4.7.0" // Use latest or appropriate version
  },
  "devDependencies": {
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "eslint": "^8.15.0",
    "eslint-plugin-import": "^2.27.5",
    "typescript": "^5.0.0" // Use latest or appropriate version
  },
  "private": true
}
*/

// And a tsconfig.json (place in functions/tsconfig.json):
/*
{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2017", // Or newer like es2020, es2021
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  },
  "compileOnSave": true,
  "include": [
    "src" // if your .ts files are in functions/src/
    // or "index.ts" if it's directly in functions/
  ],
  "exclude": [
    "node_modules",
    "lib"
  ]
}
*/
// If index.ts is directly in functions/, change "include" in tsconfig.json to ["index.ts"] or ["."]
// and "main" in package.json to "lib/index.js" still assuming outDir is "lib".
// If your index.ts is in functions/src/index.ts, then "include": ["src"] is correct.
// For this generation, I'm assuming index.ts is in functions/ so "include": ["."] or specific files.
// I will assume the tsconfig.json would be:
// {
//   "compilerOptions": {
//     "module": "commonjs",
//     "noImplicitReturns": true,
//     "noUnusedLocals": true,
//     "outDir": "lib",
//     "sourceMap": true,
//     "strict": true,
//     "target": "es2020",
//     "esModuleInterop": true,
//     "resolveJsonModule": true,
//     "skipLibCheck": true,
//     "forceConsistentCasingInFileNames": true
//   },
//   "compileOnSave": true,
//   "include": [
//     "index.ts" // Specifically including the root index.ts
//   ],
//   "exclude": [
//     "node_modules",
//     "lib"
//   ]
// }
