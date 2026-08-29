import { Route, Routes } from 'react-router'
import HomePage from './pages/HomePage'
import PartyPassportPage from './pages/PartyPassportPage'
import PartyResultPage from './pages/PartyResultPage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/party/:code" element={<PartyPassportPage />} />
      <Route path="/party/:code/result" element={<PartyResultPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
