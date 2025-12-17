import React from 'react'
import { AppBar, Toolbar, Typography, Button } from '@mui/material'

export default function Header({ user, onLogout }) {
  return (
    <AppBar position="fixed" sx={{ zIndex: 1400 }}>
      <Toolbar>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>Mini-WhatsApp</Typography>
        {user ? (
          <>
            <Typography sx={{ mx: 2 }}>{user.username}</Typography>
            <Button color="inherit" onClick={onLogout}>Logout</Button>
          </>
        ) : (
          <Typography>Guest</Typography>
        )}
      </Toolbar>
    </AppBar>
  )
}