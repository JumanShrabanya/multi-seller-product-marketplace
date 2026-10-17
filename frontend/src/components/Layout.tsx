import { Outlet } from 'react-router-dom'
import { Header } from './Header'

export function Layout() {
  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <Header />
      <main id="main" className="app-content">
        <Outlet />
      </main>
      <footer className="app-footer">
        <span>BajriX Multi-Seller Marketplace</span>
        <span className="muted">Demo build — email/password login, no real payments.</span>
      </footer>
    </div>
  )
}
