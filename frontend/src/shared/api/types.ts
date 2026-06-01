// TypeScript shapes mirroring the backend JSON exactly (snake_case on the wire).

export type FieldStatus = 'new' | 'overridden' | 'unchanged'

/** A raw extracted leaf (also used as `previous_value`). */
export interface ExtractedField {
  value: string
  confidence: number // 0..1
  source: string // e.g. "p.4 §1"
}

/** A field in the merged case. `status` is absent when the field was untouched by the latest follow-up. */
export interface MergedField extends ExtractedField {
  status?: FieldStatus
  previous_value?: ExtractedField // present only when status === 'overridden'
}

/** section name -> field name -> field. */
export type Sections = Record<string, Record<string, MergedField>>

export interface CaseState {
  case_id: string
  version: number
  case_classification: string | null
  extracted_at: string
  source_document: string
  sections: Sections
  missing_fields: string[]
}

export interface Query {
  id: string
  case_id: string
  field_path: string
  question: string
}
