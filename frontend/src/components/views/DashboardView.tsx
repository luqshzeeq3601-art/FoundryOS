import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { DashboardSummaryDto, MachineDto, PagedResponse, DowntimeReasonCode } from '../../types';
import { MetricTile } from '../common/MetricTile';
import { StatusBeacon } from '../common/StatusBeacon';
import { IndustrialButton } from '../common/IndustrialButton';
import { Modal } from '../common/Modal';
import { Banner } from '../common/Banner';
import { ErrorState } from '../common/ErrorState';
import { getErrorMessage } from '../../utils/errors';
import { AlertOctagon, Cpu, RefreshCw, Flame } from 'lucide-react';

// World-class OEE benchmarks. Color follows the value against target, and the text says so too.
const OEE_FACTORS = [
  { key: 'plantOee', label: 'Plant OEE', target: 85 },
  { key: 'plantAvailability', label: 'Availability', target: 90 },
  { key: 'plantPerformance', label: 'Performance', target: 95 },
  { key: 'plantQuality', label: 'Quality', target: 99 },
] as const satisfies ReadonlyArray<{ key: keyof DashboardSummaryDto; label: string; target: number }>;

type Tone = 'good' | 'warn' | 'bad';

const oeeTone = (value: number, target: number): Tone =>
  value >= target ? 'good' : value >= target - 15 ? 'warn' : 'bad';

const TONE_CLASS: Record<Tone, string> = {
  good: 'text-terminal-green',
  warn: 'text-hazard-amber',
  bad: 'text-hazard-red',
};

