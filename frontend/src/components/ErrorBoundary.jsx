import React from 'react'
import { Box, Typography, Paper, Button } from '@mui/material'

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
          <Paper sx={{ p: 3, maxWidth: 700 }}>
            <Typography variant="h6" color="error">An unexpected error occurred</Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt:2 }}>{String(this.state.error)}</Typography>
            <Box sx={{ mt:2 }}>
              <Button variant="contained" onClick={() => window.location.reload()}>Reload</Button>
            </Box>
          </Paper>
        </Box>
      )
    }
    return this.props.children
  }
}
