import React, { useEffect, useState } from 'react'
import { Box, CssBaseline, AppBar, Toolbar, Typography, Container, Grid, Paper } from '@mui/material'
import Header from './components/Header'
import Auth from './components/Auth'
import Contacts from './components/Contacts'
import Groups from './components/Groups'
import Chat from './components/Chat'
import api from './api'
import { connectSocket, disconnectSocket, getSocket } from './socket'

export default function App() {
  const [user, setUser] = useState(null) // { username, token }
  const [users, setUsers] = useState([])
  const [selectedContact, setSelectedContact] = useState(null)
  const [selectedGroup, setSelectedGroup] = useState(null)

  useEffect(() => { fetchUsers() }, [])
  useEffect(() => {
    if (user) {
      connectSocket({ username: user.username, token: user.token })
      const s = getSocket()
      s?.on('new_message', (m) => {
        // simple console log - Chat component will refetch on selection
        console.log('incoming', m)
      })
    } else {
      disconnectSocket()
    }
    return () => { const s = getSocket(); s?.off('new_message') }
  }, [user])

  async function fetchUsers() {
    try {
      const res = await api.get('/api/users')
      setUsers(res.data)
    } catch (e) { console.error(e) }
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f5f7fb' }}>
      <CssBaseline />
      <Header user={user} onLogout={() => { setUser(null); setSelectedContact(null); setSelectedGroup(null); fetchUsers() }} />
      <Container maxWidth="lg" sx={{ paddingTop: 4 }}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={4}>
            <Paper sx={{ height: '74vh', p:2, overflow: 'auto' }}>
              {!user ? (
                <Auth onLogin={(u) => { setUser(u); fetchUsers() }} onRegister={() => fetchUsers()} />
              ) : (
                <div>
                  <Contacts users={users} currentUser={user?.username} onSelect={c => { setSelectedContact(c); setSelectedGroup(null); }} />
                  <Groups currentUser={user} onSelect={g => { setSelectedGroup(g); setSelectedContact(null); }} />
                </div>
              )}
            </Paper>
          </Grid>

          <Grid item xs={12} md={8}>
            <Paper sx={{ height: '74vh', p:2, display: 'flex', flexDirection: 'column' }}>
              <Chat currentUser={user} contact={selectedContact} groupId={selectedGroup} />
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  )
}