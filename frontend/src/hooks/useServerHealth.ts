import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

export type ServerHealth = 'up' | 'degraded' | 'down' | 'checking';

/** Polls the backend's public health endpoint so status indicators reflect the real server state. */
export function useServerHealth(): ServerHealth {
  const { data, isError, isPending } = useQuery({
    queryKey: ['server-health'],
    queryFn: async () => {
      const res = await axios.get<{ status: string }>('/actuator/health', {
        timeout: 5000,
        validateStatus: () => true,
      });
      return res.data?.status ?? 'DOWN';
    },
    refetchInterval: 15000,
    refetchIntervalInBackground: false,
    retry: false,
  });

  if (isPending) return 'checking';
  if (isError) return 'down';
  if (data === 'UP') return 'up';
  return data === 'DOWN' ? 'down' : 'degraded';
}
