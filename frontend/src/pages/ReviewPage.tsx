import { useParams } from 'react-router-dom'
import type { MergedField } from '../shared/api/types'
import { useCase } from '../modules/case-review/useCase'
import { useReviewStore } from '../modules/case-review/store'
import FieldRow from '../modules/case-review/FieldRow'
import RaiseQueryModal from '../modules/case-review/RaiseQueryModal'

const SECTION_TITLES: Record<string, string> = {
  patient: 'Patient',
  suspect_drug: 'Suspect Drug',
  adverse_event: 'Adverse Event',
  reporter: 'Reporter',
}

export default function ReviewPage() {
  const { caseId = 'PV-2026-0451' } = useParams()
  const { state, reload } = useCase(caseId)
  const { sortLowFirst, conflictsOnly, classification, setClassification, toggleSort, toggleConflictsOnly } =
    useReviewStore()

  if (state.status === 'loading') return <div className="p-8 text-navy">Loading…</div>
  if (state.status === 'error') {
    return (
      <div className="p-8">
        <p className="text-confidence-low">Error: {state.message}</p>
        <button onClick={reload} className="mt-3 rounded bg-brand px-3 py-1.5 text-white">
          Retry
        </button>
      </div>
    )
  }

  const c = state.data

  return (
    <div className="min-h-screen pb-16">
      <header className="bg-navy px-6 py-4 text-white">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold">PV Case Review</h1>
            <p className="text-sm text-teal">
              {c.case_id} · v{c.version}
            </p>
          </div>
          <label className="text-sm">
            Classification{' '}
            <select
              className="ml-1 rounded bg-white px-2 py-1 text-navy"
              value={classification ?? c.case_classification ?? 'null'}
              onChange={(e) => setClassification(e.target.value === 'null' ? null : e.target.value)}
            >
              <option value="significant">significant</option>
              <option value="non-significant">non-significant</option>
              <option value="null">null</option>
            </select>
          </label>
        </div>
      </header>

      <div className="flex items-center gap-5 border-b bg-white px-6 py-2 text-sm">
        <label className="flex items-center gap-1">
          <input type="checkbox" checked={sortLowFirst} onChange={toggleSort} /> Sort by confidence (low first)
        </label>
        <label className="flex items-center gap-1">
          <input type="checkbox" checked={conflictsOnly} onChange={toggleConflictsOnly} /> Conflicts only
        </label>
      </div>

      {c.missing_fields.length > 0 && (
        <div className="mx-6 mt-4 rounded border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          <strong>AI could not extract:</strong> {c.missing_fields.join(', ')}
        </div>
      )}

      <main className="space-y-6 p-6">
        {Object.entries(c.sections).map(([sectionKey, fields]) => {
          let entries = Object.entries(fields) as [string, MergedField][]
          if (conflictsOnly) entries = entries.filter(([, f]) => f.status === 'overridden')
          if (sortLowFirst) entries = [...entries].sort((a, b) => a[1].confidence - b[1].confidence)
          if (entries.length === 0) return null
          return (
            <section key={sectionKey}>
              <h2 className="mb-2 font-semibold text-brand">{SECTION_TITLES[sectionKey] ?? sectionKey}</h2>
              <div className="rounded bg-white px-4 shadow-sm">
                {entries.map(([name, field]) => (
                  <FieldRow key={name} path={`${sectionKey}.${name}`} name={name} field={field} />
                ))}
              </div>
            </section>
          )
        })}
      </main>

      <RaiseQueryModal caseId={c.case_id} />
    </div>
  )
}
