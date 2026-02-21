import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Assuming admin.initializeApp() is called in index.ts
const db = admin.firestore();

// Default status to revert to when a status expires.
// This should match one of the AppPresetStatus.id values.
const DEFAULT_EXPIRED_STATUS_ID = "online";

/**
 * Scheduled Cloud Function to automatically clear expired user statuses.
 * Runs periodically (e.g., every hour) to check and reset:
 * 1. Global statuses on User documents (`globalStatusExpiresAt`).
 * 2. Group-specific statuses in `users/{userId}/groupStatusDetails` subcollections (`expiresAt`).
 *
 * Expired statuses are reset to a default (e.g., "online") and custom text/icons are cleared.
 * Group-specific statuses are deleted to allow fallback to global status.
 *
 * @param {functions.EventContext} context - The event context.
 * @return {Promise<null>} A promise that resolves when the function completes.
 */
export const clearExpiredStatuses = functions.pubsub
  .schedule("every 60 minutes") // Configurable schedule (e.g., "every 15 minutes", "every 1 hours")
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    const batchSize = 200; // Firestore batch write limit is 500 operations; process in smaller chunks.
    let operationsCommitted = 0;

    functions.logger.info(`[clearExpiredStatuses] Starting scheduled job. Current time: ${now.toDate().toISOString()}`);

    // 1. Clean up expired global statuses on User documents
    try {
      const expiredGlobalUsersQuery = db
        .collection("users")
        .where("globalStatusExpiresAt", "<=", now);

      let lastGlobalSnapshot: admin.firestore.QueryDocumentSnapshot | null = null;
      let globalHasMore = true;

      functions.logger.info("[clearExpiredStatuses] Starting global status cleanup phase.");
      while (globalHasMore) {
        let currentGlobalQuery = expiredGlobalUsersQuery;
        if (lastGlobalSnapshot) {
          currentGlobalQuery = currentGlobalQuery.startAfter(lastGlobalSnapshot);
        }
        currentGlobalQuery = currentGlobalQuery.limit(batchSize);

        const snapshot = await currentGlobalQuery.get();
        if (snapshot.empty) {
          globalHasMore = false;
          break;
        }
        lastGlobalSnapshot = snapshot.docs[snapshot.docs.length - 1];

        let globalBatch = db.batch(); // Initialize batch inside loop for potentially multiple batches
        let globalDocsInBatch = 0;

        snapshot.forEach((doc) => {
          functions.logger.log(`[clearExpiredStatuses] Processing user ${doc.id} for expired global status.`);
          globalBatch.update(doc.ref, {
            activeStatusId: DEFAULT_EXPIRED_STATUS_ID,
            globalCustomStatusText: null,
            globalCustomStatusIconKey: null,
            globalStatusExpiresAt: null, // Clear the expiry
          });
          globalDocsInBatch++;
          if (globalDocsInBatch >= batchSize) { // Commit if batch is full
            globalBatch.commit()
                .then(results => { operationsCommitted += results.length; functions.logger.info(`[clearExpiredStatuses] Committed ${results.length} global status updates in a batch.`);})
                .catch(err => functions.logger.error("[clearExpiredStatuses] Error committing global status batch:", err));
            globalBatch = db.batch(); // Start a new batch
            globalDocsInBatch = 0;
          }
        });

        if (globalDocsInBatch > 0) { // Commit any remaining operations in the last batch
          await globalBatch.commit();
          operationsCommitted += globalDocsInBatch; // Assuming commit() returns array of WriteResult or number
          functions.logger.info(`[clearExpiredStatuses] Committed final ${globalDocsInBatch} global status updates.`);
        }
      }
      functions.logger.info("[clearExpiredStatuses] Global status cleanup phase complete.");
    } catch (error) {
      functions.logger.error("[clearExpiredStatuses] Error processing global user statuses:", error);
    }

    // 2. Clean up expired group-specific statuses in `groupStatusDetails` subcollections
    try {
      const expiredGroupStatusesQuery = db
        .collectionGroup("groupStatusDetails") // Query across all users' subcollections
        .where("expiresAt", "<=", now);

      functions.logger.info("[clearExpiredStatuses] Starting group-specific status cleanup phase.");
      // For collectionGroup queries, especially if they can return many results,
      // consider more robust pagination or processing in smaller, more frequent intervals if necessary.
      // The current limit(batchSize * 5) is a heuristic to get a manageable set.
      const groupStatusSnapshot = await expiredGroupStatusesQuery.limit(batchSize * 2).get(); // Fetch a larger set to process in batches

      if (groupStatusSnapshot.empty) {
        functions.logger.info("[clearExpiredStatuses] No expired group-specific statuses found in this run.");
      } else {
        functions.logger.info(`[clearExpiredStatuses] Found ${groupStatusSnapshot.size} expired group-specific statuses to process.`);
        let groupStatusBatch = db.batch();
        let groupStatusDocsInBatch = 0;

        for (const doc of groupStatusSnapshot.docs) { // Use for...of for async operations within loop if needed, though not here
          functions.logger.log(`[clearExpiredStatuses] Processing group status ${doc.ref.path} for expiration.`);
          // Deleting the specific status document causes it to fallback to global/default user status.
          groupStatusBatch.delete(doc.ref);
          groupStatusDocsInBatch++;

          if (groupStatusDocsInBatch >= batchSize) {
            await groupStatusBatch.commit();
            operationsCommitted += groupStatusDocsInBatch;
            functions.logger.info(`[clearExpiredStatuses] Committed ${groupStatusDocsInBatch} group status deletions.`);
            groupStatusBatch = db.batch(); // New batch
            groupStatusDocsInBatch = 0;
          }
        }
        if (groupStatusDocsInBatch > 0) { // Commit remaining operations
          await groupStatusBatch.commit();
          operationsCommitted += groupStatusDocsInBatch;
          functions.logger.info(`[clearExpiredStatuses] Committed final ${groupStatusDocsInBatch} group status deletions.`);
        }
      }
      functions.logger.info("[clearExpiredStatuses] Group-specific status cleanup phase complete.");
    } catch (error) {
      functions.logger.error("[clearExpiredStatuses] Error processing group-specific statuses:", error);
    }

    functions.logger.info(`[clearExpiredStatuses] Job finished. Total operations/documents processed estimate: ${operationsCommitted}.`);
    return null;
  });
