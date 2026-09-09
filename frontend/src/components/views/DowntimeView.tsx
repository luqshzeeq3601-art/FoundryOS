import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { DowntimeEventDto, DowntimeReasonCode, PagedResponse, MachineDto, CreateDowntimeRequest, ResolveDowntimeRequest } from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { Modal } from '../common/Modal';
import { AlertTriangle, CheckCircle, Plus, Clock, Cpu } from 'lucide-react';

export const DowntimeView: React.FC = () => {
  const queryClient = useQueryClient();

  const [openOnly, setOpenOnly] = useState<boolean | undefined>(true);
  const [selectedMachineFilter, setSelectedMachineFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  // Modals
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [resolvingEvent, setResolvingEvent] = useState<DowntimeEventDto | null>(null);

  // Form states
  const [machineId, setMachineId] = useState('');
  const [reasonCode, setReasonCode] = useState<DowntimeReasonCode>('BREAKDOWN');
  const [description, setDescription] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch Machines for dropdown filter/forms
  const { data: machinesData } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-list-all'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 100 }),
  });

  // Fetch Downtime Events
  const { data: downtimeData, isLoading } = useQuery<PagedResponse<DowntimeEventDto>>({
    queryKey: ['downtime-events', openOnly, selectedMachineFilter, page],
    queryFn: () =>
      api.get<PagedResponse<DowntimeEventDto>>('/downtime-events', {
        openOnly: openOnly !== undefined ? openOnly : undefined,
        machineId: selectedMachineFilter || undefined,
        page,
        size: 15,
      }),
    refetchInterval: 5000,
  });

  // Log Downtime Mutation
  const logMutation = useMutation({
    mutationFn: (data: CreateDowntimeRequest) =>
      api.post<DowntimeEventDto>('/downtime-events', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['downtime-events'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsLogModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to log downtime event.');
    },
  });

  // Resolve Downtime Mutation
  const resolveMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResolveDowntimeRequest }) =>
      api.post<DowntimeEventDto>(`/downtime-events/${id}/resolve`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['downtime-events'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setResolvingEvent(null);
      setResolutionNote('');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to resolve downtime event.');
    },
  });

  const resetForm = () => {
    setMachineId('');
    setReasonCode('BREAKDOWN');
    setDescription('');
    setErrorMessage(null);
  };

  const handleLogSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    logMutation.mutate({
      machineId,
      reasonCode,
      description,
    });
  };

  const handleResolveSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolvingEvent) return;
    setErrorMessage(null);
    resolveMutation.mutate({
      id: resolvingEvent.id,
      data: {
        resolutionNote,
        expectedVersion: resolvingEvent.version,
      },
    });
  };

  const machines = machinesData?.content || [];
  const events = downtimeData?.content || [];

  const getReasonBadge = (code: DowntimeReasonCode) => {
    switch (code) {
      case 'BREAKDOWN':
        return <IndustrialBadge variant="danger">BREAKDOWN</IndustrialBadge>;
      case 'MATERIAL_SHORTAGE':
        return <IndustrialBadge variant="warning">MATERIAL SHORTAGE</IndustrialBadge>;
      case 'SETUP':
        return <IndustrialBadge variant="info">SETUP / CHANGEOVER</IndustrialBadge>;
      case 'OTHER':
      default:
        return <IndustrialBadge variant="default">OTHER</IndustrialBadge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ OUTAGE & FAULT MANAGEMENT // TELEMETRY ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <AlertTriangle size={22} className="text-hazard-red" />
            <span>DOWNTIME & STOPPAGE LOG</span>
          </h1>
        </div>

        <IndustrialButton
          variant="hazard"
          size="md"
          onClick={() => {
            resetForm();
            setIsLogModalOpen(true);
          }}
        >
          <Plus size={16} className="mr-1" />
          <span>LOG DOWNTIME EVENT</span>
        </IndustrialButton>
      </div>

      {/* Global Error Banner */}
      {errorMessage && (
        <div className="p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono flex items-center justify-between">
          <span>[ ERROR ]: {errorMessage}</span>
          <button onClick={() => setErrorMessage(null)} className="text-white hover:underline">
            DISMISS
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">VIEW:</span>
          <div className="flex gap-1">
            <button
              onClick={() => setOpenOnly(true)}
              className={`px-3 py-1 text-xs font-mono uppercase border transition-colors ${
                openOnly === true
                  ? 'bg-red-950 text-hazard-red border-hazard-red font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              OPEN STOPPAGES ONLY
            </button>
            <button
              onClick={() => setOpenOnly(false)}
              className={`px-3 py-1 text-xs font-mono uppercase border transition-colors ${
                openOnly === false
                  ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              RESOLVED LOG
            </button>
            <button
              onClick={() => setOpenOnly(undefined)}
              className={`px-3 py-1 text-xs font-mono uppercase border transition-colors ${
                openOnly === undefined
                  ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              ALL EVENTS
            </button>
          </div>
        </div>

        <div className="w-full sm:w-64">
          <select
            value={selectedMachineFilter}
            onChange={(e) => setSelectedMachineFilter(e.target.value)}
            className="w-full bg-industrial-900 border border-substrate-border px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
          >
            <option value="">ALL MACHINES</option>
            {machines.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name} ({m.serialNumber})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Downtime Events List / Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">MACHINE</th>
                <th className="p-3">REASON CODE</th>
                <th className="p-3">START TIME</th>
                <th className="p-3">DURATION / RESOLUTION</th>
                <th className="p-3">NOTES & DETAILS</th>
                <th className="p-3 text-right">ACTION</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    SCANNING TELEMETRY LOGS...
                  </td>
                </tr>
              ) : events.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO DOWNTIME EVENTS FOUND FOR CURRENT FILTER.
                  </td>
                </tr>
              ) : (
                events.map((ev) => {
                  const isOpen = !ev.endTime;
                  const startTime = new Date(ev.startTime);
                  const endTime = ev.endTime ? new Date(ev.endTime) : null;
                  const durationMinutes = endTime
                    ? Math.round((endTime.getTime() - startTime.getTime()) / (1000 * 60))
                    : Math.round((Date.now() - startTime.getTime()) / (1000 * 60));

                  return (
                    <tr
                      key={ev.id}
                      className={`transition-colors ${
                        isOpen ? 'bg-red-950/20 hover:bg-red-950/30' : 'hover:bg-industrial-900/60'
                      }`}
                    >
                      <td className="p-3">
                        <div className="font-bold text-white flex items-center gap-1.5">
                          <Cpu size={14} className="text-industrial-400" />
                          <span>{ev.machineName}</span>
                        </div>
                        <div className="text-[10px] text-industrial-500 mt-0.5">
                          EVENT ID: {ev.id.substring(0, 8)}...
                        </div>
                      </td>
                      <td className="p-3">{getReasonBadge(ev.reasonCode)}</td>
                      <td className="p-3 text-industrial-300">
                        {ev.startTime.replace('T', ' ').substring(0, 19)}
                      </td>
                      <td className="p-3">
                        {isOpen ? (
                          <div className="flex items-center gap-1.5 text-hazard-red font-bold animate-pulse">
                            <Clock size={12} />
                            <span>OPEN ({durationMinutes} MINS)</span>
                          </div>
                        ) : (
                          <div>
                            <div className="text-terminal-green flex items-center gap-1">
                              <CheckCircle size={12} />
                              <span>RESOLVED ({durationMinutes} MINS)</span>
                            </div>
                            <div className="text-[10px] text-industrial-500">
                              BY: {ev.resolverName || 'SYSTEM'}
                            </div>
                          </div>
                        )}
                      </td>
                      <td className="p-3 text-industrial-300 max-w-xs">
                        {ev.description && <div>{ev.description}</div>}
                        {ev.resolutionNote && (
                          <div className="text-[11px] text-industrial-400 mt-0.5 italic">
                            RES: {ev.resolutionNote}
                          </div>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        {isOpen && (
                          <IndustrialButton
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              setResolvingEvent(ev);
                              setResolutionNote('');
                              setErrorMessage(null);
                            }}
                          >
                            RESOLVE
                          </IndustrialButton>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {downtimeData && downtimeData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {downtimeData.page + 1} OF {downtimeData.totalPages} ({downtimeData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={downtimeData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={downtimeData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>

      {/* Log Downtime Modal */}
      <Modal
        isOpen={isLogModalOpen}
        onClose={() => setIsLogModalOpen(false)}
        title="RECORD DOWNTIME EVENT"
        subtitle="Halt asset and initialize root cause duration counter"
        hazard={true}
      >
        <form onSubmit={handleLogSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Target Machine *
            </label>
            <select
              required
              value={machineId}
              onChange={(e) => setMachineId(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-hazard-red"
            >
              <option value="">-- SELECT MACHINE --</option>
              {machines.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.serialNumber}) — Current: {m.status}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Reason Classification Code *
            </label>
            <select
              required
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value as DowntimeReasonCode)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-hazard-red"
            >
              <option value="BREAKDOWN">BREAKDOWN (Mechanical/Electrical Failure)</option>
              <option value="SETUP">SETUP (Changeover / Calibration)</option>
              <option value="MATERIAL_SHORTAGE">MATERIAL SHORTAGE (Supply Blockage)</option>
              <option value="OTHER">OTHER (Environmental / Safety)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Observations / Root Cause Notes
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detail error alarms, symptoms, sensor readings..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-red resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setIsLogModalOpen(false)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="hazard"
              size="lg"
              isLoading={logMutation.isPending}
            >
              LOG DOWNTIME
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Resolve Downtime Modal */}
      <Modal
        isOpen={!!resolvingEvent}
        onClose={() => setResolvingEvent(null)}
        title="RESOLVE DOWNTIME EVENT"
        subtitle={resolvingEvent ? `Machine: ${resolvingEvent.machineName} (${resolvingEvent.reasonCode})` : ''}
      >
        <form onSubmit={handleResolveSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Corrective Action & Resolution Notes *
            </label>
            <textarea
              rows={4}
              required
              value={resolutionNote}
              onChange={(e) => setResolutionNote(e.target.value)}
              placeholder="Describe repair performed, replaced components, recalibration results..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-terminal-green resize-none"
            />
          </div>

          <div className="text-xs font-mono text-industrial-400 bg-industrial-900 p-3 border border-substrate-border">
            [ SYSTEM ]: Resolving this event will set the machine status back to IDLE and timestamp the total outage duration.
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setResolvingEvent(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              size="lg"
              isLoading={resolveMutation.isPending}
            >
              CONFIRM RESOLUTION & RESTORE
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
