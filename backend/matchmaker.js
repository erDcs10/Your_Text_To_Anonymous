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
  const u1Data = (await db.ref(`/users/${uid1}/persistentRooms`).once("value")).val() || {};
  const u2Data = (await db.ref(`/users/${uid2}/persistentRooms`).once("value")).val() || {};
  
  const commonRooms = Object.keys(u1Data).filter(roomId => Object.keys(u2Data).includes(roomId));

  if (commonRooms.length > 0) {
    console.log(`Skipped match: ${uid1} and ${uid2} are already revealed to each other.`);
    await db.ref(`/queue/${uid2}`).remove(); 
    return; 
  }
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

// Reveal 
const roomsRef = db.ref("/rooms");

roomsRef.on("child_changed", async (snapshot) => {
  const room = snapshot.val();
  const roomId = snapshot.key;

  if (room && room.type === "anonymous" && room.status === "active" && room.revealRequests) {
    const requesters = Object.keys(room.revealRequests);
    
    if (requesters.length === 2 && room.revealRequests[requesters[0]] && room.revealRequests[requesters[1]]) {
      
      const newRoomId = uuidv4();
      const [uid1, uid2] = requesters;
      const updates = {};

      console.log(`🔄 Both users agreed to reveal! Migrating room ${roomId}...`);

      // Create the new persistent room (E7)
      updates[`/rooms/${newRoomId}`] = {
        type: "persistent",
        status: "active",
        users: {
          [uid1]: true,
          [uid2]: true
        },
        createdAt: admin.database.ServerValue.TIMESTAMP
      };

      // Save to both users' permanent lists (E8)
      updates[`/users/${uid1}/persistentRooms/${newRoomId}`] = true;
      updates[`/users/${uid2}/persistentRooms/${newRoomId}`] = true;

      // End the anonymous room and point them to the new one (E10)
      updates[`/rooms/${roomId}/status`] = "ended";
      updates[`/rooms/${roomId}/revealedTo`] = newRoomId;

      try {
        await db.ref().update(updates);
        console.log(`✅ Reveal Complete! Users moved to permanent room: ${newRoomId}`);
      } catch (error) {
        console.error("❌ Reveal migration failed: ", error);
      }
    }
  }
});
