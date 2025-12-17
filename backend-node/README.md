# WhatsApp Node Backend

This backend is designed to run alongside the existing Java project without modifying it.

Quick start

1. cd backend-node
2. copy `.env.example` to `.env` and edit if needed
3. npm install
4. npm start

Server: http://localhost:3000
Socket.IO: connect to same host (e.g. ws://localhost:3000) and emit `auth` with `{ username, token }` after login.

Important endpoints

- POST /api/auth/register { username, password }
- POST /api/auth/login { username, password } -> { token }
- POST /api/auth/logout { username, token }
- POST /api/messages/send { from, to, content }
- GET  /api/messages/history/:username
- POST /api/messages/mark-read { username, messageIds: [] }
- GET  /api/users

Example curl

- Register
  curl -X POST -H "Content-Type: application/json" -d '{"username":"alice","password":"pass"}' http://localhost:3000/api/auth/register

- Login
  curl -X POST -H "Content-Type: application/json" -d '{"username":"alice","password":"pass"}' http://localhost:3000/api/auth/login

Persistence

Uses MongoDB (`whatsapp` DB by default). Both the Java and Node backends can read/write the same collections.
