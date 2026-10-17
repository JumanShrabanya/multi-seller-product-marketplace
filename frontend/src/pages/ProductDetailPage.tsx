import { Link, useParams } from 'react-router-dom'
import { EmptyState } from '../components/states'

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)

  if (!Number.isInteger(productId)) {
    return (
      <section className="page">
        <EmptyState title="Product not found" message="That product id is not valid." />
        <Link to="/" className="text-link">
          Back to products
        </Link>
      </section>
    )
  }

  return (
    <section className="page">
      <Link to="/" className="text-link">
        ← Back to products
      </Link>
      <header className="page__header">
        <h1>Product #{productId}</h1>
        <p className="muted">Offer comparison arrives in Module 7.</p>
      </header>
      <EmptyState title="Details coming next" message="The per-seller offer table lands in Module 7." />
    </section>
  )
}
