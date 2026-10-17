import { Outlet } from 'react-router-dom'
import { Header } from './Header'

export function Layout() {
  return (
    <div className="app">
      <Header />
      <main className="app-content">
        <Outlet />
      </main>
      <footer className="app-footer">
        <span>BajriX Multi-Seller Marketplace</span>
        <span className="muted">Demo build — mock seller login, no real payments.</span>
      </footer>
    </div>
  )
}
