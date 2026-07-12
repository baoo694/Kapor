// MongoDB Initialization Script
// Creates indexes and seed data for Kapor database

db = db.getSiblingDB('kapor');

// ========== INDEXES ==========

// Users
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ provider: 1, providerId: 1 }, { unique: true, sparse: true });
db.users.createIndex({ "streak.lastActiveDate": 1 });

// Daily Activities
db.daily_activities.createIndex({ userId: 1, date: -1 }, { unique: true });
db.daily_activities.createIndex({ date: 1 }, { expireAfterSeconds: 31536000 }); // TTL: 1 year

// Learning Progress
db.learning_progress.createIndex({ userId: 1, domain: 1 });
db.learning_progress.createIndex({ userId: 1, topicId: 1 }, { unique: true });
db.learning_progress.createIndex({ userId: 1, lastAccessedAt: -1 });

// Topics
db.topics.createIndex({ domain: 1, order: 1 });
db.topics.createIndex({ tags: 1 });

// Lessons
db.lessons.createIndex({ topicId: 1, order: 1 });

// Videos
db.videos.createIndex({ domain: 1, difficulty: 1 });
db.videos.createIndex({ youtubeVideoId: 1 }, { unique: true });
db.videos.createIndex({ tags: 1 });
db.videos.createIndex({ createdAt: -1 });

// Dictionary
db.dictionary.createIndex({ korean: 1 }, { unique: true });
db.dictionary.createIndex({ stem: 1 });
db.dictionary.createIndex({ "meanings.domain": 1 });

// Decks
db.decks.createIndex({ userId: 1 });
db.decks.createIndex({ userId: 1, domain: 1 });

// Flashcards
db.flashcards.createIndex({ userId: 1, deckId: 1 });
db.flashcards.createIndex({ userId: 1, "fsrs.nextReview": 1, "fsrs.state": 1 });
db.flashcards.createIndex({ "front.korean": 1 });
db.flashcards.createIndex({ tags: 1 });

// Scenarios
db.scenarios.createIndex({ domain: 1, difficulty: 1 });
db.scenarios.createIndex({ isActive: 1, order: 1 });

// Roleplay Sessions
db.roleplay_sessions.createIndex({ userId: 1, status: 1 });
db.roleplay_sessions.createIndex({ userId: 1, startedAt: -1 });

// Pronunciation
db.pronunciation_exercises.createIndex({ domain: 1, difficulty: 1 });
db.pronunciation_attempts.createIndex({ userId: 1, exerciseId: 1 });
db.pronunciation_attempts.createIndex({ userId: 1, attemptedAt: -1 });

// Admins
db.admins.createIndex({ email: 1 }, { unique: true });

print("✅ Kapor MongoDB indexes created successfully");
