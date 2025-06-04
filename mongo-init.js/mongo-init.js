// mongodb/mongo-init.js
db.createCollection("profiles");
db.createCollection("tests");
db.createUser({
  user: "admin",
  pwd: "admin",
  roles: [
    { role: "readWrite", db: "profiles_db" }
  ]
});