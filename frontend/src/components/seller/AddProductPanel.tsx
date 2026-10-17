import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { createListing, searchCatalogue, type CreateListingInput } from '../../api/seller'
import type { CatalogueProduct } from '../../api/types'
import { hasListingErrors, validateListingDraft, type ListingDraft, type ListingErrors } from '../../lib/listingValidation'
import { useDebouncedValue } from '../../lib/useDebouncedValue'
import { useToast } from '../ToastProvider'

interface Props {
  token: string
  onClose: () => void
  onCreated: () => void
}

const EMPTY_DRAFT: ListingDraft = { price: '', stock: '', minOrderQty: '1' }

export function AddProductPanel({ token, onClose, onCreated }: Props) {
  const toast = useToast()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebouncedValue(search, 300)
  const [selected, setSelected] = useState<CatalogueProduct | null>(null)
  const [draft, setDraft] = useState<ListingDraft>(EMPTY_DRAFT)
  const [errors, setErrors] = useState<ListingErrors>({})
  const [formError, setFormError] = useState<string | null>(null)

  const catalogueQuery = useQuery({
    queryKey: ['catalogue', token, debouncedSearch],
    queryFn: ({ signal }) => searchCatalogue(token, debouncedSearch, signal),
  })

  const createMutation = useMutation({
    mutationFn: (input: CreateListingInput) => createListing(token, input),
    onSuccess: () => {
      toast.success('Product added to your catalogue')
      onCreated()
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        setFormError('You already sell this product. Edit the existing listing instead.')
        return
      }
      if (error instanceof ApiError && error.status === 404) {
        setFormError('That product no longer exists.')
        return
      }
      if (error instanceof ApiError && error.status === 400 && error.fieldErrors.length > 0) {
        const fieldErrors: ListingErrors = {}
        for (const fieldError of error.fieldErrors) {
          if (fieldError.field === 'price' || fieldError.field === 'stock' || fieldError.field === 'minOrderQty') {
            fieldErrors[fieldError.field] = fieldError.message
          }
        }
        setErrors(fieldErrors)
        return
      }
      setFormError(error instanceof ApiError ? error.message : 'Could not add the product')
    },
  })

  const submit = () => {
    setFormError(null)
    if (selected === null) {
      setFormError('Select a product to add.')
      return
    }
    const validation = validateListingDraft(draft)
    if (hasListingErrors(validation)) {
      setErrors(validation)
      return
    }
    setErrors({})
    createMutation.mutate({
      productId: selected.id,
      price: Number(draft.price),
      stock: Number(draft.stock),
      minOrderQty: Number(draft.minOrderQty),
    })
  }

  return (
    <section className="add-panel" aria-label="Add a product">
      <div className="add-panel__header">
        <h2>Add a product</h2>
        <button type="button" className="button button--small button--ghost" onClick={onClose}>
          Close
        </button>
      </div>

      <label className="field field--search">
        <span>Find a product in the catalogue</span>
        <input
          type="search"
          placeholder="Search by name or brand…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </label>

      <div className="catalogue-results">
        {catalogueQuery.isPending ? <p className="muted">Searching…</p> : null}
        {catalogueQuery.isError ? <p className="field-error">Could not load the catalogue.</p> : null}
        {catalogueQuery.data && catalogueQuery.data.length === 0 ? (
          <p className="muted">No matching products.</p>
        ) : null}
        {catalogueQuery.data && catalogueQuery.data.length > 0 ? (
          <ul className="catalogue-list">
            {catalogueQuery.data.map((product) => (
              <li key={product.id}>
                <button
                  type="button"
                  className={`catalogue-option${selected?.id === product.id ? ' is-selected' : ''}`}
                  onClick={() => setSelected(product)}
                >
                  <span>{product.name}</span>
                  <span className="muted">
                    {product.brand} · {product.unit}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}
      </div>

      {selected ? <p className="add-panel__selected">Selected: <strong>{selected.name}</strong></p> : null}

      <div className="add-panel__form">
        <label className="field field--small">
          <span>Price ₹</span>
          <input
            type="number"
            min={0}
            step="0.01"
            value={draft.price}
            onChange={(event) => setDraft((current) => ({ ...current, price: event.target.value }))}
          />
          {errors.price ? <span className="field-error">{errors.price}</span> : null}
        </label>

        <label className="field field--small">
          <span>Stock</span>
          <input
            type="number"
            min={0}
            step="1"
            value={draft.stock}
            onChange={(event) => setDraft((current) => ({ ...current, stock: event.target.value }))}
          />
          {errors.stock ? <span className="field-error">{errors.stock}</span> : null}
        </label>

        <label className="field field--small">
          <span>Min. order</span>
          <input
            type="number"
            min={1}
            step="1"
            value={draft.minOrderQty}
            onChange={(event) => setDraft((current) => ({ ...current, minOrderQty: event.target.value }))}
          />
          {errors.minOrderQty ? <span className="field-error">{errors.minOrderQty}</span> : null}
        </label>
      </div>

      {formError ? <p className="field-error">{formError}</p> : null}

      <div className="add-panel__actions">
        <button type="button" className="button" onClick={submit} disabled={createMutation.isPending}>
          Add listing
        </button>
        <button type="button" className="button button--ghost" onClick={onClose} disabled={createMutation.isPending}>
          Cancel
        </button>
      </div>
    </section>
  )
}
