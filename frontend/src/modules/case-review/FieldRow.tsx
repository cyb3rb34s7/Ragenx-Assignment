import type { MergedField } from '../../shared/api/types'
import { useReviewStore } from './store'

// Small inline helpers (kept in the module, not a utils folder).
const humanize = (key: string) => key.replace(/_/g, ' ').replace(/\b\w/g, (ch) => ch.toUpperCase())

function confidenceClass(c: number): string {
  if (c < 0.8) return 'text-confidence-low'
  if (c <= 0.9) return 'text-confidence-medium'
  return 'text-confidence-high'
}

const statusClass: Record<string, string> = {
  new: 'bg-status-new',
  overridden: 'bg-status-overridden',
  unchanged: 'bg-status-unchanged',
}

/** One field: label, value (+ conflict view), source, confidence, status pill, raise-query. Dumb. */
export default function FieldRow({ path, name, field }: { path: string; name: string; field: MergedField }) {
  const openQuery = useReviewStore((s) => s.openQuery)
  const isConflict = field.status === 'overridden'

  return (
    <div className="flex items-start justify-between gap-4 border-b border-slate-200 py-2 last:border-0">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <span className="font-medium">{humanize(name)}</span>
          {field.status && (
            <span className={`rounded px-1.5 py-0.5 text-xs text-white ${statusClass[field.status]}`}>
              {field.status}
            </span>
          )}
        </div>

        {isConflict && field.previous_value ? (
          <div className="flex items-baseline gap-3">
            <span className="font-semibold">{field.value}</span>
            <span className="text-sm text-slate-400 line-through">{field.previous_value.value}</span>
          </div>
        ) : (
          <div className="font-semibold">{field.value}</div>
        )}

        <div className="text-xs text-slate-500">source: {field.source}</div>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-1">
        <span className={`text-sm font-semibold ${confidenceClass(field.confidence)}`}>
          {(field.confidence * 100).toFixed(0)}%
        </span>
        {isConflict && (
          <button
            onClick={() => openQuery(path)}
            className="rounded bg-brand px-2 py-1 text-xs text-white hover:opacity-90"
          >
            Raise Query
          </button>
        )}
      </div>
    </div>
  )
}
