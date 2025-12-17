const { getDb } = require('../utils/db');
const { v4: uuidv4 } = require('uuid');

async function createGroup(name, createdBy, members = []) {
  const db = getDb();
  const group = { id: uuidv4(), name, createdBy, members: Array.from(new Set([...members, createdBy])), createdAt: new Date() };
  await db.collection('groups').insertOne(group);
  return group;
}

async function addMember(groupId, username) {
  const db = getDb();
  await db.collection('groups').updateOne({ id: groupId }, { $addToSet: { members: username } });
  return db.collection('groups').findOne({ id: groupId });
}

async function removeMember(groupId, username) {
  const db = getDb();
  await db.collection('groups').updateOne({ id: groupId }, { $pull: { members: username } });
  return db.collection('groups').findOne({ id: groupId });
}

async function getGroupById(groupId) {
  const db = getDb();
  return db.collection('groups').findOne({ id: groupId });
}

async function getUserGroups(username) {
  const db = getDb();
  return db.collection('groups').find({ members: username }).toArray();
}

module.exports = { createGroup, addMember, removeMember, getGroupById, getUserGroups };