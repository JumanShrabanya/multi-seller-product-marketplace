export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  code: string
  message: string
  fieldErrors: FieldError[]
}

export interface ProductSummary {
  id: number
  name: string
  brand: string
  category: string
  unit: string
  lowestPrice: number | null
  offerCount: number
  currentlyUnavailable: boolean
}

export interface Offer {
  sellerId: number
  sellerName: string
  sellerCity: string
  price: number
  stock: number
  minOrderQty: number
}

export interface ProductDetail {
  id: number
  name: string
  brand: string
  category: string
  unit: string
  description: string
  lowestPrice: number | null
  offerCount: number
  currentlyUnavailable: boolean
  offers: Offer[]
}

export type ProductSearchParams = {
  q?: string
  category?: string
  brand?: string
  minPrice?: number
  maxPrice?: number
  inStockOnly?: boolean
  sort?: string
  page?: number
  size?: number
}

export type SellerStatus = 'APPROVED' | 'PENDING' | 'REJECTED'

export type UserRole = 'BUYER' | 'SELLER'

export interface Account {
  token: string
  role: UserRole
  userId: number
  name: string
  email: string
  sellerStatus: SellerStatus | null
}

export interface SellerListing {
  id: number
  productId: number
  productName: string
  brand: string
  price: number
  stock: number
  minOrderQty: number
  active: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export interface CatalogueProduct {
  id: number
  name: string
  brand: string
  category: string
  unit: string
}
