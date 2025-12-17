const { getDb } = require('../utils/db');

async function createMessage(message) {
  const db = getDb();
  await db.collection('messages').insertOne(message);
  return message;
}

async function getUndeliveredMessages(receiver) {
  const db = getDb();
  return db.collection('messages')
    .find({ receiver, status: 'SENT' })
    .sort({ timestamp: 1 })
    .toArray();
}

async function getGroupMessages(groupId) {
  const db = getDb();
  return db.collection('messages')
    .find({ groupId })
    .sort({ timestamp: 1 })
    .toArray();
}

async function getMessageHistory(username) {
  const db = getDb();
  return db.collection('messages')
    .find({ $or: [{ sender: username }, { receiver: username }] })
    .sort({ timestamp: 1 })
    .toArray();
}

async function updateMessageStatus(messageId, status) {
  const db = getDb();
  await db.collection('messages').updateOne({ id: messageId }, { $set: { status } });
}

async function updateManyStatus(ids, status) {
  const db = getDb();
  await db.collection('messages').updateMany({ id: { $in: ids } }, { $set: { status } });
}

module.exports = { createMessage, getUndeliveredMessages, getMessageHistory, updateMessageStatus, updateManyStatus };