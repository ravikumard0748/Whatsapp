import { io } from 'socket.io-client'

let socket = null

export function connectSocket({ username, token, apiUrl }) {
  if (socket && socket.connected) return socket
  socket = io(apiUrl || import.meta.env.VITE_API_URL || 'http://localhost:3000')
  socket.on('connect', () => {
    if (username && token) socket.emit('auth', { username, token })
  })
  return socket
}

export function getSocket() { return socket }

export function disconnectSocket() {
  if (socket) socket.disconnect()
  socket = null
}