import { Routes, Route, Navigate } from 'react-router-dom'
import ReviewPage from './pages/ReviewPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/cases/PV-2026-0451" replace />} />
      <Route path="/cases/:caseId" element={<ReviewPage />} />
      <Route path="*" element={<div className="p-8 text-navy">Not found</div>} />
    </Routes>
  )
}
