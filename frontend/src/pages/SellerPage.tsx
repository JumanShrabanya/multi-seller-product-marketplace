import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { fetchSellerListings } from '../api/seller'
import { AddProductPanel } from '../components/seller/AddProductPanel'
import { ListingRow } from '../components/seller/ListingRow'
import { EmptyState, ErrorState, LoadingState } from '../components/states'
import { StatusBadge } from '../components/StatusBadge'
import { useSession } from '../session/SessionProvider'
import type { Seller } from '../api/types'

const PAGE_SIZE = 20

export function SellerPage() {
  const { mode, sellerId, seller, sellersLoading, setMode } = useSession()

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
        <LoadingState label="Loading seller…" />
      </section>
    )
  }

  if (sellerId === null || seller === null) {
    return (
      <section className="page">
        <EmptyState title="No seller selected" message="Pick a seller from the header to continue." />
      </section>
    )
  }

  return <SellerDashboard sellerId={sellerId} seller={seller} />
}

function SellerDashboard({ sellerId, seller }: { sellerId: number; seller: Seller }) {
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const canWrite = seller.status === 'APPROVED'

  const listingsQuery = useQuery({
    queryKey: ['seller-listings', sellerId, page],
    queryFn: ({ signal }) => fetchSellerListings(sellerId, page, PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })

  const refresh = () => {
    void listingsQuery.refetch()
  }

  const data = listingsQuery.data

  return (
    <section className="page">
      <header className="seller-hero">
        <div className="page__header">
          <h1>{seller.name}</h1>
          <p className="muted">
            {seller.city} · Seller #{seller.id}
          </p>
        </div>
        <StatusBadge status={seller.status} />
      </header>

      {!canWrite ? (
        <div className="notice notice--warn">
          {seller.status === 'PENDING'
            ? 'This seller is awaiting approval. You can view listings, but adding, editing, or stopping them is disabled until an admin approves this seller.'
            : 'This seller was rejected, so listings cannot be changed.'}
        </div>
      ) : null}

      <div className="toolbar">
        <div className="toolbar__title">
          <h2 className="section-title">Your listings</h2>
          {data ? <span className="muted">{data.totalElements} total</span> : null}
        </div>
        {canWrite ? (
          <button type="button" className="button" onClick={() => setAdding((current) => !current)}>
            {adding ? 'Close' : 'Add product'}
          </button>
        ) : null}
      </div>

      {adding && canWrite ? (
        <AddProductPanel
          sellerId={sellerId}
          onClose={() => setAdding(false)}
          onCreated={() => {
            setAdding(false)
            setPage(0)
            refresh()
          }}
        />
      ) : null}

      {listingsQuery.isPending ? <LoadingState label="Loading your listings…" /> : null}

      {listingsQuery.isError ? (
        <ErrorState
          message={listingsQuery.error instanceof ApiError ? listingsQuery.error.message : 'Could not load your listings.'}
          onRetry={refresh}
        />
      ) : null}

      {data && !listingsQuery.isError ? (
        data.content.length === 0 ? (
          <EmptyState title="No listings yet" message={canWrite ? 'Add a product to start selling.' : undefined} />
        ) : (
          <>
            <div className="table-wrap">
              <table className="offers-table listings-table">
                <thead>
                  <tr>
                    <th scope="col">Product</th>
                    <th scope="col" className="num">
                      Price
                    </th>
                    <th scope="col" className="num">
                      Stock
                    </th>
                    <th scope="col" className="num">
                      Min. order
                    </th>
                    <th scope="col">Status</th>
                    <th scope="col">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((listing) => (
                    <ListingRow
                      key={listing.id}
                      listing={listing}
                      sellerId={sellerId}
                      canWrite={canWrite}
                      onChanged={refresh}
                    />
                  ))}
                </tbody>
              </table>
            </div>

            {data.totalPages > 1 ? (
              <nav className="pagination" aria-label="Pagination">
                <button
                  type="button"
                  className="button button--ghost"
                  disabled={data.page === 0}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                >
                  ← Previous
                </button>
                <span className="muted">
                  Page {data.page + 1} of {data.totalPages}
                </span>
                <button
                  type="button"
                  className="button button--ghost"
                  disabled={data.page + 1 >= data.totalPages}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Next →
                </button>
              </nav>
            ) : null}
          </>
        )
      ) : null}
    </section>
  )
}
