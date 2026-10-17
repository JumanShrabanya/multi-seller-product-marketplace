import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { fetchProducts } from '../api/products'
import type { ProductSearchParams, ProductSummary } from '../api/types'
import { EmptyState, ErrorState, LoadingState } from '../components/states'
import { formatPrice } from '../lib/format'
import { useDebouncedValue } from '../lib/useDebouncedValue'

const PAGE_SIZE = 12

const SORT_OPTIONS = [
  { value: 'name', label: 'Name (A–Z)' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
]

interface FilterDraft {
  q: string
  category: string
  brand: string
  minPrice: string
  maxPrice: string
}

function numberParam(params: URLSearchParams, key: string): number | undefined {
  const raw = params.get(key)
  if (raw === null || raw === '') {
    return undefined
  }
  const value = Number(raw)
  return Number.isFinite(value) ? value : undefined
}

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [filters, setFilters] = useState<FilterDraft>(() => ({
    q: searchParams.get('q') ?? '',
    category: searchParams.get('category') ?? '',
    brand: searchParams.get('brand') ?? '',
    minPrice: searchParams.get('minPrice') ?? '',
    maxPrice: searchParams.get('maxPrice') ?? '',
  }))
  const debouncedFilters = useDebouncedValue(filters, 400)

  useEffect(() => {
    setSearchParams(
      (previous) => {
        const next = new URLSearchParams(previous)
        const pairs: [string, string][] = [
          ['q', debouncedFilters.q],
          ['category', debouncedFilters.category],
          ['brand', debouncedFilters.brand],
          ['minPrice', debouncedFilters.minPrice],
          ['maxPrice', debouncedFilters.maxPrice],
        ]
        let changed = false
        for (const [key, value] of pairs) {
          const trimmed = value.trim()
          if ((next.get(key) ?? '') !== trimmed) {
            changed = true
          }
          if (trimmed === '') {
            next.delete(key)
          } else {
            next.set(key, trimmed)
          }
        }
        if (!changed) {
          return previous
        }
        next.delete('page')
        return next
      },
      { replace: true },
    )
  }, [debouncedFilters, setSearchParams])

  const params: ProductSearchParams = {
    q: searchParams.get('q') ?? undefined,
    category: searchParams.get('category') ?? undefined,
    brand: searchParams.get('brand') ?? undefined,
    minPrice: numberParam(searchParams, 'minPrice'),
    maxPrice: numberParam(searchParams, 'maxPrice'),
    inStockOnly: searchParams.get('inStockOnly') === 'true' ? true : undefined,
    sort: searchParams.get('sort') ?? undefined,
    page: numberParam(searchParams, 'page') ?? 0,
    size: PAGE_SIZE,
  }

  const productsQuery = useQuery({
    queryKey: ['products', params],
    queryFn: ({ signal }) => fetchProducts(params, signal),
    placeholderData: keepPreviousData,
  })

  const setParam = (key: string, value: string | undefined, resetPage = true) => {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous)
      if (value === undefined || value === '') {
        next.delete(key)
      } else {
        next.set(key, value)
      }
      if (resetPage) {
        next.delete('page')
      }
      return next
    })
  }

  const page = params.page ?? 0
  const data = productsQuery.data
  const hasFilters =
    filters.q !== '' ||
    filters.category !== '' ||
    filters.brand !== '' ||
    filters.minPrice !== '' ||
    filters.maxPrice !== '' ||
    params.inStockOnly === true

  const clearFilters = () => {
    setFilters({ q: '', category: '', brand: '', minPrice: '', maxPrice: '' })
    setSearchParams({}, { replace: true })
  }

  return (
    <section className="page">
      <header className="page__header">
        <h1>Products</h1>
        <p className="muted">Compare offers from every approved seller in one place.</p>
      </header>

      <div className="filters">
        <label className="field field--search">
          <span>Search</span>
          <input
            type="search"
            placeholder="Product or brand…"
            value={filters.q}
            onChange={(event) => setFilters((current) => ({ ...current, q: event.target.value }))}
          />
        </label>

        <label className="field">
          <span>Category</span>
          <input
            type="text"
            placeholder="e.g. Cement"
            value={filters.category}
            onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))}
          />
        </label>

        <label className="field">
          <span>Brand</span>
          <input
            type="text"
            placeholder="e.g. UltraTech"
            value={filters.brand}
            onChange={(event) => setFilters((current) => ({ ...current, brand: event.target.value }))}
          />
        </label>

        <label className="field field--small">
          <span>Min ₹</span>
          <input
            type="number"
            min={0}
            value={filters.minPrice}
            onChange={(event) => setFilters((current) => ({ ...current, minPrice: event.target.value }))}
          />
        </label>

        <label className="field field--small">
          <span>Max ₹</span>
          <input
            type="number"
            min={0}
            value={filters.maxPrice}
            onChange={(event) => setFilters((current) => ({ ...current, maxPrice: event.target.value }))}
          />
        </label>

        <label className="field">
          <span>Sort</span>
          <select
            value={params.sort ?? 'name'}
            onChange={(event) => setParam('sort', event.target.value)}
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="checkbox">
          <input
            type="checkbox"
            checked={params.inStockOnly === true}
            onChange={(event) => setParam('inStockOnly', event.target.checked ? 'true' : undefined)}
          />
          <span>In stock only</span>
        </label>

        {hasFilters ? (
          <button type="button" className="button button--ghost" onClick={clearFilters}>
            Clear filters
          </button>
        ) : null}
      </div>

      {productsQuery.isPending ? <LoadingState label="Loading products…" /> : null}

      {productsQuery.isError ? (
        <ErrorState
          message={
            productsQuery.error instanceof ApiError
              ? productsQuery.error.message
              : 'Something went wrong loading products.'
          }
          onRetry={() => productsQuery.refetch()}
        />
      ) : null}

      {data && !productsQuery.isError ? (
        <>
          <p className="results-summary muted">
            {data.totalElements} product{data.totalElements === 1 ? '' : 's'}
            {data.totalPages > 1 ? ` · page ${data.page + 1} of ${data.totalPages}` : ''}
          </p>

          {data.content.length === 0 ? (
            <EmptyState title="No products match your search" message="Try clearing filters or a different term." />
          ) : (
            <ul className="product-grid">
              {data.content.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </ul>
          )}

          {data.totalPages > 1 ? (
            <nav className="pagination" aria-label="Pagination">
              <button
                type="button"
                className="button button--ghost"
                disabled={data.page === 0}
                onClick={() => setParam('page', String(data.page - 1), false)}
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
                onClick={() => setParam('page', String(data.page + 1), false)}
              >
                Next →
              </button>
            </nav>
          ) : null}
        </>
      ) : null}
    </section>
  )
}

function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <li className="product-card">
      <Link to={`/products/${product.id}`} className="product-card__link">
        <div className="product-card__meta">
          <span className="tag">{product.category}</span>
          <span className="muted">{product.brand}</span>
        </div>
        <h2 className="product-card__name">{product.name}</h2>
        <p className="muted product-card__unit">{product.unit}</p>
        <div className="product-card__footer">
          {product.currentlyUnavailable || product.lowestPrice === null ? (
            <span className="badge badge--rejected">Currently unavailable</span>
          ) : (
            <>
              <span className="product-card__price">{formatPrice(product.lowestPrice)}</span>
              <span className="muted">
                {product.offerCount} seller{product.offerCount === 1 ? '' : 's'}
              </span>
            </>
          )}
        </div>
      </Link>
    </li>
  )
}
