import { Link } from 'react-router-dom'
import { EmptyState } from '../components/states'

export function NotFoundPage() {
  return (
    <section className="page">
      <EmptyState title="Page not found" message="The page you were looking for does not exist." />
      <Link to="/" className="text-link">
        Back to products
      </Link>
    </section>
  )
}
