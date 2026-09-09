import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { DashboardSummaryDto, MachineDto, PagedResponse, DowntimeReasonCode } from '../../types';
import { MetricTile } from '../common/MetricTile';
import { StatusBeacon } from '../common/StatusBeacon';
import { IndustrialButton } from '../common/IndustrialButton';
import { Modal } from '../common/Modal';
import { AlertOctagon, Cpu, RefreshCw, Flame } from 'lucide-react';

export const DashboardView: React.FC = () => {
  const [isBreakdownModalOpen, setIsBreakdownModalOpen] = useState(false);
  const [selectedMachineId, setSelectedMachineId] = useState('');
  const [reasonCode, setReasonCode] = useState<DowntimeReasonCode>('BREAKDOWN');
  const [description, setDescription] = useState('');
  const [createWorkOrder, setCreateWorkOrder] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Fetch Dashboard Summary
  const { data: summary, refetch: refetchSummary, isLoading: isSummaryLoading } = useQuery<DashboardSummaryDto>({
    queryKey: ['dashboard-summary'],
    queryFn: () => api.get<DashboardSummaryDto>('/dashboard/summary'),
    refetchInterval: 5000, // Real-time 5s polling
  });

  // Fetch Machines
  const { data: machinesData, refetch: refetchMachines } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-overview'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 50 }),
    refetchInterval: 5000,
  });

  const handleReportBreakdown = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMachineId) return;
    setIsSubmitting(true);
    setActionMessage(null);
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
      setActionMessage('Emergency breakdown logged and maintenance notified.');
    } catch (err: any) {
      setActionMessage(err.response?.data?.error?.message || 'Failed to report breakdown.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const machines = machinesData?.content || [];

  return (
    <div className="space-y-6">
      {/* Top Banner Alert if any machines are DOWN */}
      {summary && summary.downMachines > 0 && (
        <div className="bg-red-950/90 border-2 border-hazard-red p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-pulse">
          <div className="flex items-center gap-3">
            <AlertOctagon className="text-hazard-red shrink-0" size={24} />
            <div>
              <div className="text-xs font-mono font-bold uppercase text-hazard-red">
                [ CRITICAL TELEMETRY ALERT ] — {summary.downMachines} MACHINE(S) IN DOWN STATE
              </div>
              <div className="text-xs font-mono text-industrial-300">
                Active downtime events require immediate technician dispatch and fault resolution.
              </div>
            </div>
          </div>
          <IndustrialButton
            variant="hazard"
            size="sm"
            onClick={() => setIsBreakdownModalOpen(true)}
            className="shrink-0"
          >
            REPORT FAULT
          </IndustrialButton>
        </div>
      )}

      {/* Action Notification */}
      {actionMessage && (
        <div className="p-3 bg-industrial-850 border border-industrial-600 text-xs font-mono text-terminal-green flex items-center justify-between">
          <span>[ STATUS ]: {actionMessage}</span>
          <button onClick={() => setActionMessage(null)} className="text-industrial-400 hover:text-white">
            DISMISS
          </button>
        </div>
      )}

      {/* OEE & Primary Telemetry Strip */}
      <div className="bg-substrate-card border border-substrate-border p-5">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-4 border-b border-substrate-border">
          <div>
            <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
              [ OVERALL EQUIPMENT EFFECTIVENESS // OEE ]
            </div>
            <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
              <span>PLANT TELEMETRY OVERVIEW</span>
              <span className="text-xs font-normal text-terminal-green border border-terminal-green/40 px-1.5 py-0.5 bg-terminal-green/10">
                LIVE STREAM
              </span>
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
              title="Refresh Data"
            >
              <RefreshCw size={14} className={isSummaryLoading ? 'animate-spin' : ''} />
            </IndustrialButton>

            <IndustrialButton
              variant="hazard"
              size="md"
              onClick={() => setIsBreakdownModalOpen(true)}
            >
              <Flame size={16} className="mr-1.5" />
              <span>EMERGENCY BREAKDOWN</span>
            </IndustrialButton>
          </div>
        </div>

        {/* OEE Score Breakdown Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 pt-4">
          <div className="p-3 bg-industrial-900 border border-substrate-border">
            <div className="text-[10px] font-mono uppercase text-industrial-400">PLANT OEE</div>
            <div className="text-2xl sm:text-3xl font-bold font-mono text-white mt-1">
              {summary ? `${summary.plantOee}%` : '--'}
            </div>
            <div className="text-[10px] font-mono text-industrial-500 mt-1">Target: &ge;85.0%</div>
          </div>

          <div className="p-3 bg-industrial-900 border border-substrate-border">
            <div className="text-[10px] font-mono uppercase text-industrial-400">AVAILABILITY</div>
            <div className="text-2xl sm:text-3xl font-bold font-mono text-terminal-green mt-1">
              {summary ? `${summary.plantAvailability}%` : '--'}
            </div>
            <div className="text-[10px] font-mono text-industrial-500 mt-1">Uptime vs Planned</div>
          </div>

          <div className="p-3 bg-industrial-900 border border-substrate-border">
            <div className="text-[10px] font-mono uppercase text-industrial-400">PERFORMANCE</div>
            <div className="text-2xl sm:text-3xl font-bold font-mono text-hazard-amber mt-1">
              {summary ? `${summary.plantPerformance}%` : '--'}
            </div>
            <div className="text-[10px] font-mono text-industrial-500 mt-1">Operating Speed Index</div>
          </div>

          <div className="p-3 bg-industrial-900 border border-substrate-border">
            <div className="text-[10px] font-mono uppercase text-industrial-400">QUALITY RATE</div>
            <div className="text-2xl sm:text-3xl font-bold font-mono text-white mt-1">
              {summary ? `${summary.plantQuality}%` : '--'}
            </div>
            <div className="text-[10px] font-mono text-industrial-500 mt-1">Good Units / Total</div>
          </div>
        </div>
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

        {machines.length === 0 ? (
          <div className="text-center py-8 text-xs font-mono text-industrial-500">
            NO MACHINE ASSETS CONFIGURED. GO TO MACHINES VIEW TO REGISTER ASSETS.
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
                    <span className="text-[10px] font-mono text-industrial-500 uppercase">
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
                  <span className="text-[10px] font-mono text-industrial-500">
                    VER: v{m.version}
                  </span>
                  {m.status === 'DOWN' ? (
                    <span className="text-[10px] font-mono text-hazard-red font-bold animate-pulse">
                      [ REQUIRES ACTION ]
                    </span>
                  ) : (
                    <span className="text-[10px] font-mono text-industrial-400">
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
        title="EMERGENCY MACHINE BREAKDOWN"
        subtitle="Immediately halt machine and dispatch maintenance ticket"
        hazard={true}
      >
        <form onSubmit={handleReportBreakdown} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Select Affected Machine Asset *
            </label>
            <select
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
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Reason Classification Code *
            </label>
            <select
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
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Fault Description & Observations
            </label>
            <textarea
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
              Automatically generate High-Priority Maintenance Work Order
            </label>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setIsBreakdownModalOpen(false)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="hazard"
              size="lg"
              isLoading={isSubmitting}
            >
              CONFIRM BREAKDOWN & HALT ASSET
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
