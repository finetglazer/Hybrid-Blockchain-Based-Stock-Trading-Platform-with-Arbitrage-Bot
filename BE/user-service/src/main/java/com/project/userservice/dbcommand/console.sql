// 1) Create the collection (optional, since MongoDB auto-creates on first insert,
//    but explicit creation is sometimes good practice)
db.createCollection("verification_tokens");



// 2) Create a TTL index on the `expiresAt` field.
//    Documents will automatically be removed after their expiresAt time passes.
db.verification_tokens.createIndex(
    { expiresAt: 1 },
    { expireAfterSeconds: 0 }
);

// 3) Insert a sample document (representing a pending verification)
db.verification_tokens.insertOne({
    email: "john@example.com",
    username: "JohnDoe",
    passwordHash: "$2a$10$somethingHashed", // example of a hashed password
    token: "randomStringOrUUID",
    createdAt: new Date(),
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000) // 24 hours from now
});
