import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchProduct } from '../api/products'
import type { Offer } from '../api/types'
import { EmptyState, ErrorState, LoadingState } from '../components/states'
import { formatPrice } from '../lib/format'

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)
  const isValidId = Number.isInteger(productId) && productId > 0

  const productQuery = useQuery({
    queryKey: ['product', productId],
    queryFn: ({ signal }) => fetchProduct(productId, signal),
    enabled: isValidId,
  })

  if (!isValidId) {
    return (
      <section className="page">
        <EmptyState title="Product not found" message="That product id is not valid." />
        <Link to="/" className="text-link">
          ← Back to products
        </Link>
      </section>
    )
  }

  if (productQuery.isPending) {
    return (
      <section className="page">
        <LoadingState label="Loading product…" />
      </section>
    )
  }

  if (productQuery.isError) {
    const error = productQuery.error
    const notFound = error instanceof ApiError && error.status === 404
    return (
      <section className="page">
        <ErrorState
          message={notFound ? 'This product was not found.' : error instanceof ApiError ? error.message : 'Something went wrong.'}
          onRetry={notFound ? undefined : () => productQuery.refetch()}
        />
        <Link to="/" className="text-link">
          ← Back to products
        </Link>
      </section>
    )
  }

  const product = productQuery.data
  const bestPrice = product.offers.reduce<number | null>(
    (lowest, offer) => (lowest === null || offer.price < lowest ? offer.price : lowest),
    null,
  )

  return (
    <section className="page">
      <nav className="breadcrumb" aria-label="Breadcrumb">
        <Link to="/">Products</Link>
        <span aria-hidden="true">/</span>
        <span className="breadcrumb__current">{product.name}</span>
      </nav>

      <article className="product-hero">
        <div>
          <div className="product-card__meta">
            <span className="tag">{product.category}</span>
            <span className="muted">{product.brand}</span>
          </div>
          <h1>{product.name}</h1>
          <p className="muted">{product.unit}</p>
          {product.description ? <p className="product-description">{product.description}</p> : null}
        </div>

        <aside className="product-hero__aside">
          {product.currentlyUnavailable ? (
            <>
              <span className="badge badge--rejected">Currently unavailable</span>
              <span className="muted">No approved seller has this in stock right now.</span>
            </>
          ) : (
            <>
              <span className="muted">Lowest price</span>
              <span className="product-hero__price">
                {product.lowestPrice !== null ? formatPrice(product.lowestPrice) : '—'}
              </span>
              <span className="muted">
                from {product.offerCount} seller{product.offerCount === 1 ? '' : 's'}
              </span>
            </>
          )}
        </aside>
      </article>

      <h2 className="section-title">Seller offers</h2>

      {product.offers.length === 0 ? (
        <EmptyState title="No offers available" message="Check back later — no seller currently offers this product." />
      ) : (
        <div className="table-wrap">
          <table className="offers-table">
            <thead>
              <tr>
                <th scope="col">Seller</th>
                <th scope="col">City</th>
                <th scope="col" className="num">
                  Price
                </th>
                <th scope="col" className="num">
                  Stock
                </th>
                <th scope="col" className="num">
                  Min. order
                </th>
              </tr>
            </thead>
            <tbody>
              {product.offers.map((offer) => (
                <OfferRow key={offer.sellerId} offer={offer} isBest={bestPrice !== null && offer.price === bestPrice} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function OfferRow({ offer, isBest }: { offer: Offer; isBest: boolean }) {
  return (
    <tr className={isBest ? 'offers-table__row--best' : undefined}>
      <td>
        <span className="offer-seller">{offer.sellerName}</span>
        {isBest ? <span className="badge badge--approved offer-best-badge">Best price</span> : null}
      </td>
      <td>{offer.sellerCity}</td>
      <td className="num">{formatPrice(offer.price)}</td>
      <td className="num">{offer.stock}</td>
      <td className="num">{offer.minOrderQty}</td>
    </tr>
  )
}
