import type { SellerListing } from '../api/types'

export interface ListingStatus {
  label: string
  tone: 'approved' | 'pending' | 'rejected'
  hint: string
}

export function listingStatus(listing: SellerListing): ListingStatus {
  if (!listing.active) {
    return { label: 'Stopped', tone: 'rejected', hint: 'Not shown to buyers.' }
  }
  if (listing.stock === 0) {
    return { label: 'Out of stock', tone: 'pending', hint: 'Hidden from buyers until stock is added.' }
  }
  if (listing.stock < listing.minOrderQty) {
    return { label: 'Below MOQ', tone: 'pending', hint: 'Hidden from buyers until stock reaches the minimum order.' }
  }
  return { label: 'Active', tone: 'approved', hint: 'Visible to buyers.' }
}
