import React from 'react'
import { List, ListItem, ListItemText, Typography } from '@mui/material'

export default function MessageList({ messages = [], me }) {
  return (
    <List sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', gap: 1, p: 1 }}>
      {messages.map(m => {
        const mine = m.sender === me
        return (
          <ListItem key={m.id} sx={{ display: 'flex', justifyContent: mine ? 'flex-end' : 'flex-start' }}>
            <div className={`msg-bubble ${mine ? 'mine' : 'theirs'}`}>
              <div className="msg-content">{m.content}</div>
              <div className="msg-meta">{new Date(m.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})} {mine ? ' • ' + m.status : ''}</div>
            </div>
          </ListItem>
        )
      })}
    </List>
  )
}