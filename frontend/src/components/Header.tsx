import { NavLink } from 'react-router-dom'
import { useSession } from '../session/SessionProvider'
import { StatusBadge } from './StatusBadge'

export function Header() {
  const { mode, sellerId, seller, sellers, sellersLoading, sellersError, setMode, setSellerId } = useSession()

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
          <NavLink to="/seller">Seller dashboard</NavLink>
        </nav>

        <div className="role-switcher">
          <span className="role-switcher__label">View as</span>
          <div className="segmented" role="group" aria-label="View as">
            <button
              type="button"
              className={mode === 'BUYER' ? 'is-active' : ''}
              aria-pressed={mode === 'BUYER'}
              onClick={() => setMode('BUYER')}
            >
              Buyer
            </button>
            <button
              type="button"
              className={mode === 'SELLER' ? 'is-active' : ''}
              aria-pressed={mode === 'SELLER'}
              onClick={() => setMode('SELLER')}
            >
              Seller
            </button>
          </div>
        </div>
      </div>

      {mode === 'SELLER' ? (
        <div className="app-header__seller">
          {sellersLoading ? <span className="muted">Loading sellers…</span> : null}
          {sellersError ? <span className="muted">Seller list unavailable — is the backend running?</span> : null}
          {!sellersLoading && !sellersError ? (
            <>
              <label className="seller-picker">
                <span>Signed in as</span>
                <select value={sellerId ?? ''} onChange={(event) => setSellerId(Number(event.target.value))}>
                  {sellers.map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name} — {option.city}
                    </option>
                  ))}
                </select>
              </label>
              {seller ? <StatusBadge status={seller.status} /> : null}
            </>
          ) : null}
        </div>
      ) : null}
    </header>
  )
}
