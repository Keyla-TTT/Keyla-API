// mongodb/mongo-init.js
db.createCollection("profiles");
db.createCollection("tests");
db.createCollection("typingTests");
db.createCollection("statistics");
db.createUser({
  user: "admin",
  pwd: "admin",
  roles: [
    { role: "readWrite", db: "profiles_db" }
  ]
});