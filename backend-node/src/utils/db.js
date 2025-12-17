const { MongoClient } = require('mongodb');
let client, db;

async function connect(uri, dbName) {
  client = new MongoClient(uri);
  await client.connect();
  db = client.db(dbName);
  await db.collection('messages').createIndex({ receiver: 1, status: 1 });
  await db.collection('users').createIndex({ username: 1 }, { unique: true });
  console.log('Connected to MongoDB (Node backend):', uri, '(db:', dbName + ')');
  return db;
}

function getDb() {
  if (!db) throw new Error('DB not connected');
  return db;
}

async function close() {
  if (client) await client.close();
}

module.exports = { connect, getDb, close };