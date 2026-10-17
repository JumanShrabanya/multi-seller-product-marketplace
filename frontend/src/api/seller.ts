import { apiRequest, buildQuery } from './client'
import type { CatalogueProduct, PageResponse, Seller, SellerListing } from './types'

export function fetchMockSellers(signal?: AbortSignal) {
  return apiRequest<Seller[]>('/api/sellers/mock', { signal })
}

export function fetchSellerListings(sellerId: number, page: number, size: number, signal?: AbortSignal) {
  return apiRequest<PageResponse<SellerListing>>(`/api/seller/listings${buildQuery({ page, size })}`, {
    sellerId,
    signal,
  })
}

export function searchCatalogue(sellerId: number, query: string, signal?: AbortSignal) {
  return apiRequest<CatalogueProduct[]>(`/api/seller/products/search${buildQuery({ q: query })}`, {
    sellerId,
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

export function createListing(sellerId: number, input: CreateListingInput) {
  return apiRequest<SellerListing>('/api/seller/listings', { method: 'POST', body: input, sellerId })
}

export function updateListing(sellerId: number, id: number, input: UpdateListingInput) {
  return apiRequest<SellerListing>(`/api/seller/listings/${id}`, { method: 'PATCH', body: input, sellerId })
}

export function stopListing(sellerId: number, id: number) {
  return apiRequest<void>(`/api/seller/listings/${id}`, { method: 'DELETE', sellerId })
}
