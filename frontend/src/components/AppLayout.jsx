import AssignmentIcon from '@mui/icons-material/Assignment'
import BusinessIcon from '@mui/icons-material/Business'
import DashboardIcon from '@mui/icons-material/Dashboard'
import DescriptionIcon from '@mui/icons-material/Description'
import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import PaymentIcon from '@mui/icons-material/Payment'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import SettingsIcon from '@mui/icons-material/Settings'
import StarIcon from '@mui/icons-material/Star'
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser'
import WorkIcon from '@mui/icons-material/Work'
import HandshakeIcon from '@mui/icons-material/Handshake'
import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import RequireProfile from './RequireProfile'

const DRAWER_WIDTH = 260

const NAV_ITEMS = {
  PROFESSIONAL: [
    { label: 'Dashboard', path: '/professional', icon: <DashboardIcon /> },
    { label: 'Progetti disponibili', path: '/professional/projects', icon: <WorkIcon /> },
    { label: 'Le mie candidature', path: '/professional/applications', icon: <AssignmentIcon /> },
    { label: 'Contratti', path: '/professional/contracts', icon: <DescriptionIcon /> },
    { label: 'Pagamenti', path: '/professional/payments', icon: <PaymentIcon /> },
    { label: 'Feedback ricevuti', path: '/professional/feedback', icon: <StarIcon /> },
  ],
  COMPANY: [
    { label: 'Dashboard', path: '/company', icon: <DashboardIcon /> },
    { label: 'I miei progetti', path: '/company/projects', icon: <BusinessIcon /> },
    { label: 'Contratti', path: '/company/contracts', icon: <DescriptionIcon /> },
    { label: 'Pagamenti', path: '/company/payments', icon: <PaymentIcon /> },
  ],
  ADMIN: [
    { label: 'Dashboard', path: '/admin', icon: <DashboardIcon /> },
    { label: 'Validazione utenti', path: '/admin/users', icon: <VerifiedUserIcon /> },
    { label: 'Configurazione commissione', path: '/admin/settings', icon: <SettingsIcon /> },
    { label: 'Transazioni', path: '/admin/transactions', icon: <ReceiptLongIcon /> },
  ],
}

function primaryRole(roles) {
  if (roles.includes('ADMIN')) return 'ADMIN'
  if (roles.includes('COMPANY')) return 'COMPANY'
  if (roles.includes('PROFESSIONAL')) return 'PROFESSIONAL'
  return null
}

const ROLE_COLOR = {
  PROFESSIONAL: '#4f46e5',
  COMPANY: '#14b8a6',
  ADMIN: '#7c3aed',
}

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { roles, username, logout } = useAuth()
  const location = useLocation()

  const role = primaryRole(roles)
  const navItems = role ? NAV_ITEMS[role] : []

  const roleColor = role ? ROLE_COLOR[role] : '#4f46e5'

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar
        sx={{
          background: 'linear-gradient(135deg, #4f46e5, #14b8a6)',
          gap: 1,
        }}
      >
        <HandshakeIcon sx={{ color: 'white' }} />
        <Typography variant="h6" noWrap sx={{ fontWeight: 700, color: 'white' }}>
          SkillMatch
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ flexGrow: 1, py: 1 }}>
        {navItems.map((item) => {
          const isSelected = location.pathname === item.path
          return (
            <ListItemButton
              key={item.path}
              component={Link}
              to={item.path}
              selected={isSelected}
              onClick={() => setMobileOpen(false)}
              sx={{
                mx: 1,
                borderRadius: 2,
                borderLeft: isSelected ? `3px solid ${roleColor}` : '3px solid transparent',
                '&.Mui-selected': {
                  bgcolor: `${roleColor}1a`,
                  '&:hover': { bgcolor: `${roleColor}26` },
                },
              }}
            >
              <ListItemIcon sx={{ color: isSelected ? roleColor : undefined }}>{item.icon}</ListItemIcon>
              <ListItemText
                primary={item.label}
                slotProps={{ primary: { sx: { fontWeight: isSelected ? 700 : 400 } } }}
              />
            </ListItemButton>
          )
        })}
      </List>
      <Divider />
      <List>
        <ListItemButton onClick={logout} sx={{ mx: 1, borderRadius: 2 }}>
          <ListItemIcon>
            <LogoutIcon />
          </ListItemIcon>
          <ListItemText primary="Logout" />
        </ListItemButton>
      </List>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        color="inherit"
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMobileOpen(true)}
            sx={{ display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          {role && (
            <Chip
              label={role.charAt(0) + role.slice(1).toLowerCase()}
              size="small"
              sx={{ bgcolor: `${roleColor}1a`, color: roleColor, fontWeight: 700 }}
            />
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2">{username}</Typography>
            <Avatar
              sx={{
                width: 32,
                height: 32,
                background: 'linear-gradient(135deg, #4f46e5, #14b8a6)',
              }}
            >
              {username?.charAt(0).toUpperCase()}
            </Avatar>
          </Box>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box component="main" sx={{ flexGrow: 1, p: 3, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` } }}>
        <Toolbar />
        <RequireProfile>
          <Outlet />
        </RequireProfile>
      </Box>
    </Box>
  )
}
