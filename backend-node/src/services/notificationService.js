const sockets = new Map(); // username -> socket

function registerSocket(username, socket) {
  sockets.set(username, socket);
}

function unregisterSocket(username) {
  sockets.delete(username);
}

function isOnline(username) {
  return sockets.has(username);
}

function notifyUser(username, event, payload) {
  const socket = sockets.get(username);
  if (socket && socket.connected) {
    socket.emit(event, payload);
  }
}

module.exports = { registerSocket, unregisterSocket, isOnline, notifyUser };