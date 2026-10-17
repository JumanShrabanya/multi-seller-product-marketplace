import { apiRequest, buildQuery } from './client'
import type { CatalogueProduct, PageResponse, SellerListing } from './types'

export function fetchSellerListings(token: string, page: number, size: number, signal?: AbortSignal) {
  return apiRequest<PageResponse<SellerListing>>(`/api/seller/listings${buildQuery({ page, size })}`, {
    token,
    signal,
  })
}

export function searchCatalogue(token: string, query: string, signal?: AbortSignal) {
  return apiRequest<CatalogueProduct[]>(`/api/seller/products/search${buildQuery({ q: query })}`, {
    token,
    signal,
  })
}

export interface CreateListingInput {
  productId: number
  price: number
  stock: number
  minOrderQty: number
}

export interface UpdateListingInput {
  price: number
  stock: number
  minOrderQty: number
  version: number
}

export function createListing(token: string, input: CreateListingInput) {
  return apiRequest<SellerListing>('/api/seller/listings', { method: 'POST', body: input, token })
}

export function updateListing(token: string, id: number, input: UpdateListingInput) {
  return apiRequest<SellerListing>(`/api/seller/listings/${id}`, { method: 'PATCH', body: input, token })
}

export function stopListing(token: string, id: number) {
  return apiRequest<void>(`/api/seller/listings/${id}`, { method: 'DELETE', token })
}
