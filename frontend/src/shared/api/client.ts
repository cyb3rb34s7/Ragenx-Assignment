import ky from 'ky'
import type { CaseState, Query } from './types'

// Backend base URL from frontend/.env (see .env.example).
// ⚠️ Flagged fallback: if VITE_API_BASE_URL is unset, default to the known local dev port.
// This is a deliberate dev convenience, not a silent error-swallow.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8412'

/**
 * Every backend response has this shape — success OR error — so we branch on `success`,
 * not on the HTTP status code. (Discriminated union: TS narrows `data` vs `error` by `success`.)
 */
type ApiEnvelope<T> =
  | { success: true; data: T; trace_id: string }
  | { success: false; error: { code: string; message: string; details?: unknown }; trace_id: string }

/** Thrown when the backend returns `success: false`. Components catch this and read `.code`. */
export class ApiError extends Error {
  readonly code: string
  readonly traceId: string

  constructor(code: string, message: string, traceId: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
  }
}

/**
 * One configured ky instance.
 * - prefix: every call is joined onto this base URL (ky v2 renamed `prefixUrl` → `prefix`).
 * - throwHttpErrors:false: ky won't throw on 4xx/5xx — our error body is still JSON, so we
 *   read it and branch on `success` ourselves. One code path for success and error.
 */
const http = ky.create({ prefix: API_BASE_URL, throwHttpErrors: false })

/** Run a request, unwrap the envelope: return `data` on success, throw `ApiError` otherwise. */
async function unwrap<T>(call: Promise<Response>): Promise<T> {
  const response = await call
  const body = (await response.json()) as ApiEnvelope<T>
  if (body.success) return body.data
  throw new ApiError(body.error.code, body.error.message, body.trace_id)
}

/** The whole API surface the UI needs. */
export const api = {
  getCase: (caseId: string) => unwrap<CaseState>(http.get(`cases/${caseId}`)),

  raiseQuery: (q: { case_id: string; field_path: string; question: string }) =>
    unwrap<Query>(http.post('queries', { json: q })),

  listQueries: (caseId: string) =>
    unwrap<Query[]>(http.get('queries', { searchParams: { caseId } })),
}
