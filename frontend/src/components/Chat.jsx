import React, { useEffect, useState } from 'react'
import { Typography, Box } from '@mui/material'
import api from '../api'
import MessageList from './MessageList'
import MessageInput from './MessageInput'

export default function Chat({ currentUser, contact, groupId }) {
  const [messages, setMessages] = useState([])

  useEffect(() => { if (contact) fetchHistory(); if (groupId) fetchGroupHistory(); }, [contact, groupId])

  async function fetchHistory() {
    try {
      const res = await api.get(`/api/messages/history/${contact}`)
      setMessages(res.data)
    } catch (e) { console.error(e) }
  }

  async function fetchGroupHistory() {
    try {
      const res = await api.get(`/api/groups/${groupId}/messages`)
      setMessages(res.data)
    } catch (e) { console.error(e) }
  }

  if (!contact && !groupId) return <Typography variant="h6">Select a contact or group to start chatting</Typography>

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Typography variant="h6">{groupId ? `Group: ${groupId}` : `Chat with ${contact}`}</Typography>
      <MessageList messages={messages} me={currentUser?.username} />
      {groupId ? (
        <MessageInput from={currentUser?.username} to={null} onSent={() => fetchGroupHistory()} groupId={groupId} />
      ) : (
        <MessageInput from={currentUser?.username} to={contact} onSent={fetchHistory} />
      )}
    </Box>
  )
}