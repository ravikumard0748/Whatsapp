import React, { useState } from 'react'
import { Box, TextField, Button } from '@mui/material'
import api from '../api'

export default function MessageInput({ from, to, onSent, groupId }) {
  const [text, setText] = useState('')

  async function send() {
    if (!text.trim()) return
    try {
      if (groupId) {
        await api.post(`/api/groups/${groupId}/message`, { from, content: text })
      } else {
        await api.post('/api/messages/send', { from, to, content: text })
      }
      setText('')
      onSent && onSent()
    } catch (e) { console.error(e) }
  }

  return (
    <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
      <TextField fullWidth value={text} onChange={e => setText(e.target.value)} />
      <Button variant="contained" onClick={send}>Send</Button>
    </Box>
  )
}