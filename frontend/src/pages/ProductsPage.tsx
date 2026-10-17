import { EmptyState } from '../components/states'

export function ProductsPage() {
  return (
    <section className="page">
      <header className="page__header">
        <h1>Products</h1>
        <p className="muted">Browse the marketplace catalogue across every approved seller.</p>
      </header>
      <EmptyState title="Catalogue coming next" message="Search, filters and offers arrive in Module 7." />
    </section>
  )
}
