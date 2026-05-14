const admin = require("firebase-admin");
const { v4: uuidv4 } = require("uuid");
const serviceAccount = require("./serviceAccountKey.json");

// 1. Initialize Firebase with our VIP Pass
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://your-text-to-anonymous-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.database();
const queueRef = db.ref("/queue");

console.log("🚀 Custom Matchmaker Backend Started!");
console.log("Listening for users joining the queue...");

// 2. Listen to the queue continuously
queueRef.on("value", async (snapshot) => {
  // If queue is empty or has only 1 person, do nothing.
  if (!snapshot.exists() || snapshot.numChildren() < 2) {
    return;
  }

  // Fetch the oldest 2 users in the queue
  const qSnapshot = await queueRef.orderByChild("timestamp").limitToFirst(2).once("value");
  
  // Double check we still have 2 (prevents race conditions)
  if (qSnapshot.numChildren() < 2) return; 

  const users = [];
  qSnapshot.forEach((child) => {
    users.push(child.key);
  });

  const [uid1, uid2] = users;
  const roomId = uuidv4();

  // Create an atomic payload
  const updates = {};
  updates[`/queue/${uid1}`] = null; // Remove user 1
  updates[`/queue/${uid2}`] = null; // Remove user 2
  
  // Create Room
  updates[`/rooms/${roomId}`] = {
    type: "anonymous",
    status: "active",
    users: {
      [uid1]: true,
      [uid2]: true
    },
    createdAt: admin.database.ServerValue.TIMESTAMP
  };

  // Assign Room to Users
  updates[`/users/${uid1}/activeRoom`] = roomId;
  updates[`/users/${uid2}/activeRoom`] = roomId;

  try {
    // Commit all changes to database at the exact same time
    await db.ref().update(updates);
    console.log(`✅ MATCHED! ${uid1} and ${uid2} are now in room: ${roomId}`);
  } catch (error) {
    console.error("❌ Matchmaking failed: ", error);
  }
});
