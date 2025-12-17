import React, { useState } from 'react'
import { Box, TextField, Button, Typography } from '@mui/material'
import api from '../api'

export default function Auth({ onLogin, onRegister }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  async function handleRegister() {
    try {
      await api.post('/api/auth/register', { username, password })
      setMessage('Registered — you can now login')
      onRegister && onRegister()
    } catch (e) { setMessage(e?.response?.data?.error || 'Register failed') }
  }

  async function handleLogin() {
    try {
      const res = await api.post('/api/auth/login', { username, password })
      const payload = { username: res.data.username, token: res.data.token }
      localStorage.setItem('wa_user', JSON.stringify(payload))
      onLogin && onLogin(payload)
    } catch (e) { setMessage(e?.response?.data?.error || 'Login failed') }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6">Sign in / Register</Typography>
      <TextField label="Username" value={username} onChange={e => setUsername(e.target.value)} />
      <TextField type="password" label="Password" value={password} onChange={e => setPassword(e.target.value)} />
      <Box sx={{ display: 'flex', gap: 1 }}>
        <Button variant="contained" onClick={handleLogin}>Login</Button>
        <Button variant="outlined" onClick={handleRegister}>Register</Button>
      </Box>
      {message && <Typography color="error">{message}</Typography>}
    </Box>
  )
}