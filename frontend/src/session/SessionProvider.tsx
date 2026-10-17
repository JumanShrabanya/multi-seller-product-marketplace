import { useQuery } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { fetchMockSellers } from '../api/seller'
import type { Seller } from '../api/types'

export type SessionMode = 'BUYER' | 'SELLER'

const MODE_KEY = 'bajrix.mode'
const SELLER_KEY = 'bajrix.sellerId'

interface SessionValue {
  mode: SessionMode
  sellerId: number | null
  seller: Seller | null
  sellers: Seller[]
  sellersLoading: boolean
  sellersError: boolean
  setMode: (mode: SessionMode) => void
  setSellerId: (sellerId: number) => void
}

const SessionContext = createContext<SessionValue | null>(null)

function readStoredMode(): SessionMode {
  return localStorage.getItem(MODE_KEY) === 'SELLER' ? 'SELLER' : 'BUYER'
}

function readStoredSellerId(): number | null {
  const raw = localStorage.getItem(SELLER_KEY)
  if (raw === null) {
    return null
  }
  const parsed = Number(raw)
  return Number.isInteger(parsed) ? parsed : null
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<SessionMode>(readStoredMode)
  const [sellerId, setSellerIdState] = useState<number | null>(readStoredSellerId)

  const sellersQuery = useQuery({
    queryKey: ['mock-sellers'],
    queryFn: ({ signal }) => fetchMockSellers(signal),
    staleTime: Infinity,
  })

  const sellers = sellersQuery.data ?? []

  useEffect(() => {
    localStorage.setItem(MODE_KEY, mode)
  }, [mode])

  useEffect(() => {
    if (sellerId === null) {
      localStorage.removeItem(SELLER_KEY)
    } else {
      localStorage.setItem(SELLER_KEY, String(sellerId))
    }
  }, [sellerId])

  useEffect(() => {
    if (mode !== 'SELLER' || sellerId !== null || sellers.length === 0) {
      return
    }
    setSellerIdState(sellers[0].id)
  }, [mode, sellerId, sellers])

  const setMode = useCallback((next: SessionMode) => setModeState(next), [])
  const setSellerId = useCallback((next: number) => setSellerIdState(next), [])

  const seller = useMemo(() => sellers.find((candidate) => candidate.id === sellerId) ?? null, [sellers, sellerId])

  const value = useMemo<SessionValue>(
    () => ({
      mode,
      sellerId,
      seller,
      sellers,
      sellersLoading: sellersQuery.isPending,
      sellersError: sellersQuery.isError,
      setMode,
      setSellerId,
    }),
    [mode, sellerId, seller, sellers, sellersQuery.isPending, sellersQuery.isError, setMode, setSellerId],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession(): SessionValue {
  const context = useContext(SessionContext)
  if (context === null) {
    throw new Error('useSession must be used within a SessionProvider')
  }
  return context
}
