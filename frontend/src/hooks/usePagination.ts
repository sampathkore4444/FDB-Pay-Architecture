import { useState, useCallback } from 'react';

export function usePagination(defaultSize = 20) {
  const [page, setPage] = useState(0);
  const [size] = useState(defaultSize);

  const nextPage = useCallback(() => setPage((p) => p + 1), []);
  const prevPage = useCallback(() => setPage((p) => Math.max(0, p - 1)), []);
  const goToPage = useCallback((p: number) => setPage(p), []);

  return { page, size, nextPage, prevPage, goToPage };
}
