import { NavLink } from 'react-router-dom'
import { useSession } from '../session/SessionProvider'

export function Header() {
  const { account, isSeller, logout } = useSession()

  return (
    <header className="app-header">
      <div className="app-header__bar">
        <NavLink to="/" className="brand">
          <span className="brand__mark" aria-hidden="true">
            B
          </span>
          <span className="brand__text">
            BajriX <span className="brand__sub">Marketplace</span>
          </span>
        </NavLink>

        <nav className="app-nav">
          <NavLink to="/" end>
            Products
          </NavLink>
          {isSeller ? <NavLink to="/seller">Seller dashboard</NavLink> : null}
        </nav>

        <div className="account">
          {account ? (
            <>
              <span className="account__identity">
                <span className="account__name">{account.name}</span>
                <span className="account__role">{account.role === 'SELLER' ? 'Seller' : 'Buyer'}</span>
              </span>
              <button type="button" className="button button--small button--ghost" onClick={logout}>
                Sign out
              </button>
            </>
          ) : (
            <NavLink to="/login" className="button button--small">
              Sign in
            </NavLink>
          )}
        </div>
      </div>
    </header>
  )
}
