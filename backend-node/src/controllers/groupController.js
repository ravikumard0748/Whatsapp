const groupModel = require('../models/groupModel');
const messageModel = require('../models/messageModel');
const notify = require('../services/notificationService');

async function createGroup(req, res) {
  const { name, createdBy, members } = req.body;
  if (!name || !createdBy) return res.status(400).json({ error: 'missing fields' });
  try {
    const g = await groupModel.createGroup(name, createdBy, members || []);
    return res.json(g);
  } catch (e) { res.status(500).json({ error: e.message }); }
}

async function addMember(req, res) {
  const groupId = req.params.id;
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: 'missing username' });
  try {
    const g = await groupModel.addMember(groupId, username);
    // notify user added
    notify.notifyUser(username, 'group_added', { groupId: g.id, name: g.name });
    return res.json(g);
  } catch (e) { res.status(500).json({ error: e.message }); }
}

async function listUserGroups(req, res) {
  const username = req.params.username;
  if (!username) return res.status(400).json({ error: 'missing username' });
  try {
    const groups = await groupModel.getUserGroups(username);
    return res.json(groups);
  } catch (e) { res.status(500).json({ error: e.message }); }
}

async function getGroupMessages(req, res) {
  const groupId = req.params.id;
  try {
    const group = await groupModel.getGroupById(groupId);
    if (!group) return res.status(404).json({ error: 'group not found' });
    const msgs = await messageModel.getGroupMessages(groupId);
    return res.json(msgs);
  } catch (e) { res.status(500).json({ error: e.message }); }
}

async function sendGroupMessage(req, res) {
  const groupId = req.params.id;
  const { from, content } = req.body;
  if (!from || !content) return res.status(400).json({ error: 'missing fields' });
  try {
    const group = await groupModel.getGroupById(groupId);
    if (!group) return res.status(404).json({ error: 'group not found' });

    const m = { id: require('uuid').v4(), sender: from, receiver: null, content, timestamp: new Date().toISOString(), status: 'SENT', groupId };
    await messageModel.createMessage(m);

    // Notify group members (including sender for UI update)
    for (const member of group.members) {
      notify.notifyUser(member, 'new_group_message', { ...m, groupId, groupName: group.name });
    }

    return res.json({ ok: true, message: m });
  } catch (e) { res.status(500).json({ error: e.message }); }
}

module.exports = { createGroup, addMember, listUserGroups, getGroupMessages, sendGroupMessage };