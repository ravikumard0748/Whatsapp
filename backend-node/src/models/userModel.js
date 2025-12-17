const { getDb } = require('../utils/db');
const bcrypt = require('bcrypt');
const SALT_ROUNDS = 10;

async function createUser(username, password) {
  const db = getDb();
  const hash = await bcrypt.hash(password, SALT_ROUNDS);
  const user = { username, password: hash, status: 'OFFLINE', createdAt: new Date() };
  await db.collection('users').insertOne(user);
  return { username, status: user.status };
}

async function findByUsername(username) {
  const db = getDb();
  return db.collection('users').findOne({ username });
}

async function setStatus(username, status) {
  const db = getDb();
  await db.collection('users').updateOne({ username }, { $set: { status } });
}

async function listUsers() {
  const db = getDb();
  return db.collection('users').find({}, { projection: { password: 0 } }).toArray();
}

async function verifyPassword(userDoc, password) {
  return bcrypt.compare(password, userDoc.password);
}

module.exports = { createUser, findByUsername, setStatus, listUsers, verifyPassword };