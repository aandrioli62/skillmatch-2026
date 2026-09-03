import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import PlaceholderPage from './components/PlaceholderPage'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import AdminDashboard from './pages/admin/Dashboard'
import CompanyContracts from './pages/company/Contracts'
import CompanyDashboard from './pages/company/Dashboard'
import CompanyPayments from './pages/company/Payments'
import CompanyProjects from './pages/company/Projects'
import NotFound from './pages/NotFound'
import ProfessionalApplications from './pages/professional/Applications'
import ProfessionalContracts from './pages/professional/Contracts'
import ProfessionalDashboard from './pages/professional/Dashboard'
import ProfessionalFeedback from './pages/professional/Feedback'
import ProfessionalPayments from './pages/professional/Payments'
import ProfessionalProjects from './pages/professional/Projects'
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
              <ProfessionalProjects />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/applications"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <ProfessionalApplications />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/contracts"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <ProfessionalContracts />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/payments"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <ProfessionalPayments />
            </ProtectedRoute>
          }
        />
        <Route
          path="/professional/feedback"
          element={
            <ProtectedRoute allowedRoles={['PROFESSIONAL']}>
              <ProfessionalFeedback />
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
              <CompanyProjects />
            </ProtectedRoute>
          }
        />
        <Route
          path="/company/contracts"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <CompanyContracts />
            </ProtectedRoute>
          }
        />
        <Route
          path="/company/payments"
          element={
            <ProtectedRoute allowedRoles={['COMPANY']}>
              <CompanyPayments />
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
