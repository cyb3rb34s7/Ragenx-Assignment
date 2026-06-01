import { useState } from 'react'
import { api, ApiError } from '../../shared/api/client'
import { useReviewStore } from './store'

/** Modal to raise a query against a field. Reads the open field from the store; POSTs on submit. */
export default function RaiseQueryModal({ caseId }: { caseId: string }) {
  const fieldPath = useReviewStore((s) => s.modalFieldPath)
  const close = useReviewStore((s) => s.closeQuery)
  const [question, setQuestion] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!fieldPath) return null

  const submit = async () => {
    setSubmitting(true)
    setError(null)
    try {
      await api.raiseQuery({ case_id: caseId, field_path: fieldPath, question })
      setQuestion('')
      close()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to submit query.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black/40" onClick={close}>
      <div className="w-full max-w-md rounded-lg bg-white p-5 shadow-xl" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-semibold text-navy">Raise a query</h2>
        <p className="mb-3 text-sm text-slate-500">on {fieldPath}</p>
        <textarea
          className="h-28 w-full rounded border border-slate-300 p-2 text-sm"
          placeholder="Your question for the reviewer…"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        />
        {error && <p className="mt-2 text-sm text-confidence-low">{error}</p>}
        <div className="mt-3 flex justify-end gap-2">
          <button onClick={close} className="rounded px-3 py-1.5 text-sm text-slate-600">
            Cancel
          </button>
          <button
            onClick={submit}
            disabled={submitting || !question.trim()}
            className="rounded bg-brand px-3 py-1.5 text-sm text-white disabled:opacity-50"
          >
            {submitting ? 'Submitting…' : 'Submit'}
          </button>
        </div>
      </div>
    </div>
  )
}
