const userModel = require('../models/userModel');
const notification = require('../services/notificationService');
const { v4: uuidv4 } = require('uuid');

const tokens = new Map(); // token -> username

async function register(req, res) {
  const { username, password } = req.body;
  if (!username || !password) return res.status(400).json({ error: 'invalid input' });
  try {
    await userModel.createUser(username, password);
    return res.json({ ok: true });
  } catch (e) {
    return res.status(400).json({ error: e.message });
  }
}

async function login(req, res) {
  const { username, password } = req.body;
  const user = await userModel.findByUsername(username);
  if (!user) return res.status(400).json({ error: 'no such user' });
  const ok = await userModel.verifyPassword(user, password);
  if (!ok) return res.status(400).json({ error: 'invalid credentials' });
  await userModel.setStatus(username, 'ONLINE');
  const token = uuidv4();
  tokens.set(token, username);
  return res.json({ token, username });
}

async function logout(req, res) {
  const { username, token } = req.body;
  const tUser = tokens.get(token);
  if (tUser !== username) return res.status(403).json({ error: 'invalid token' });
  tokens.delete(token);
  await userModel.setStatus(username, 'OFFLINE');
  return res.json({ ok: true });
}

function authMiddleware(req, res, next) {
  const token = req.headers['x-auth-token'];
  if (!token) return res.status(401).json({ error: 'no token' });
  const username = tokens.get(token);
  if (!username) return res.status(401).json({ error: 'invalid token' });
  req.username = username;
  next();
}

module.exports = { register, login, logout, authMiddleware, tokens };