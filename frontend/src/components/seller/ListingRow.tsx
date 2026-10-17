import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { createListing, stopListing, updateListing, type CreateListingInput, type UpdateListingInput } from '../../api/seller'
import type { SellerListing } from '../../api/types'
import { formatPrice } from '../../lib/format'
import { listingStatus } from '../../lib/listingStatus'
import { hasListingErrors, validateListingDraft, type ListingDraft, type ListingErrors } from '../../lib/listingValidation'
import { useToast } from '../ToastProvider'

interface Props {
  listing: SellerListing
  sellerId: number
  canWrite: boolean
  onChanged: () => void
}

function toDraft(listing: SellerListing): ListingDraft {
  return {
    price: String(listing.price),
    stock: String(listing.stock),
    minOrderQty: String(listing.minOrderQty),
  }
}

export function ListingRow({ listing, sellerId, canWrite, onChanged }: Props) {
  const toast = useToast()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState<ListingDraft>(() => toDraft(listing))
  const [errors, setErrors] = useState<ListingErrors>({})
  const [confirmStop, setConfirmStop] = useState(false)

  const status = listingStatus(listing)

  const updateMutation = useMutation({
    mutationFn: (input: UpdateListingInput) => updateListing(sellerId, listing.id, input),
    onSuccess: () => {
      toast.success('Listing updated')
      setEditing(false)
      setErrors({})
      onChanged()
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        toast.error('This listing changed elsewhere — refreshing the latest values.')
        setEditing(false)
        onChanged()
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
      toast.error(error instanceof ApiError ? error.message : 'Could not update the listing')
    },
  })

  const stopMutation = useMutation({
    mutationFn: () => stopListing(sellerId, listing.id),
    onSuccess: () => {
      toast.success('Stopped selling this product')
      setConfirmStop(false)
      onChanged()
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : 'Could not stop the listing')
      setConfirmStop(false)
    },
  })

  const relistMutation = useMutation({
    mutationFn: () => {
      const input: CreateListingInput = {
        productId: listing.productId,
        price: Number(listing.price),
        stock: listing.stock,
        minOrderQty: listing.minOrderQty,
      }
      return createListing(sellerId, input)
    },
    onSuccess: () => {
      toast.success('Product re-listed')
      onChanged()
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.message : 'Could not re-list the product'),
  })

  const startEdit = () => {
    setDraft(toDraft(listing))
    setErrors({})
    setEditing(true)
  }

  const cancelEdit = () => {
    setEditing(false)
    setErrors({})
  }

  const save = () => {
    const validation = validateListingDraft(draft)
    if (hasListingErrors(validation)) {
      setErrors(validation)
      return
    }
    updateMutation.mutate({
      price: Number(draft.price),
      stock: Number(draft.stock),
      minOrderQty: Number(draft.minOrderQty),
      version: listing.version,
    })
  }

  const busy = updateMutation.isPending || stopMutation.isPending || relistMutation.isPending

  return (
    <tr className={!listing.active ? 'listing-row--stopped' : undefined}>
      <td>
        <span className="offer-seller">{listing.productName}</span>
        {listing.brand ? <span className="muted"> · {listing.brand}</span> : null}
      </td>

      <td className="num">
        {editing ? (
          <input
            type="number"
            min={0}
            step="0.01"
            className="inline-input"
            aria-label="Price"
            value={draft.price}
            onChange={(event) => setDraft((current) => ({ ...current, price: event.target.value }))}
          />
        ) : (
          formatPrice(listing.price)
        )}
        {editing && errors.price ? <span className="field-error">{errors.price}</span> : null}
      </td>

      <td className="num">
        {editing ? (
          <input
            type="number"
            min={0}
            step="1"
            className="inline-input"
            aria-label="Stock"
            value={draft.stock}
            onChange={(event) => setDraft((current) => ({ ...current, stock: event.target.value }))}
          />
        ) : (
          listing.stock
        )}
        {editing && errors.stock ? <span className="field-error">{errors.stock}</span> : null}
      </td>

      <td className="num">
        {editing ? (
          <input
            type="number"
            min={1}
            step="1"
            className="inline-input"
            aria-label="Minimum order quantity"
            value={draft.minOrderQty}
            onChange={(event) => setDraft((current) => ({ ...current, minOrderQty: event.target.value }))}
          />
        ) : (
          listing.minOrderQty
        )}
        {editing && errors.minOrderQty ? <span className="field-error">{errors.minOrderQty}</span> : null}
      </td>

      <td>
        <span className={`badge badge--${status.tone}`} title={status.hint}>
          {status.label}
        </span>
      </td>

      <td className="listing-actions">
        {!canWrite ? null : editing ? (
          <>
            <button type="button" className="button button--small" onClick={save} disabled={busy}>
              Save
            </button>
            <button type="button" className="button button--small button--ghost" onClick={cancelEdit} disabled={busy}>
              Cancel
            </button>
          </>
        ) : listing.active ? (
          confirmStop ? (
            <>
              <span className="muted">Stop selling?</span>
              <button
                type="button"
                className="button button--small button--danger"
                onClick={() => stopMutation.mutate()}
                disabled={busy}
              >
                Yes
              </button>
              <button
                type="button"
                className="button button--small button--ghost"
                onClick={() => setConfirmStop(false)}
                disabled={busy}
              >
                No
              </button>
            </>
          ) : (
            <>
              <button type="button" className="button button--small button--ghost" onClick={startEdit}>
                Edit
              </button>
              <button type="button" className="button button--small button--ghost" onClick={() => setConfirmStop(true)}>
                Stop selling
              </button>
            </>
          )
        ) : (
          <button
            type="button"
            className="button button--small"
            onClick={() => relistMutation.mutate()}
            disabled={busy}
          >
            Re-list
          </button>
        )}
      </td>
    </tr>
  )
}
