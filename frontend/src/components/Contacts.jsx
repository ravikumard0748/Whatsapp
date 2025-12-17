import React from 'react'
import { List, ListItemButton, ListItemText, Avatar, Badge } from '@mui/material'

export default function Contacts({ users = [], currentUser, onSelect }) {
  return (
    <div>
      <List>
        {users.filter(u => u.username !== currentUser).map(u => (
          <ListItemButton key={u.username} onClick={() => onSelect(u.username)}>
            <Avatar sx={{ mr: 2 }}>{u.username.charAt(0).toUpperCase()}</Avatar>
            <ListItemText primary={u.username} secondary={u.status} />
          </ListItemButton>
        ))}
      </List>
    </div>
  )
}