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
import {
  AppBar,
  Avatar,
  Box,
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
import { useAuth } from '../context/AuthContext'

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

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { roles, username, logout } = useAuth()
  const location = useLocation()

  const role = primaryRole(roles)
  const navItems = role ? NAV_ITEMS[role] : []

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar>
        <Typography variant="h6" noWrap sx={{ fontWeight: 700 }}>
          SkillMatch
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ flexGrow: 1 }}>
        {navItems.map((item) => (
          <ListItemButton
            key={item.path}
            component={Link}
            to={item.path}
            selected={location.pathname === item.path}
            onClick={() => setMobileOpen(false)}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Divider />
      <List>
        <ListItemButton onClick={logout}>
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
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {role ? role.charAt(0) + role.slice(1).toLowerCase() : ''}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2">{username}</Typography>
            <Avatar sx={{ width: 32, height: 32 }}>{username?.charAt(0).toUpperCase()}</Avatar>
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
        <Outlet />
      </Box>
    </Box>
  )
}
