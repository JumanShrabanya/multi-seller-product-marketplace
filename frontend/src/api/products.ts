import { apiRequest, buildQuery } from './client'
import type { PageResponse, ProductDetail, ProductSearchParams, ProductSummary } from './types'

export function fetchProducts(params: ProductSearchParams, signal?: AbortSignal) {
  return apiRequest<PageResponse<ProductSummary>>(`/api/products${buildQuery(params)}`, { signal })
}

export function fetchProduct(id: number, signal?: AbortSignal) {
  return apiRequest<ProductDetail>(`/api/products/${id}`, { signal })
}
