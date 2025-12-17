const messageModel = require('../models/messageModel');
const userModel = require('../models/userModel');
const { createTextMessage } = require('../utils/messageFactory');
const notify = require('../services/notificationService');

async function sendMessage(req, res) {
  const { from, to, content } = req.body;
  if (!from || !to || !content) return res.status(400).json({ error: 'missing fields' });
  const receiver = await userModel.findByUsername(to);
  if (!receiver) return res.status(400).json({ error: 'receiver not found' });

  let msg = createTextMessage(from, to, content);
  await messageModel.createMessage(msg);

  if (notify.isOnline(to)) {
    msg.status = 'DELIVERED';
    await messageModel.updateMessageStatus(msg.id, 'DELIVERED');
    notify.notifyUser(to, 'new_message', msg);
    notify.notifyUser(from, 'message_status', { id: msg.id, status: 'DELIVERED' });
  } else {
    notify.notifyUser(from, 'message_status', { id: msg.id, status: 'SENT' });
  }

  return res.json({ ok: true, message: msg });
}

async function getHistory(req, res) {
  const username = req.params.username;
  const user = await userModel.findByUsername(username);
  if (!user) return res.status(400).json({ error: 'user not found' });
  const history = await messageModel.getMessageHistory(username);
  return res.json(history);
}

async function markRead(req, res) {
  const { username, messageIds } = req.body;
  if (!username || !messageIds) return res.status(400).json({ error: 'missing fields' });
  await messageModel.updateManyStatus(messageIds, 'READ');
  const history = await messageModel.getMessageHistory(username);
  for (const m of history) {
    if (messageIds.includes(m.id)) {
      notify.notifyUser(m.sender, 'message_status', { id: m.id, status: 'READ' });
    }
  }
  return res.json({ ok: true });
}

module.exports = { sendMessage, getHistory, markRead };