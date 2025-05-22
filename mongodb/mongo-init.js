// mongodb/mongo-init.js
db.createCollection("profiles");
db.createUser({
  user: "app_user",
  pwd: "app_password",
  roles: [
    { role: "readWrite", db: "profiles_db" }
  ]
});