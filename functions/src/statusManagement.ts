import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Ensure Firebase Admin is initialized (typically in index.ts or a shared config)
// if (admin.apps.length === 0) {
//   admin.initializeApp();
// }
const db = admin.firestore();

// Default status to revert to when a status expires.
// Could be configured elsewhere or passed if more dynamic.
const DEFAULT_EXPIRED_STATUS_ID = "online"; // Assuming "online" is an AppPresetStatus.id

/**
 * Scheduled function to clean up expired user statuses.
 * This includes global statuses on the User object and group-specific statuses
 * in the groupStatusDetails subcollection.
 *
 * Schedule: e.g., "every 1 hours" or "every 15 minutes from 00:00 to 23:59"
 * For testing, can be triggered via HTTP or manually.
 */
export const clearExpiredStatuses = functions.pubsub
  .schedule("every 60 minutes") // Adjust schedule as needed
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    const batchSize = 200; // Firestore batch write limit is 500, process in smaller chunks.
    let operationsCommitted = 0;

    functions.logger.info("Starting scheduled job: clearExpiredStatuses");

    // 1. Clean up expired global statuses on User documents
    try {
      const expiredGlobalUsersQuery = db
        .collection("users")
        .where("globalStatusExpiresAt", "<=", now);

      let lastGlobalSnapshot: admin.firestore.QueryDocumentSnapshot | null = null;
      let globalHasMore = true;
      let globalBatch = db.batch();
      let globalDocsInBatch = 0;

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

        snapshot.forEach((doc) => {
          functions.logger.log(`Processing user ${doc.id} for expired global status.`);
          globalBatch.update(doc.ref, {
            activeStatusId: DEFAULT_EXPIRED_STATUS_ID,
            globalCustomStatusText: null,
            globalCustomStatusIconKey: null,
            globalStatusExpiresAt: null,
          });
          globalDocsInBatch++;
          if (globalDocsInBatch >= batchSize) {
            await globalBatch.commit();
            operationsCommitted += globalDocsInBatch;
            functions.logger.info(`Committed ${globalDocsInBatch} global status updates.`);
            globalBatch = db.batch(); // Start a new batch
            globalDocsInBatch = 0;
          }
        });
      }
      if (globalDocsInBatch > 0) {
        await globalBatch.commit();
        operationsCommitted += globalDocsInBatch;
        functions.logger.info(`Committed final ${globalDocsInBatch} global status updates.`);
      }
      functions.logger.info("Global status cleanup phase complete.");
    } catch (error) {
      functions.logger.error("Error cleaning up global user statuses:", error);
    }

    // 2. Clean up expired group-specific statuses in `groupStatusDetails` subcollections
    try {
      const expiredGroupStatusesQuery = db
        .collectionGroup("groupStatusDetails")
        .where("expiresAt", "<=", now);

      // Note: CollectionGroup queries are harder to paginate effectively without specific ordering
      // if we expect huge numbers. For moderate numbers, limit and process.
      // If this becomes very large, consider a different approach for large-scale iteration.
      let groupStatusBatch = db.batch();
      let groupStatusDocsInBatch = 0;
      const groupStatusSnapshot = await expiredGroupStatusesQuery.limit(batchSize * 5).get(); // Get a larger initial set for collection group

      if (groupStatusSnapshot.empty) {
        functions.logger.info("No expired group-specific statuses found.");
      } else {
        groupStatusSnapshot.forEach((doc) => {
          functions.logger.log(`Processing group status ${doc.ref.path} for expiration.`);
          // Revert to reflect global status or a default for the group
          // For simplicity, we delete the specific override, letting it fallback to global.
          // Alternatively, update to a default group status.
          groupStatusBatch.delete(doc.ref); // Deleting the specific status reverts to global/default.
          // Or, to update instead of delete:
          // groupStatusBatch.update(doc.ref, {
          //   type: "APP_PRESET", // Or whatever your default type is
          //   activeStatusReferenceId: DEFAULT_EXPIRED_STATUS_ID,
          //   customText: null,
          //   customIconKey: null,
          //   expiresAt: null,
          //   lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
          // });
          groupStatusDocsInBatch++;
          if (groupStatusDocsInBatch >= batchSize) {
            groupStatusBatch.commit()
              .then(() => {
                operationsCommitted += groupStatusDocsInBatch;
                functions.logger.info(`Committed ${groupStatusDocsInBatch} group status updates/deletions.`);
              })
              .catch(err => functions.logger.error("Error committing group status batch:", err));
            groupStatusBatch = db.batch();
            groupStatusDocsInBatch = 0;
          }
        });

        if (groupStatusDocsInBatch > 0) {
          await groupStatusBatch.commit();
          operationsCommitted += groupStatusDocsInBatch;
          functions.logger.info(`Committed final ${groupStatusDocsInBatch} group status updates/deletions.`);
        }
      }
      functions.logger.info("Group-specific status cleanup phase complete.");
    } catch (error) {
      functions.logger.error("Error cleaning up group-specific statuses:", error);
    }

    functions.logger.info(`clearExpiredStatuses job finished. Total operations committed: ${operationsCommitted}.`);
    return null;
  });
