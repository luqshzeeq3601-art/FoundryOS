import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, downtimeApi } from '../../services/api-client';
import { 
  DowntimeEventDto, 
  DowntimeReasonCode, 
  DowntimeTriggerSource,
  PagedResponse, 
  MachineDto, 
  CreateDowntimeRequest, 
  ResolveDowntimeRequest,
  AcknowledgeRootCauseRequest,
  MicroStopSummary
} from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { Modal } from '../common/Modal';
import { 
  AlertTriangle, 
  CheckCircle, 
  Plus, 
  Clock, 
  Cpu, 
  Zap, 
  Activity, 
  Timer, 
  AlertOctagon, 
  BarChart2, 
  RefreshCw
} from 'lucide-react';

export const DowntimeView: React.FC = () => {
  const queryClient = useQueryClient();

  const [openOnly, setOpenOnly] = useState<boolean | undefined>(true);
  const [selectedMachineFilter, setSelectedMachineFilter] = useState<string>('');
  const [page, setPage] = useState(0);

  // Modals
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [resolvingEvent, setResolvingEvent] = useState<DowntimeEventDto | null>(null);
  const [acknowledgingEvent, setAcknowledgingEvent] = useState<DowntimeEventDto | null>(null);

  // Form states
  const [machineId, setMachineId] = useState('');
  const [reasonCode, setReasonCode] = useState<DowntimeReasonCode>('BREAKDOWN');
  const [ackReasonCode, setAckReasonCode] = useState<DowntimeReasonCode>('TOOLING_JAM');
  const [description, setDescription] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [ackNote, setAckNote] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
    refetchInterval: 4000,
  });

  // Fetch Pending Root-Cause Events (for Attention Banner & Operator Prompts)
  const { data: pendingRootCauses = [] } = useQuery<DowntimeEventDto[]>({
    queryKey: ['pending-root-causes'],
    queryFn: () => downtimeApi.getPendingRootCauses(),
    refetchInterval: 3000,
  });

  // Fetch Micro-Stop Summary for Selected Machine
  const { data: microStopSummary, isLoading: isSummaryLoading, refetch: refetchSummary } = useQuery<MicroStopSummary>({
    queryKey: ['micro-stop-summary', selectedMachineFilter],
    queryFn: () => {
      const targetMachine = selectedMachineFilter || (machinesData?.content?.[0]?.id ?? '');
      if (!targetMachine) return Promise.reject(new Error('No machine selected'));
      return downtimeApi.getMicroStopSummary(targetMachine);
    },
    enabled: Boolean(selectedMachineFilter || machinesData?.content?.[0]?.id),
    refetchInterval: 10000,
  });

  // Log Downtime Mutation
  const logMutation = useMutation({
    mutationFn: (data: CreateDowntimeRequest) =>
      api.post<DowntimeEventDto>('/downtime-events', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['downtime-events'] });
      queryClient.invalidateQueries({ queryKey: ['pending-root-causes'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['micro-stop-summary'] });
      setIsLogModalOpen(false);
      resetForm();
      setSuccessMessage('Downtime event recorded successfully.');
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
      queryClient.invalidateQueries({ queryKey: ['pending-root-causes'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['micro-stop-summary'] });
      setResolvingEvent(null);
      setResolutionNote('');
      setSuccessMessage('Downtime event resolved and machine returned to operational state.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to resolve downtime event.');
    },
  });

  // Acknowledge Root Cause Mutation
  const acknowledgeMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: AcknowledgeRootCauseRequest }) =>
      downtimeApi.acknowledgeRootCause(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['downtime-events'] });
      queryClient.invalidateQueries({ queryKey: ['pending-root-causes'] });
      queryClient.invalidateQueries({ queryKey: ['micro-stop-summary'] });
      setAcknowledgingEvent(null);
      setAckNote('');
      setSuccessMessage('Root cause classification logged successfully.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to acknowledge root cause.');
    },
  });

  // Manual Stream Evaluation Mutation (for testing & immediate sync)
  const evaluateMutation = useMutation({
    mutationFn: (machineId: string) => downtimeApi.evaluateStream(machineId),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['downtime-events'] });
      queryClient.invalidateQueries({ queryKey: ['pending-root-causes'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['micro-stop-summary'] });
      setSuccessMessage(`Evaluation completed: ${res.message}`);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Stream evaluation failed.');
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

  const handleAckSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!acknowledgingEvent) return;
    setErrorMessage(null);
    acknowledgeMutation.mutate({
      id: acknowledgingEvent.id,
      data: {
        reasonCode: ackReasonCode,
        resolutionNote: ackNote || undefined,
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
      case 'TOOLING_JAM':
        return <IndustrialBadge variant="warning">TOOLING JAM</IndustrialBadge>;
      case 'MICRO_STOP':
        return <IndustrialBadge variant="info">MICRO-STOP (&lt;180s)</IndustrialBadge>;
      case 'OPERATOR_PAUSE':
        return <IndustrialBadge variant="default">OPERATOR PAUSE</IndustrialBadge>;
      case 'UNPLANNED_MAINTENANCE':
        return <IndustrialBadge variant="danger">UNPLANNED MAINT</IndustrialBadge>;
      case 'OTHER':
      default:
        return <IndustrialBadge variant="default">OTHER</IndustrialBadge>;
    }
  };

  const getTriggerSourceBadge = (source: DowntimeTriggerSource) => {
    switch (source) {
      case 'AUTOMATED_SENSOR':
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-mono uppercase bg-cyan-950/70 text-cyan-400 border border-cyan-800/80">
            <Zap size={10} /> AUTO SENSOR
          </span>
        );
      case 'HEARTBEAT_TIMEOUT':
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-mono uppercase bg-amber-950/70 text-amber-400 border border-amber-800/80">
            <Timer size={10} /> HEARTBEAT TIMEOUT
          </span>
        );
      case 'MANUAL':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[10px] font-mono uppercase bg-industrial-800 text-industrial-300 border border-industrial-700">
            MANUAL OPERATOR
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500 flex items-center gap-2">
            <span>[ SPRINT 7 // E4-S3 ]</span>
            <span>•</span>
            <span>AUTOMATED MICRO-STOP & DOWNTIME ENGINE</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <AlertTriangle size={22} className="text-hazard-red" />
            <span>DOWNTIME & STOPPAGE LOG</span>
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {selectedMachineFilter && (
            <IndustrialButton
              variant="outline"
              size="md"
              isLoading={evaluateMutation.isPending}
              onClick={() => evaluateMutation.mutate(selectedMachineFilter)}
              title="Force stream sensor evaluation against current order"
            >
              <Activity size={15} className="mr-1 text-cyan-400" />
              <span>EVALUATE STREAM</span>
            </IndustrialButton>
          )}

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
      </div>

      {/* Operator Attention Banner for Pending Root Causes */}
      {pendingRootCauses.length > 0 && (
        <div className="bg-amber-950/90 border-2 border-amber-500 p-4 shadow-lg animate-pulse">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <AlertOctagon size={24} className="text-amber-400 shrink-0" />
              <div>
                <div className="text-xs font-mono font-bold uppercase text-amber-200 tracking-wider">
                  ⚠️ OPERATOR ROOT-CAUSE ACTION REQUIRED ({pendingRootCauses.length} PENDING)
                </div>
                <div className="text-xs font-mono text-amber-300 mt-0.5">
                  Machine stoppage has exceeded 180 seconds (3 minutes). Classify root cause code to satisfy OEE audit trail.
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <IndustrialButton
                variant="warning"
                size="sm"
                onClick={() => {
                  setAcknowledgingEvent(pendingRootCauses[0]);
                  setAckReasonCode(pendingRootCauses[0].reasonCode === 'MICRO_STOP' ? 'TOOLING_JAM' : pendingRootCauses[0].reasonCode);
                  setAckNote('');
                }}
              >
                CLASSIFY ROOT CAUSE ({pendingRootCauses[0].machineName})
              </IndustrialButton>
            </div>
          </div>
        </div>
      )}

      {/* Feedback Banners */}
      {errorMessage && (
        <div className="p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono flex items-center justify-between">
          <span>[ ERROR ]: {errorMessage}</span>
          <button onClick={() => setErrorMessage(null)} className="text-white hover:underline">
            DISMISS
          </button>
        </div>
      )}

      {successMessage && (
        <div className="p-3 bg-emerald-950/80 border border-emerald-500 text-emerald-400 text-xs font-mono flex items-center justify-between">
          <span>[ SUCCESS ]: {successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-white hover:underline">
            DISMISS
          </button>
        </div>
      )}

      {/* Micro-Stop & Outage Analytics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Micro-Stop Stoppages (< 180s) */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs font-mono uppercase text-cyan-400 mb-1">
            <span className="flex items-center gap-1.5">
              <Zap size={14} /> MICRO-STOPS (&lt;180s)
            </span>
            <span className="text-[10px] text-industrial-500">AUTO-RESOLVED</span>
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-1">
            {microStopSummary ? microStopSummary.microStopCount : '—'}
          </div>
          <div className="text-[11px] font-mono text-industrial-400 mt-1 flex items-center justify-between">
            <span>LOST DURATION:</span>
            <span className="text-cyan-300 font-bold">
              {microStopSummary ? `${microStopSummary.totalMicroStopDurationSeconds}s` : '—'}
            </span>
          </div>
          <div className="w-full bg-industrial-900 h-1 mt-2">
            <div 
              className="bg-cyan-500 h-1 transition-all" 
              style={{ width: `${Math.min(100, microStopSummary?.microStopPercentage || 0)}%` }} 
            />
          </div>
        </div>

        {/* Major Downtime Instances (>= 180s) */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs font-mono uppercase text-hazard-red mb-1">
            <span className="flex items-center gap-1.5">
              <AlertTriangle size={14} /> MAJOR OUTAGES (&ge;180s)
            </span>
            <span className="text-[10px] text-industrial-500">REQUIRES ACK</span>
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-1">
            {microStopSummary ? microStopSummary.majorDowntimeCount : '—'}
          </div>
          <div className="text-[11px] font-mono text-industrial-400 mt-1 flex items-center justify-between">
            <span>LOST DURATION:</span>
            <span className="text-hazard-red font-bold">
              {microStopSummary ? `${Math.round(microStopSummary.totalMajorDowntimeDurationSeconds / 60)} min` : '—'}
            </span>
          </div>
          <div className="w-full bg-industrial-900 h-1 mt-2">
            <div 
              className="bg-hazard-red h-1 transition-all" 
              style={{ width: `${Math.min(100, 100 - (microStopSummary?.microStopPercentage || 0))}%` }} 
            />
          </div>
        </div>

        {/* Micro-Stop Loss Ratio */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs font-mono uppercase text-industrial-400 mb-1">
            <span className="flex items-center gap-1.5">
              <BarChart2 size={14} /> MICRO-STOP FREQ RATIO
            </span>
            <span className="text-[10px] text-industrial-500">OEE IMPACT</span>
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-1">
            {microStopSummary ? `${microStopSummary.microStopPercentage.toFixed(1)}%` : '—'}
          </div>
          <div className="text-[11px] font-mono text-industrial-400 mt-1">
            Proportion of events resolved &lt; 3 minutes
          </div>
          <div className="w-full bg-industrial-900 h-1 mt-2">
            <div 
              className="bg-amber-400 h-1 transition-all" 
              style={{ width: `${Math.min(100, microStopSummary?.microStopPercentage || 0)}%` }} 
            />
          </div>
        </div>

        {/* Total Sensor Detection Status */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs font-mono uppercase text-terminal-green mb-1">
            <span className="flex items-center gap-1.5">
              <Activity size={14} /> DETECTION LATENCY
            </span>
            <span className="text-[10px] text-terminal-green font-bold">&le; 5.0 SEC</span>
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-1">
            ACTIVE
          </div>
          <div className="text-[11px] font-mono text-industrial-400 mt-1">
            Heartbeat & cycle threshold monitoring
          </div>
          <div className="w-full bg-industrial-900 h-1 mt-2">
            <div className="bg-terminal-green h-1 w-full" />
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">VIEW:</span>
          <div className="flex flex-wrap gap-1">
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

        <div className="w-full sm:w-72 flex items-center gap-2">
          <select
            value={selectedMachineFilter}
            onChange={(e) => {
              setSelectedMachineFilter(e.target.value);
              setPage(0);
            }}
            className="w-full bg-industrial-900 border border-substrate-border px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
          >
            <option value="">ALL MACHINES</option>
            {machines.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name} ({m.serialNumber})
              </option>
            ))}
          </select>

          <IndustrialButton
            size="sm"
            variant="outline"
            onClick={() => refetchSummary()}
            title="Refresh summary statistics"
          >
            <RefreshCw size={14} className={isSummaryLoading ? 'animate-spin' : ''} />
          </IndustrialButton>
        </div>
      </div>

      {/* Downtime Events List / Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">MACHINE</th>
                <th className="p-3">TRIGGER SOURCE</th>
                <th className="p-3">REASON CODE</th>
                <th className="p-3">START TIME</th>
                <th className="p-3">DURATION / STATUS</th>
                <th className="p-3">ROOT CAUSE & NOTES</th>
                <th className="p-3 text-right">ACTION</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="p-6 text-center text-industrial-500">
                    SCANNING TELEMETRY & DOWNTIME LOGS...
                  </td>
                </tr>
              ) : events.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-6 text-center text-industrial-500">
                    NO DOWNTIME EVENTS FOUND FOR CURRENT FILTER.
                  </td>
                </tr>
              ) : (
                events.map((ev) => {
                  const isOpen = !ev.endTime;
                  const startTime = new Date(ev.startTime);
                  const endTime = ev.endTime ? new Date(ev.endTime) : null;
                  const durationSeconds = endTime
                    ? Math.round((endTime.getTime() - startTime.getTime()) / 1000)
                    : Math.round((Date.now() - startTime.getTime()) / 1000);
                  const durationMinutes = Math.round(durationSeconds / 60);

                  const isPromptPending = Boolean(ev.rootCausePromptedAt && !ev.rootCauseAcknowledgedAt);

                  return (
                    <tr
                      key={ev.id}
                      className={`transition-colors ${
                        isPromptPending
                          ? 'bg-amber-950/30 hover:bg-amber-950/40 border-l-2 border-l-amber-500'
                          : isOpen 
                            ? 'bg-red-950/20 hover:bg-red-950/30' 
                            : 'hover:bg-industrial-900/60'
                      }`}
                    >
                      <td className="p-3">
                        <div className="font-bold text-white flex items-center gap-1.5">
                          <Cpu size={14} className="text-industrial-400" />
                          <span>{ev.machineName}</span>
                        </div>
                        <div className="text-[10px] text-industrial-500 mt-0.5">
                          ID: {ev.id.substring(0, 8)}...
                        </div>
                      </td>

                      <td className="p-3">
                        {getTriggerSourceBadge(ev.triggerSource || 'MANUAL')}
                      </td>

                      <td className="p-3">
                        <div className="space-y-1">
                          {getReasonBadge(ev.reasonCode)}
                          {ev.isMicroStop && (
                            <div className="text-[10px] text-cyan-400 font-bold flex items-center gap-1">
                              <Zap size={10} /> &lt; 180s AUTO
                            </div>
                          )}
                        </div>
                      </td>

                      <td className="p-3 text-industrial-300">
                        {ev.startTime.replace('T', ' ').substring(0, 19)}
                      </td>

                      <td className="p-3">
                        {isOpen ? (
                          <div className="flex items-center gap-1.5 text-hazard-red font-bold animate-pulse">
                            <Clock size={12} />
                            <span>OPEN ({durationSeconds}s / {durationMinutes}m)</span>
                          </div>
                        ) : (
                          <div>
                            <div className="text-terminal-green flex items-center gap-1 font-bold">
                              <CheckCircle size={12} />
                              <span>{durationSeconds < 180 ? `${durationSeconds}s` : `${durationMinutes}m`}</span>
                            </div>
                            <div className="text-[10px] text-industrial-500">
                              BY: {ev.resolverName || (ev.isMicroStop ? 'SYSTEM (AUTO)' : 'SYSTEM')}
                            </div>
                          </div>
                        )}
                      </td>

                      <td className="p-3 text-industrial-300 max-w-xs">
                        {isPromptPending ? (
                          <div className="text-amber-400 font-bold flex items-center gap-1">
                            <AlertOctagon size={12} />
                            <span>ATTENTION: CLASSIFICATION REQUIRED (&ge;3 MINS)</span>
                          </div>
                        ) : ev.rootCauseAcknowledgedAt ? (
                          <div className="text-terminal-green text-[11px]">
                            ✓ Root cause acknowledged
                          </div>
                        ) : null}

                        {ev.description && <div className="text-white mt-0.5">{ev.description}</div>}
                        {ev.resolutionNote && (
                          <div className="text-[11px] text-industrial-400 mt-0.5 italic">
                            RES: {ev.resolutionNote}
                          </div>
                        )}
                      </td>

                      <td className="p-3 text-right">
                        <div className="flex justify-end gap-1.5">
                          {isPromptPending && (
                            <IndustrialButton
                              variant="warning"
                              size="sm"
                              onClick={() => {
                                setAcknowledgingEvent(ev);
                                setAckReasonCode(ev.reasonCode === 'MICRO_STOP' ? 'TOOLING_JAM' : ev.reasonCode);
                                setAckNote('');
                              }}
                            >
                              CLASSIFY
                            </IndustrialButton>
                          )}

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
                        </div>
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

      {/* Operator Root-Cause Acknowledgment Modal */}
      <Modal
        isOpen={!!acknowledgingEvent}
        onClose={() => setAcknowledgingEvent(null)}
        title="OPERATOR ROOT-CAUSE CLASSIFICATION"
        subtitle={acknowledgingEvent ? `Asset: ${acknowledgingEvent.machineName} — Outage Duration >= 3 Minutes` : ''}
        hazard={false}
      >
        <form onSubmit={handleAckSubmit} className="space-y-4">
          <div className="p-3 bg-amber-950/60 border border-amber-500/70 text-amber-200 text-xs font-mono">
            [ OEE ACCURACY GATE ]: The asset stopped during active production and crossed the micro-stop threshold. Please select the verified physical cause.
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Verified Root Cause Classification *
            </label>
            <select
              required
              value={ackReasonCode}
              onChange={(e) => setAckReasonCode(e.target.value as DowntimeReasonCode)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-amber-400"
            >
              <option value="TOOLING_JAM">TOOLING JAM (Feeder / Chuck / Cutter Obstruction)</option>
              <option value="MATERIAL_SHORTAGE">MATERIAL SHORTAGE (Blank / Stock Starvation)</option>
              <option value="BREAKDOWN">BREAKDOWN (Mechanical / Electrical Component Failure)</option>
              <option value="OPERATOR_PAUSE">OPERATOR PAUSE (Inspection / Adjustment / Hygiene)</option>
              <option value="UNPLANNED_MAINTENANCE">UNPLANNED MAINTENANCE (Emergency Lube / Tighten)</option>
              <option value="SETUP">SETUP (Changeover / Recalibration)</option>
              <option value="OTHER">OTHER (Facility / Air / Power Fluctuation)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Root-Cause Observations & Corrective Notes
            </label>
            <textarea
              rows={3}
              value={ackNote}
              onChange={(e) => setAckNote(e.target.value)}
              placeholder="e.g. Swarf wrapped around spindle collar; cleared manually..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-amber-400 resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setAcknowledgingEvent(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="warning"
              size="lg"
              isLoading={acknowledgeMutation.isPending}
            >
              CONFIRM ROOT CAUSE
            </IndustrialButton>
          </div>
        </form>
      </Modal>

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
              <option value="TOOLING_JAM">TOOLING JAM (Obstruction / Jam)</option>
              <option value="OPERATOR_PAUSE">OPERATOR PAUSE (Inspection / Adjustment)</option>
              <option value="UNPLANNED_MAINTENANCE">UNPLANNED MAINTENANCE</option>
              <option value="MICRO_STOP">MICRO_STOP (&lt; 180s Stoppage)</option>
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
