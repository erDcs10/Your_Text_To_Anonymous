const admin = require("firebase-admin");
const { v4: uuidv4 } = require("uuid");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://your-text-to-anonymous-default-rtdb.asia-southeast1.firebasedatabase.app"
});

const db = admin.database();
const queueRef = db.ref("/queue");

console.log("🚀 Custom Matchmaker Backend Started!");
console.log("Listening for users joining the queue...");

queueRef.on("value", async (snapshot) => {
  if (!snapshot.exists() || snapshot.numChildren() < 2) {
    return;
  }

  const qSnapshot = await queueRef.orderByChild("timestamp").limitToFirst(2).once("value");
  
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

roomsRef.on("child_changed", async (snapshot) => {
  const room = snapshot.val();
  const roomId = snapshot.key;

  if (room && room.status === "ended" && room.type === "anonymous") {
    console.log(`Room ${roomId} ended. Scheduling deletion in 5 seconds...`);
    setTimeout(async () => {
      try {
        await db.ref(`/rooms/${roomId}`).remove();
        console.log(`Garbage Collection: Deleted dead room ${roomId}`);
      } catch (error) {
        console.error(`Failed to delete room ${roomId}`, error);
      }
    }, 5000);
  }
});

const notifRef = db.ref("/notificationRequests");

notifRef.on("child_added", async (snapshot) => {
  const req = snapshot.val();
  const reqId = snapshot.key;

  try {
    const roomSnap = await db.ref(`/rooms/${req.roomId}`).once("value");
    const room = roomSnap.val();

    if (room && room.users) {
      const uids = Object.keys(room.users);
      const receiverId = uids.find(id => id !== req.senderId);

      if (receiverId) {
        const tokenSnap = await db.ref(`/users/${receiverId}/fcmToken`).once("value");
        const token = tokenSnap.val();

        if (token) {
          const payload = {
            token: token,
            notification: {
              title: "New Message",
              body: req.text
            },
            data: {
              roomId: req.roomId
            }
          };

          await admin.messaging().send(payload);
        }
      }
    }
  } catch (error) {
    console.error(error);
  } finally {
    await db.ref(`/notificationRequests/${reqId}`).remove();
  }
});

const logoutRef = db.ref("/logoutRequests");

logoutRef.on("child_added", async (snapshot) => {
  const req = snapshot.val();
  const reqId = snapshot.key;

  try {
    if (req && req.uid) {
      const updates = {};
      updates[`/users/${req.uid}/fcmToken`] = null;
      updates[`/queue/${req.uid}`] = null;
      updates[`/users/${req.uid}/activeRoom`] = null;
      updates[`/logoutRequests/${reqId}`] = null;

      await db.ref().update(updates);
    }
  } catch (error) {
    console.error(error);
  }
});

const deleteRoomRef = db.ref("/deleteRoomRequests");

deleteRoomRef.on("child_added", async (snapshot) => {
  const req = snapshot.val();
  const reqId = snapshot.key;

  try {
    if (req && req.roomId) {
      const roomSnap = await db.ref(`/rooms/${req.roomId}`).once("value");
      const room = roomSnap.val();
      const updates = {};

      if (room && room.users) {
        const uids = Object.keys(room.users);
        uids.forEach(uid => {
          updates[`/users/${uid}/persistentRooms/${req.roomId}`] = null;
        });
      }

      updates[`/rooms/${req.roomId}`] = null;
      updates[`/deleteRoomRequests/${reqId}`] = null;

      await db.ref().update(updates);
    }
  } catch (error) {
    console.error(error);
  }
});