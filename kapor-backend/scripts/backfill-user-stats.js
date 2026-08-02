/*
 * Rebuilds Profile > UserStats from the immutable learning_activity_events
 * ledger. This script is dry-run by default.
 *
 * Preview:
 *   mongosh "$MONGO_URI" scripts/backfill-user-stats.js
 * Apply:
 *   mongosh "$MONGO_URI" --eval 'var APPLY=true' scripts/backfill-user-stats.js
 *
 * It only updates users with at least one event and overwrites their four
 * derived all-time counters, so re-running it is safe.
 */
const apply = typeof APPLY !== 'undefined' && APPLY === true;

const totals = db.learning_activity_events.aggregate([
  {
    $group: {
      _id: '$userId',
      totalStudyMinutes: { $sum: { $ifNull: ['$minutesStudied', 0] } },
      totalCardsReviewed: { $sum: { $ifNull: ['$cardsReviewed', 0] } },
      totalRoleplaySessions: { $sum: { $ifNull: ['$roleplaySessions', 0] } },
      totalVideosWatched: { $sum: { $ifNull: ['$videosWatched', 0] } },
    },
  },
]);

let affected = 0;
totals.forEach((row) => {
  // Spring maps User.id to String in Java, while existing Mongo users may
  // still store _id as ObjectId. Match the physical Mongo type here.
  const userId = typeof row._id === 'string' && ObjectId.isValid(row._id)
    ? new ObjectId(row._id)
    : row._id;
  const stats = {
    totalStudyMinutes: row.totalStudyMinutes,
    totalCardsReviewed: row.totalCardsReviewed,
    totalRoleplaySessions: row.totalRoleplaySessions,
    totalVideosWatched: row.totalVideosWatched,
  };
  affected += 1;
  if (apply) {
    const result = db.users.updateOne({ _id: userId }, { $set: { stats } });
    if (result.matchedCount !== 1) {
      throw new Error(`Không tìm thấy user ${row._id} để cập nhật stats`);
    }
  } else {
    printjson({ userId: row._id, stats });
  }
});

print(`${apply ? 'Updated' : 'Previewed'} ${affected} user profile(s).`);
