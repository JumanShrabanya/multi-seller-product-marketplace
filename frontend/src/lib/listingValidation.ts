export interface ListingDraft {
  price: string
  stock: string
  minOrderQty: string
}

export type ListingErrors = Partial<Record<keyof ListingDraft, string>>

export function validateListingDraft(draft: ListingDraft): ListingErrors {
  const errors: ListingErrors = {}

  const price = Number(draft.price)
  if (draft.price.trim() === '' || Number.isNaN(price)) {
    errors.price = 'Price is required'
  } else if (price <= 0) {
    errors.price = 'Price must be greater than 0'
  } else if (!/^\d+(\.\d{1,2})?$/.test(draft.price.trim())) {
    errors.price = 'Use a number with up to 2 decimals'
  }

  const stock = Number(draft.stock)
  if (draft.stock.trim() === '' || !Number.isInteger(stock)) {
    errors.stock = 'Stock must be a whole number'
  } else if (stock < 0) {
    errors.stock = 'Stock cannot be negative'
  }

  const minOrderQty = Number(draft.minOrderQty)
  if (draft.minOrderQty.trim() === '' || !Number.isInteger(minOrderQty)) {
    errors.minOrderQty = 'Minimum order must be a whole number'
  } else if (minOrderQty < 1) {
    errors.minOrderQty = 'Minimum order must be at least 1'
  } else if (errors.stock === undefined && stock > 0 && minOrderQty > stock) {
    errors.minOrderQty = 'Minimum order cannot exceed stock'
  }

  return errors
}

export function hasListingErrors(errors: ListingErrors): boolean {
  return Object.keys(errors).length > 0
}
