import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useSession } from '../session/SessionProvider'

const DEMO_ACCOUNTS = [
  { label: 'BuildRight Supplies', role: 'Seller', email: 'buildright@example.com', password: 'buildright123' },
  { label: 'Metro Construction Mart', role: 'Seller', email: 'metro@example.com', password: 'metro123' },
  { label: 'New Horizon Traders (pending)', role: 'Seller', email: 'newhorizon@example.com', password: 'newhorizon123' },
  { label: 'Priya Sharma', role: 'Buyer', email: 'priya@example.com', password: 'priya123' },
]

export function LoginPage() {
  const { account, login } = useSession()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const mutation = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: (next) => {
      navigate(next.role === 'SELLER' ? '/seller' : '/', { replace: true })
    },
  })

  if (account) {
    return <Navigate to={account.role === 'SELLER' ? '/seller' : '/'} replace />
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    mutation.mutate()
  }

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.status === 401
        ? 'Wrong email or password. Please try again.'
        : mutation.error.message
      : null

  return (
    <section className="page page--narrow">
      <div className="login-card">
        <h1>Sign in</h1>
        <p className="muted">Sign in to buy, or to manage your own listings as a seller.</p>

        <form className="login-form" onSubmit={submit}>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>

          {errorMessage ? (
            <p className="field-error" role="alert">
              {errorMessage}
            </p>
          ) : null}

          <button type="submit" className="button" disabled={mutation.isPending}>
            {mutation.isPending ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="demo-accounts">
          <p className="demo-accounts__title">Demo accounts — click to fill</p>
          <ul>
            {DEMO_ACCOUNTS.map((demo) => (
              <li key={demo.email}>
                <button
                  type="button"
                  className="demo-account"
                  onClick={() => {
                    setEmail(demo.email)
                    setPassword(demo.password)
                  }}
                >
                  <span>{demo.label}</span>
                  <span className="muted">
                    {demo.role} · {demo.email}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  )
}
