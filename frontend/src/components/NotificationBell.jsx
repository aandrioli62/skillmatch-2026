import NotificationsIcon from '@mui/icons-material/Notifications'
import { Badge, Box, Divider, IconButton, List, ListItemButton, ListItemText, Menu, Typography } from '@mui/material'
import { useCallback, useEffect, useState } from 'react'
import api from '../services/api'
import { formatDateTime } from '../utils/format'

const POLL_INTERVAL_MS = 30000

export default function NotificationBell() {
  const [anchorEl, setAnchorEl] = useState(null)
  const [notifications, setNotifications] = useState([])

  const fetchNotifications = useCallback(() => {
    api
      .get('/notifications/mine')
      .then((res) => setNotifications(res.data))
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchNotifications()
    const interval = setInterval(fetchNotifications, POLL_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [fetchNotifications])

  const unreadCount = notifications.filter((n) => !n.read).length

  const handleNotificationClick = (notification) => {
    if (notification.read) return
    api
      .patch(`/notifications/${notification.id}/read`)
      .then(() => {
        setNotifications((prev) => prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n)))
      })
      .catch(() => {})
  }

  return (
    <>
      <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        slotProps={{ paper: { sx: { width: 360, maxWidth: '90vw' } } }}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
            Notifiche
          </Typography>
        </Box>
        <Divider />
        {notifications.length === 0 ? (
          <Box sx={{ px: 2, py: 3 }}>
            <Typography variant="body2" color="text.secondary">
              Nessuna notifica.
            </Typography>
          </Box>
        ) : (
          <List disablePadding sx={{ maxHeight: 360, overflowY: 'auto' }}>
            {notifications.map((notification) => (
              <ListItemButton
                key={notification.id}
                divider
                onClick={() => handleNotificationClick(notification)}
                sx={{
                  alignItems: 'flex-start',
                  bgcolor: notification.read ? 'transparent' : 'action.hover',
                }}
              >
                <ListItemText
                  primary={notification.message}
                  secondary={formatDateTime(notification.createdAt)}
                  slotProps={{
                    primary: {
                      sx: { fontWeight: notification.read ? 400 : 700, whiteSpace: 'normal' },
                    },
                  }}
                />
              </ListItemButton>
            ))}
          </List>
        )}
      </Menu>
    </>
  )
}
