import { useCallback, useEffect, useState } from "react";
import { api, ApiError } from "../../shared/api/client";
import type { CaseState } from "../../shared/api/types";

type State =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; data: CaseState };

/** Fetches the merged case. Server state lives here, not in components or the store. */
export function useCase(caseId: string) {
  const [state, setState] = useState<State>({ status: "loading" });

  const load = useCallback(() => {
    setState({ status: "loading" });
    api
      .getCase(caseId)
      .then((data) => setState({ status: "ready", data }))
      .catch((e) =>
        setState({
          status: "error",
          // ApiError = backend said no; otherwise the backend is unreachable (→ JSON fallback).
          message:
            e instanceof ApiError
              ? `${e.code}: ${e.message}`
              : "Backend unreachable",
        }),
      );
  }, [caseId]);

  useEffect(load, [load]);

  return { state, reload: load };
}
