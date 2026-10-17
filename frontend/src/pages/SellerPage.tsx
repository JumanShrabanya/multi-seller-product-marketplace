import { EmptyState } from '../components/states'
import { StatusBadge } from '../components/StatusBadge'
import { useSession } from '../session/SessionProvider'

export function SellerPage() {
  const { mode, seller, sellersLoading, setMode } = useSession()

  if (mode !== 'SELLER') {
    return (
      <section className="page">
        <EmptyState title="You are browsing as a buyer" message="Switch to Seller to manage listings." />
        <button type="button" className="button" onClick={() => setMode('SELLER')}>
          Switch to seller view
        </button>
      </section>
    )
  }

  if (sellersLoading) {
    return (
      <section className="page">
        <EmptyState title="Loading seller…" />
      </section>
    )
  }

  if (seller === null) {
    return (
      <section className="page">
        <EmptyState title="No seller selected" message="Pick a seller from the header to continue." />
      </section>
    )
  }

  return (
    <section className="page">
      <header className="page__header">
        <h1>{seller.name}</h1>
        <p className="muted">
          {seller.city} · Seller #{seller.id}
        </p>
        <StatusBadge status={seller.status} />
      </header>

      {seller.status !== 'APPROVED' ? (
        <div className="notice notice--warn">
          {seller.status === 'PENDING'
            ? 'This seller is awaiting approval. Listing changes are disabled until an admin approves it.'
            : 'This seller was rejected. Listing changes are disabled.'}
        </div>
      ) : null}

      <EmptyState title="Listings coming next" message="The listings table and editor arrive in Module 8." />
    </section>
  )
}
