require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { connect } = require('./utils/db');
const authCtrl = require('./controllers/authController');
const msgCtrl = require('./controllers/messageController');
const notify = require('./services/notificationService');

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://127.0.0.1:27017';
const DB_NAME = process.env.DB_NAME || 'whatsapp';

(async () => {
  await connect(MONGO_URI, DB_NAME);

  const app = express();
  app.use(cors());
  app.use(express.json());

  app.post('/api/auth/register', authCtrl.register);
  app.post('/api/auth/login', authCtrl.login);
  app.post('/api/auth/logout', authCtrl.logout);

  app.post('/api/messages/send', msgCtrl.sendMessage);
  app.get('/api/messages/history/:username', msgCtrl.getHistory);
  app.post('/api/messages/mark-read', msgCtrl.markRead);

  app.get('/api/users', async (req, res) => {
    const users = await require('./models/userModel').listUsers();
    res.json(users);
  });

  // Group endpoints
  app.post('/api/groups', require('./controllers/groupController').createGroup);
  app.post('/api/groups/:id/add', require('./controllers/groupController').addMember);
  app.get('/api/groups/user/:username', require('./controllers/groupController').listUserGroups);
  app.get('/api/groups/:id/messages', require('./controllers/groupController').getGroupMessages);
  app.post('/api/groups/:id/message', require('./controllers/groupController').sendGroupMessage);

  const server = require('http').createServer(app);
  const { Server } = require('socket.io');
  const io = new Server(server, { cors: { origin: '*' } });

  io.on('connection', (socket) => {
    socket.on('auth', async ({ username, token }) => {
      const tokens = require('./controllers/authController').tokens;
      if (tokens.get(token) !== username) {
        socket.emit('error', 'invalid token');
        return;
      }
      notify.registerSocket(username, socket);
      await require('./models/userModel').setStatus(username, 'ONLINE');

      // deliver undelivered messages
      const undelivered = await require('./models/messageModel').getUndeliveredMessages(username);
      for (const m of undelivered) {
        await require('./models/messageModel').updateMessageStatus(m.id, 'DELIVERED');
        socket.emit('new_message', { ...m, status: 'DELIVERED' });
        notify.notifyUser(m.sender, 'message_status', { id: m.id, status: 'DELIVERED' });
      }

      socket.on('disconnect', async () => {
        notify.unregisterSocket(username);
        await require('./models/userModel').setStatus(username, 'OFFLINE');
      });
    });
  });

  server.listen(PORT, () => console.log(`Node backend listening on http://localhost:${PORT}`));
})();