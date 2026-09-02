import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import PlaceholderPage from './components/PlaceholderPage'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import AdminDashboard from './pages/admin/Dashboard'
import CompanyDashboard from './pages/company/Dashboard'
import NotFound from './pages/NotFound'
import ProfessionalDashboard from './pages/professional/Dashboard'
import Unauthorized from './pages/Unauthorized'

function RoleHome() {
  const { hasRole } = useAuth()

  if (hasRole('ADMIN')) return <Navigate to="/admin" replace />
  if (hasRole('COMPANY')) return <Navigate to="/company" replace />
  if (hasRole('PROFESSIONAL')) return <Navigate to="/professional" replace />
  return <Navigate to="/unauthorized" replace />
}

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<RoleHome />} />

        <Route
          path="/professional"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <ProfessionalDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/projects"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <PlaceholderPage title="Progetti disponibili" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/applications"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <PlaceholderPage title="Le mie candidature" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/feedback"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <PlaceholderPage title="Feedback ricevuti" />
            </ProtectedRoute>
          }
        />

        <Route
          path="/company"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <CompanyDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/company/projects"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <PlaceholderPage title="I miei progetti" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/company/contracts"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <PlaceholderPage title="Contratti" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/company/payments"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <PlaceholderPage title="Pagamenti" />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <PlaceholderPage title="Validazione utenti" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/settings"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <PlaceholderPage title="Configurazione commissione" />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/transactions"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <PlaceholderPage title="Transazioni" />
            </ProtectedRoute>
          }
        />

        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

export default App
