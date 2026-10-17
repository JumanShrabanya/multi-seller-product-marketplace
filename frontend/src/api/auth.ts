import { apiRequest } from './client'
import type { Account } from './types'

export function login(email: string, password: string) {
  return apiRequest<Account>('/api/auth/login', { method: 'POST', body: { email, password } })
}
