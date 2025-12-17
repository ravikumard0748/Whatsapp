# Mini-WhatsApp Frontend (React + Vite + MUI)

Quick start

1. cd frontend
2. copy `.env.example` to `.env` and, if needed, set `VITE_API_URL` to your Node backend (default: `http://localhost:3000`).
3. npm install
4. npm run dev

Notes
- Uses Material UI for a simple professional look.
- Socket.IO client included (connects after login for real-time notifications).
- The app expects the Node backend API at `VITE_API_URL` with endpoints described in `backend-node/README.md`.

Next improvements
- Add message typing indicators, message loading spinners, and persistent client-side caching.
- Add profile pictures and conversational timestamps formatting.
- Implement JWT + auth-protected endpoints in Node backend and wire client authorization headers.