export const DashboardView: React.FC = () => {
  const [isBreakdownModalOpen, setIsBreakdownModalOpen] = useState(false);
  const [selectedMachineId, setSelectedMachineId] = useState('');
  const [reasonCode, setReasonCode] = useState<DowntimeReasonCode>('BREAKDOWN');
  const [description, setDescription] = useState('');
  const [createWorkOrder, setCreateWorkOrder] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch Dashboard Summary
  const {
    data: summary,
    refetch: refetchSummary,
    isFetching: isSummaryFetching,
    isError: isSummaryError,
    error: summaryError,
  } = useQuery<DashboardSummaryDto>({
    queryKey: ['dashboard-summary'],
    queryFn: () => api.get<DashboardSummaryDto>('/dashboard/summary'),
    refetchInterval: 5000, // Real-time 5s polling
  });

  // Fetch Machines
  const {
    data: machinesData,
    refetch: refetchMachines,
    isLoading: isMachinesLoading,
    isError: isMachinesError,
    error: machinesError,
    isFetching: isMachinesFetching,
  } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-overview'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 50 }),
    refetchInterval: 5000,
  });

  const handleReportBreakdown = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMachineId) return;
    setIsSubmitting(true);
    setActionMessage(null);
    setFormError(null);
    try {
      await api.post('/operations/breakdown', {
        machineId: selectedMachineId,
        reasonCode,
        description,
        createWorkOrder,
      });
      setIsBreakdownModalOpen(false);
      setDescription('');
      setSelectedMachineId('');
      refetchSummary();
      refetchMachines();
      setActionMessage({ tone: 'success', text: 'Breakdown reported. Maintenance has been notified.' });
    } catch (err) {
      // Keep the dialog open so the operator can correct and resubmit.
      setFormError(getErrorMessage(err, 'Breakdown could not be reported. Try again.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const machines = machinesData?.content || [];

  return (
    <div className="space-y-6">
      {/* Top Banner Alert if any machines are DOWN */}
      {summary && summary.downMachines > 0 && (
        <div role="alert" className="bg-red-950/90 border-2 border-hazard-red p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <AlertOctagon className="text-hazard-red shrink-0 animate-beacon" size={24} aria-hidden="true" />
            <div>
              <div className="text-xs font-mono font-bold uppercase text-hazard-red">
                {summary.downMachines} {summary.downMachines === 1 ? 'MACHINE IS' : 'MACHINES ARE'} DOWN
              </div>
              <div className="text-xs font-mono text-industrial-300">
                Dispatch a technician and log the fault so downtime is tracked.
              </div>
            </div>
          </div>
          <IndustrialButton
            variant="hazard"
            size="sm"
            onClick={() => { setFormError(null); setIsBreakdownModalOpen(true); }}
            className="shrink-0"
          >
            REPORT FAULT
          </IndustrialButton>
        </div>
      )}

      {/* Action Notification */}
      {actionMessage && (
        <Banner tone={actionMessage.tone} onDismiss={() => setActionMessage(null)}>
          {actionMessage.text}
        </Banner>
      )}

      {isSummaryError && (
        <ErrorState
          title="Plant summary could not be loaded"
          error={summaryError}
          onRetry={() => refetchSummary()}
          isRetrying={isSummaryFetching}
        />
      )}

      {/* OEE & Primary Telemetry Strip */}
      <div className="bg-substrate-card border border-substrate-border p-5">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-4 border-b border-substrate-border">
          <div>
            <div className="text-xs font-mono uppercase tracking-widest text-industrial-400">
              Overall equipment effectiveness
            </div>
            <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
              <span>PLANT OVERVIEW</span>
              {!isSummaryError && summary && (
                <span className="text-xs font-normal text-terminal-green border border-terminal-green/40 px-1.5 py-0.5 bg-terminal-green/10">
                  LIVE · 5s
                </span>
              )}
            </h1>
          </div>

          <div className="flex items-center gap-2">
            <IndustrialButton
              variant="outline"
              size="sm"
              onClick={() => {
                refetchSummary();
                refetchMachines();
              }}
              aria-label="Refresh dashboard"
              className="min-w-[44px] min-h-[44px]"
            >
              <RefreshCw size={14} className={isSummaryFetching || isMachinesFetching ? 'animate-spin' : ''} aria-hidden="true" />
            </IndustrialButton>

            <IndustrialButton
              variant="hazard"
              size="md"
              onClick={() => { setFormError(null); setIsBreakdownModalOpen(true); }}
            >
              <Flame size={16} className="mr-1.5" aria-hidden="true" />
              <span>REPORT BREAKDOWN</span>
            </IndustrialButton>
          </div>
        </div>

        {/* OEE Score Breakdown Grid */}
        <dl className="grid grid-cols-2 md:grid-cols-4 gap-3 pt-4" aria-busy={!summary && !isSummaryError}>
          {OEE_FACTORS.map((factor) => {
            const value = summary ? summary[factor.key] : undefined;
            const tone = value === undefined ? null : oeeTone(value, factor.target);
            return (
              <div key={factor.key} className="p-3 bg-industrial-900 border border-substrate-border">
                <dt className="text-xs font-mono uppercase text-industrial-400">{factor.label}</dt>
                <dd className={`text-2xl sm:text-3xl font-bold font-mono mt-1 tabular-nums ${tone ? TONE_CLASS[tone] : 'text-white'}`}>
                  {value === undefined ? '--' : `${value}%`}
                </dd>
                <dd className="text-xs font-mono text-industrial-400 mt-1">
                  Target ≥{factor.target}%{tone && tone !== 'good' ? ' · below target' : ''}
                </dd>
              </div>
            );
          })}
        </dl>
      </div>

      {/* KPI Metric Tiles Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricTile
          label="MACHINES ONLINE"
          tag="FLEET"
          value={summary ? `${summary.runningMachines} / ${summary.totalMachines}` : '--'}
          subtext={summary ? `${summary.idleMachines} IDLE // ${summary.downMachines} DOWN` : undefined}
          status={summary && summary.downMachines > 0 ? 'danger' : 'normal'}
        />

        <MetricTile
          label="ACTIVE PRODUCTION"
          tag="ORDERS"
          value={summary ? summary.activeProductionOrders : '--'}
          subtext={summary ? `${summary.completedProductionOrders} COMPLETED TODAY` : undefined}
          status={summary && summary.activeProductionOrders > 0 ? 'success' : 'normal'}
        />

        <MetricTile
          label="OPEN DOWNTIMES"
          tag="CRITICAL"
          value={summary ? summary.openDowntimeEvents : '--'}
          subtext={summary ? `${summary.criticalWorkOrders} CRITICAL WORK ORDERS` : undefined}
          status={summary && summary.openDowntimeEvents > 0 ? 'danger' : 'normal'}
        />

        <MetricTile
          label="SCRAP RATE"
          tag="DEFECTS"
          value={summary ? `${summary.scrapRate}%` : '--'}
          unit={summary ? `(${summary.totalScrapQuantity} UNITS)` : undefined}
          subtext={summary ? `${summary.totalGoodQuantity} GOOD UNITS PRODUCED` : undefined}
          status={summary && summary.scrapRate > 5.0 ? 'warning' : 'normal'}
        />
      </div>

      {/* Machine Fleet Live Status Map */}
      <div className="bg-substrate-card border border-substrate-border p-5">
        <div className="flex items-center justify-between pb-4 border-b border-substrate-border mb-4">
          <div className="flex items-center gap-2">
            <Cpu size={16} className="text-industrial-400" />
            <h2 className="text-sm font-bold font-mono uppercase text-white tracking-wider">
              MACHINE FLEET // LIVE TELEMETRY
            </h2>
          </div>
          <span className="text-xs font-mono text-industrial-400">
            TOTAL ASSETS: {machines.length}
          </span>
        </div>

        {isMachinesError ? (
          <ErrorState
            title="Machine status could not be loaded"
            error={machinesError}
            onRetry={() => refetchMachines()}
            isRetrying={isMachinesFetching}
            compact
          />
        ) : isMachinesLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3" aria-busy="true" aria-label="Loading machines">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-32 bg-industrial-900 border border-substrate-border animate-pulse" />
            ))}
          </div>
        ) : machines.length === 0 ? (
          <div className="text-center py-8 text-sm text-industrial-300">
            <p>No machines registered yet.</p>
            <a href="#/machines" className="inline-block mt-2 text-terminal-cyan underline underline-offset-2 hover:text-white">
              Register a machine
            </a>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
            {machines.map((m) => (
              <div
                key={m.id}
                className={`p-4 border bg-industrial-900 flex flex-col justify-between ${
                  m.status === 'DOWN'
                    ? 'border-hazard-red shadow-[0_0_10px_rgba(255,42,42,0.15)]'
                    : m.status === 'RUNNING'
                    ? 'border-terminal-green/60'
                    : 'border-substrate-border'
                }`}
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-xs font-mono text-industrial-500 uppercase">
                      {m.serialNumber}
                    </span>
                    <StatusBeacon status={m.status} size="sm" />
                  </div>
                  <h3 className="font-bold font-mono text-sm text-white mt-1 truncate">
                    {m.name}
                  </h3>
                  <div className="text-xs font-mono text-industrial-400 mt-0.5">
                    LOC: {m.location}
                  </div>
                </div>

                <div className="mt-4 pt-3 border-t border-substrate-border/80 flex items-center justify-between">
                  <span className="text-xs font-mono text-industrial-500">
                    VER: v{m.version}
                  </span>
                  {m.status === 'DOWN' ? (
                    <span className="text-xs font-mono text-hazard-red font-bold">
                      NEEDS ACTION
                    </span>
                  ) : (
                    <span className="text-xs font-mono text-industrial-400">
                      STATUS: {m.status}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Emergency Breakdown Modal */}
      <Modal
        isOpen={isBreakdownModalOpen}
        onClose={() => setIsBreakdownModalOpen(false)}
        title="Report a breakdown"
        subtitle="Marks the machine as down and alerts maintenance."
        hazard={true}
      >
        <form onSubmit={handleReportBreakdown} className="space-y-4">
          {formError && (
            <div role="alert" className="p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono">
              {formError}
            </div>
          )}
          <div>
            <label htmlFor="breakdown-machine" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Machine *
            </label>
            <select
              id="breakdown-machine"
              required
              value={selectedMachineId}
              onChange={(e) => setSelectedMachineId(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-red"
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
            <label htmlFor="breakdown-reason" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Reason *
            </label>
            <select
              id="breakdown-reason"
              required
              value={reasonCode}
              onChange={(e) => setReasonCode(e.target.value as DowntimeReasonCode)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-red"
            >
              <option value="BREAKDOWN">BREAKDOWN (Mechanical/Electrical Failure)</option>
              <option value="SETUP">SETUP (Changeover / Calibration)</option>
              <option value="MATERIAL_SHORTAGE">MATERIAL SHORTAGE (Supply Blockage)</option>
              <option value="OTHER">OTHER (Environmental / Safety)</option>
            </select>
          </div>

          <div>
            <label htmlFor="breakdown-description" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              What happened
            </label>
            <textarea
              id="breakdown-description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detail error codes, abnormal noise, component failure..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-red resize-none"
            />
          </div>

          <div className="flex items-center gap-2 p-3 bg-industrial-900 border border-substrate-border">
            <input
              type="checkbox"
              id="createWoCheck"
              checked={createWorkOrder}
              onChange={(e) => setCreateWorkOrder(e.target.checked)}
              className="w-4 h-4 accent-hazard-red"
            />
            <label htmlFor="createWoCheck" className="text-xs font-mono text-industrial-200 cursor-pointer">
              Create a high-priority maintenance work order
            </label>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setIsBreakdownModalOpen(false)}
            >
              Cancel
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="hazard"
              size="lg"
              isLoading={isSubmitting}
            >
              Report breakdown
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
