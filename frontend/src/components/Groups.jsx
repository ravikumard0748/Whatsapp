import React, { useEffect, useState } from 'react'
import { Box, Button, TextField, List, ListItemButton, ListItemText } from '@mui/material'
import api from '../api'

export default function Groups({ currentUser, onSelect }) {
  const [groups, setGroups] = useState([])
  const [name, setName] = useState('')

  useEffect(() => { if (currentUser) fetchGroups() }, [currentUser])

  async function fetchGroups() {
    try {
      const res = await api.get(`/api/groups/user/${currentUser.username}`)
      setGroups(res.data)
    } catch (e) { console.error(e) }
  }

  async function create() {
    try {
      const res = await api.post('/api/groups', { name, createdBy: currentUser.username })
      setName('')
      fetchGroups()
      onSelect && onSelect(res.data.id)
    } catch (e) { console.error(e) }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
      <Box sx={{ display: 'flex', gap: 1 }}>
        <TextField value={name} onChange={e => setName(e.target.value)} size="small" placeholder="New group name" />
        <Button variant="contained" onClick={create}>Create</Button>
      </Box>
      <List>
        {groups.map(g => (
          <ListItemButton key={g.id} onClick={() => onSelect(g.id)}>
            <ListItemText primary={g.name} secondary={g.members?.join(', ')} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  )
}