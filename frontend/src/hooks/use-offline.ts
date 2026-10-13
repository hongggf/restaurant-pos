import { useState, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export function useIsOffline() {
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const queryClient = useQueryClient();

  useEffect(() => {
    function onOffline() {
      setIsOffline(true);
    }
    function onOnline() {
      setIsOffline(false);
    }

    window.addEventListener('offline', onOffline);
    window.addEventListener('online', onOnline);

    const unsubscribe = queryClient.getQueryCache().subscribe((event) => {
      if (event.type === 'updated' && event.action.type === 'error') {
        // If it's a network error or fetch failed, we can consider it offline
        const msg = event.action.error?.message?.toLowerCase() || '';
        if (msg.includes('fetch') || msg.includes('network') || msg.includes('failed')) {
          setIsOffline(true);
        }
      }
    });

    return () => {
      window.removeEventListener('offline', onOffline);
      window.removeEventListener('online', onOnline);
      unsubscribe();
    };
  }, [queryClient]);

  return isOffline;
}
