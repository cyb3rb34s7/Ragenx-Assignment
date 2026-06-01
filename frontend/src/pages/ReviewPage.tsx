import { useParams } from 'react-router-dom'

export default function ReviewPage() {
  const { caseId } = useParams<{ caseId: string }>()

  return (
    <div className="min-h-screen">
      <header className="bg-navy px-6 py-4 text-white">
        <h1 className="text-xl font-semibold">PV Case Review</h1>
        <p className="text-sm text-teal">Case {caseId}</p>
      </header>
      <main className="p-6">
        <p className="text-navy">
          Reviewer screen — toolchain ready. Build the UI here against{' '}
          <code className="rounded bg-slate-200 px-1">http://localhost:8412</code>.
        </p>
      </main>
    </div>
  )
}
