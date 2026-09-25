import { useEffect, useState } from 'react';
import { TabId, isTabId } from '../components/common/Navigation';

const readTab = (fallback: TabId): TabId => {
  const value = window.location.hash.replace(/^#\/?/, '');
  return isTabId(value) ? value : fallback;
};

/** Keeps the active tab in the URL hash so refresh, back/forward and deep links work. */
export function useHashTab(fallback: TabId = 'dashboard'): TabId {
  const [tab, setTab] = useState<TabId>(() => readTab(fallback));

  useEffect(() => {
    const onHashChange = () => setTab(readTab(fallback));
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, [fallback]);

  return tab;
}
