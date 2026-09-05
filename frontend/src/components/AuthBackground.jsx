import { Box } from '@mui/material'

// Shared gradient backdrop for the public auth pages (Landing, Register), so
// the visual identity is consistent across the whole sign-up/sign-in journey.
export default function AuthBackground({ children }) {
  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100vh',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
        position: 'relative',
        overflow: 'hidden',
        background: 'linear-gradient(135deg, #4f46e5 0%, #6366f1 45%, #14b8a6 100%)',
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          width: 480,
          height: 480,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.08)',
          top: -160,
          left: -160,
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          width: 360,
          height: 360,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.08)',
          bottom: -140,
          right: -120,
        }}
      />
      {children}
    </Box>
  )
}
