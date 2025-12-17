const { connect, getDb, close } = require('../src/utils/db');
const userModel = require('../src/models/userModel');
const messageModel = require('../src/models/messageModel');
const { createTextMessage } = require('../src/utils/messageFactory');

(async () => {
  try {
    await connect(process.env.MONGO_URI || 'mongodb://127.0.0.1:27017', process.env.DB_NAME || 'whatsapp');
    console.log('Connected for smoke test');

    // cleanup test users/messages if exist
    const db = getDb();
    await db.collection('users').deleteMany({ username: { $in: ['smoke_alice', 'smoke_bob'] } });
    await db.collection('messages').deleteMany({ sender: { $in: ['smoke_alice'] } });

    // create users
    await userModel.createUser('smoke_alice', 'pass');
    await userModel.createUser('smoke_bob', 'pass');
    console.log('Users created');

    // create message
    const m = createTextMessage('smoke_alice', 'smoke_bob', 'Hello from smoke test');
    await messageModel.createMessage(m);
    console.log('Message created: ', m.id);

    // verify in DB
    const alice = await db.collection('users').findOne({ username: 'smoke_alice' });
    const msg = await db.collection('messages').findOne({ id: m.id });
    console.log('DB user:', alice ? { username: alice.username, status: alice.status } : null);
    console.log('DB message:', msg ? { id: msg.id, sender: msg.sender, receiver: msg.receiver, status: msg.status } : null);

    console.log('Smoke test completed successfully');
  } catch (e) {
    console.error('Smoke test failed:', e);
    process.exitCode = 2;
  } finally {
    await close();
  }
})();