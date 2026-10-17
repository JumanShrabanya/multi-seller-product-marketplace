import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchSellerListings } from '../api/seller'
import { AddProductPanel } from '../components/seller/AddProductPanel'
import { ListingRow } from '../components/seller/ListingRow'
import { EmptyState, ErrorState, LoadingState } from '../components/states'
import { StatusBadge } from '../components/StatusBadge'
import type { Account } from '../api/types'
import { useSession } from '../session/SessionProvider'

const PAGE_SIZE = 20

export function SellerPage() {
  const { account, isSeller } = useSession()

  if (!account || !isSeller) {
    return <Navigate to="/login" replace />
  }

  return <SellerDashboard account={account} />
}

function SellerDashboard({ account }: { account: Account }) {
  const { logout } = useSession()
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const canWrite = account.sellerStatus === 'APPROVED'

  const listingsQuery = useQuery({
    queryKey: ['seller-listings', account.userId, page],
    queryFn: ({ signal }) => fetchSellerListings(account.token, page, PAGE_SIZE, signal),
    placeholderData: keepPreviousData,
  })

  useEffect(() => {
    if (listingsQuery.error instanceof ApiError && listingsQuery.error.status === 401) {
      logout()
    }
  }, [listingsQuery.error, logout])

  const refresh = () => {
    void listingsQuery.refetch()
  }

  const data = listingsQuery.data

  return (
    <section className="page">
      <header className="seller-hero">
        <div className="page__header">
          <h1>{account.name}</h1>
          <p className="muted">
            {account.email} · Seller #{account.userId}
          </p>
        </div>
        {account.sellerStatus ? <StatusBadge status={account.sellerStatus} /> : null}
      </header>

      {!canWrite ? (
        <div className="notice notice--warn">
          {account.sellerStatus === 'PENDING'
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
          token={account.token}
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
                      token={account.token}
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
