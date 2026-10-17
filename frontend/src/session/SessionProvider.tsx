import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { login as loginRequest } from '../api/auth'
import type { Account } from '../api/types'

const ACCOUNT_KEY = 'bajrix.account'

interface SessionValue {
  account: Account | null
  isAuthenticated: boolean
  isSeller: boolean
  login: (email: string, password: string) => Promise<Account>
  logout: () => void
}

const SessionContext = createContext<SessionValue | null>(null)

function readStoredAccount(): Account | null {
  const raw = localStorage.getItem(ACCOUNT_KEY)
  if (raw === null) {
    return null
  }
  try {
    return JSON.parse(raw) as Account
  } catch {
    return null
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [account, setAccount] = useState<Account | null>(readStoredAccount)

  const login = useCallback(async (email: string, password: string) => {
    const next = await loginRequest(email, password)
    localStorage.setItem(ACCOUNT_KEY, JSON.stringify(next))
    setAccount(next)
    return next
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(ACCOUNT_KEY)
    setAccount(null)
  }, [])

  const value = useMemo<SessionValue>(
    () => ({
      account,
      isAuthenticated: account !== null,
      isSeller: account?.role === 'SELLER',
      login,
      logout,
    }),
    [account, login, logout],
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
