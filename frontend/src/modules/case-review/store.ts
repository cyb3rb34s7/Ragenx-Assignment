import { create } from 'zustand'

/** UI state for the case-review screen. Server data (the case) lives in useCase, NEVER here. */
interface ReviewUiState {
  sortLowFirst: boolean
  conflictsOnly: boolean
  classification: string | null
  modalFieldPath: string | null // null = modal closed

  toggleSort: () => void
  toggleConflictsOnly: () => void
  setClassification: (value: string | null) => void
  openQuery: (fieldPath: string) => void
  closeQuery: () => void
}

export const useReviewStore = create<ReviewUiState>((set) => ({
  sortLowFirst: false,
  conflictsOnly: false,
  classification: null,
  modalFieldPath: null,

  toggleSort: () => set((s) => ({ sortLowFirst: !s.sortLowFirst })),
  toggleConflictsOnly: () => set((s) => ({ conflictsOnly: !s.conflictsOnly })),
  setClassification: (value) => set({ classification: value }),
  openQuery: (fieldPath) => set({ modalFieldPath: fieldPath }),
  closeQuery: () => set({ modalFieldPath: null }),
}))
