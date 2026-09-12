import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { vibrationApi, api } from '../../services/api-client';
import { 
  MachineDto, 
  PagedResponse, 
  FftSpectrumDto, 
  MachineHealthAssessmentDto, 
  FleetHealthSummaryDto,
  FaultHarmonicType
} from '../../types';
import { IndustrialCard } from '../common/IndustrialCard';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { 
  Activity, 
  Radio, 
  ShieldCheck, 
  AlertTriangle, 
  Flame, 
  CheckCircle2, 
  Zap, 
  Disc,
  TrendingDown
} from 'lucide-react';

export const VibrationHealthView: React.FC = () => {
  // State
  const [selectedMachineId, setSelectedMachineId] = useState<string>('');
  const [selectedAxis, setSelectedAxis] = useState<string>('RADIAL_X');
  const [hoveredFrequency, setHoveredFrequency] = useState<{ freq: number; amp: number } | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Fetch Machines
  const { data: machinesData } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-list-vibration'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 50 }),
  });

  const machines = machinesData?.content || [];
  const activeMachineId = selectedMachineId || (machines.length > 0 ? machines[0].id : '');
  const activeMachine = machines.find(m => m.id === activeMachineId);

  // Queries
  const { data: fleetSummary, refetch: refetchFleet } = useQuery<FleetHealthSummaryDto>({
    queryKey: ['fleet-health-summary'],
    queryFn: () => vibrationApi.getFleetHealthSummary(),
    refetchInterval: 10000,
  });

  const { data: latestAssessment, refetch: refetchAssessment } = useQuery<MachineHealthAssessmentDto | null>({
    queryKey: ['machine-health-assessment', activeMachineId],
    queryFn: () => activeMachineId ? vibrationApi.getHealthAssessment(activeMachineId) : Promise.resolve(null),
    enabled: !!activeMachineId,
    refetchInterval: 5000,
  });

  const { data: latestSpectrum, refetch: refetchSpectrum } = useQuery<FftSpectrumDto | null>({
    queryKey: ['machine-fft-spectrum', activeMachineId],
    queryFn: () => activeMachineId ? vibrationApi.getSpectrum(activeMachineId) : Promise.resolve(null),
    enabled: !!activeMachineId,
    refetchInterval: 5000,
  });

  // Rehearsal Simulation Mutation
  const simulateMutation = useMutation({
    mutationFn: (faultType: FaultHarmonicType) =>
      vibrationApi.simulateBurst({
        machineId: activeMachineId,
        faultType,
        runningSpeedRpm: 3000.0,
        sampleRateHz: 2048.0,
        sampleCount: 1024,
        noiseLevel: 0.04,
      }),
    onSuccess: (res) => {
      refetchAssessment();
      refetchSpectrum();
      refetchFleet();
      setActionMessage(`Vibration burst analyzed: Health score is ${res.healthScore}% (${res.healthStatus}) - ISO 10816 ${res.isoSeverityZone}`);
    },
    onError: (err: any) => {
      setActionMessage(`Simulation error: ${err?.response?.data?.error?.message || err.message}`);
    }
  });

  // ISO 10816 Zone Color Helpers
  const getZoneBadgeVariant = (zone?: string): 'success' | 'info' | 'warning' | 'danger' => {
    switch (zone) {
      case 'ZONE_A': return 'success';
      case 'ZONE_B': return 'info';
      case 'ZONE_C': return 'warning';
      case 'ZONE_D': return 'danger';
      default: return 'info';
    }
  };

  const getHealthScoreColor = (score: number) => {
    if (score >= 90) return 'text-terminal-green border-terminal-green';
    if (score >= 75) return 'text-terminal-cyan border-terminal-cyan';
    if (score >= 60) return 'text-hazard-amber border-hazard-amber';
    return 'text-hazard-red border-hazard-red';
  };

  return (
    <div className="space-y-6">
      {/* Header & Machine Selector */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-substrate-border pb-4">
        <div>
          <div className="flex items-center gap-2 text-hazard-red text-xs font-mono font-bold tracking-widest uppercase">
            <Radio size={14} className="animate-pulse" />
            <span>PREDICTIVE MAINTENANCE SUITE // E6-S1</span>
          </div>
          <h1 className="text-xl sm:text-2xl font-mono font-black tracking-wider uppercase text-white mt-1">
            SPINDLE VIBRATION FFT & ISO 10816 HEALTH SCORING
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-industrial-900 border border-substrate-border px-3 py-1.5 rounded">
            <span className="text-[10px] font-mono text-industrial-400 uppercase">INSPECT SPINDLE:</span>
            <select
              value={activeMachineId}
              onChange={(e) => setSelectedMachineId(e.target.value)}
              aria-label="Select Machine for Vibration Analysis"
              className="bg-transparent text-xs font-mono font-bold text-white focus:outline-none"
            >
              {machines.map((m) => (
                <option key={m.id} value={m.id} className="bg-industrial-900 text-white">
                  {m.name} [{m.serialNumber}] - {m.status}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-1 bg-industrial-900 border border-substrate-border p-1 rounded text-xs font-mono">
            {(['RADIAL_X', 'RADIAL_Y', 'AXIAL_Z'] as const).map((axis) => (
              <button
                key={axis}
                onClick={() => setSelectedAxis(axis)}
                className={`px-2 py-0.5 rounded uppercase ${
                  selectedAxis === axis
                    ? 'bg-industrial-700 text-white font-bold'
                    : 'text-industrial-400 hover:text-white'
                }`}
              >
                {axis.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Action Notification Banner */}
      {actionMessage && (
        <div className="bg-industrial-800/80 border border-industrial-600 px-4 py-2 text-xs font-mono text-industrial-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-hazard-red animate-ping" />
            <span>{actionMessage}</span>
          </div>
          <button 
            onClick={() => setActionMessage(null)} 
            className="text-industrial-400 hover:text-white"
          >
            [DISMISS]
          </button>
        </div>
      )}

      {/* Top Fleet Health Metrics Overview */}
      {fleetSummary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">FLEET AVG HEALTH</div>
            <div className="text-xl font-mono font-black text-white mt-1">
              {fleetSummary.averageFleetHealthScore}%
            </div>
            <div className="text-[10px] font-mono text-terminal-green mt-0.5">
              {fleetSummary.totalMachinesAssessed} MACHINES MONITORED
            </div>
          </div>

          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">ISO ZONE A (GOOD)</div>
            <div className="text-xl font-mono font-black text-terminal-green mt-1">
              {fleetSummary.zoneACount}
            </div>
            <div className="text-[10px] font-mono text-industrial-400 mt-0.5">OPTIMAL KINEMATICS</div>
          </div>

          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">ISO ZONE B (ACCEPTABLE)</div>
            <div className="text-xl font-mono font-black text-terminal-cyan mt-1">
              {fleetSummary.zoneBCount}
            </div>
            <div className="text-[10px] font-mono text-industrial-400 mt-0.5">UNRESTRICTED RUN</div>
          </div>

          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">ISO ZONE C (ALERT)</div>
            <div className="text-xl font-mono font-black text-hazard-amber mt-1">
              {fleetSummary.zoneCCount}
            </div>
            <div className="text-[10px] font-mono text-hazard-amber mt-0.5">MAINTENANCE DUE</div>
          </div>

          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">ISO ZONE D (CRITICAL)</div>
            <div className="text-xl font-mono font-black text-hazard-red mt-1">
              {fleetSummary.zoneDCount}
            </div>
            <div className="text-[10px] font-mono text-hazard-red mt-0.5">TRIP / SHUTDOWN</div>
          </div>

          <div className="bg-substrate-card/90 border border-substrate-border p-3">
            <div className="text-[10px] font-mono text-industrial-400 uppercase">AT RISK (SCORE &lt; 60%)</div>
            <div className="text-xl font-mono font-black text-hazard-red mt-1">
              {fleetSummary.criticalCount + fleetSummary.warningCount}
            </div>
            <div className="text-[10px] font-mono text-industrial-400 mt-0.5">PRESCRIPTIVE ORDERS</div>
          </div>
        </div>
      )}

      {/* Main Grid: Health Gauge + FFT Spectrum Waterfall */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column: Machine Health Assessment & ISO Gauge (4 Columns) */}
        <div className="lg:col-span-4 space-y-6">
          <IndustrialCard className="p-4 space-y-5">
            <div className="flex items-center justify-between border-b border-substrate-border pb-3">
              <div>
                <div className="text-xs font-mono font-bold text-white uppercase">
                  {activeMachine?.name || 'SPINDLE TELEMETRY'}
                </div>
                <div className="text-[10px] font-mono text-industrial-400">
                  SERIAL: {activeMachine?.serialNumber} // {selectedAxis}
                </div>
              </div>
              <IndustrialBadge 
                variant={getZoneBadgeVariant(latestAssessment?.isoSeverityZone)}
                size="sm"
              >
                {latestAssessment?.isoSeverityZone || 'ZONE A'}
              </IndustrialBadge>
            </div>

            {/* Health Score Circular Brutalist Gauge */}
            <div className="flex flex-col items-center justify-center p-4 bg-black/40 border border-substrate-border rounded">
              <div className={`w-32 h-32 rounded-full border-4 flex flex-col items-center justify-center shadow-lg ${getHealthScoreColor(latestAssessment?.healthScore || 100)}`}>
                <span className="text-3xl font-mono font-black text-white">
                  {latestAssessment ? latestAssessment.healthScore : 100}%
                </span>
                <span className="text-[9px] font-mono tracking-widest uppercase text-industrial-400">
                  HEALTH SCORE
                </span>
              </div>
              <div className="mt-3 text-xs font-mono font-bold uppercase text-white tracking-wider">
                STATUS: {latestAssessment?.healthStatus || 'EXCELLENT'}
              </div>
              <div className="text-[10px] font-mono text-industrial-400 text-center mt-1">
                {latestAssessment?.healthStatusDescription || 'Optimal mechanical baseline. Zero fault harmonics.'}
              </div>
            </div>

            {/* Physical Telemetry Indicators */}
            <div className="grid grid-cols-2 gap-2 text-xs font-mono">
              <div className="bg-industrial-900 border border-substrate-border p-2.5 rounded">
                <div className="text-[10px] text-industrial-400 uppercase">VELOCITY RMS</div>
                <div className="text-sm font-bold text-white mt-0.5">
                  {latestAssessment ? latestAssessment.rmsVelocityMmS : 1.42} mm/s
                </div>
                <div className="text-[9px] text-industrial-500">ISO 10816 10-1000Hz</div>
              </div>

              <div className="bg-industrial-900 border border-substrate-border p-2.5 rounded">
                <div className="text-[10px] text-industrial-400 uppercase flex items-center gap-1">
                  <Flame size={10} className="text-hazard-amber" /> BEARING TEMP
                </div>
                <div className="text-sm font-bold text-white mt-0.5">
                  {latestAssessment?.spindleTemperatureC ? `${latestAssessment.spindleTemperatureC}°C` : '42.5°C'}
                </div>
                <div className="text-[9px] text-industrial-500">MAX 75°C LIMIT</div>
              </div>

              <div className="bg-industrial-900 border border-substrate-border p-2.5 rounded">
                <div className="text-[10px] text-industrial-400 uppercase">CREST FACTOR</div>
                <div className="text-sm font-bold text-white mt-0.5">
                  {latestSpectrum ? latestSpectrum.crestFactor : 2.10}
                </div>
                <div className="text-[9px] text-industrial-500">PEAK / RMS RATIO</div>
              </div>

              <div className="bg-industrial-900 border border-substrate-border p-2.5 rounded">
                <div className="text-[10px] text-industrial-400 uppercase">KURTOSIS</div>
                <div className="text-sm font-bold text-white mt-0.5">
                  {latestSpectrum ? latestSpectrum.kurtosis : 2.95}
                </div>
                <div className="text-[9px] text-industrial-500">GAUSSIAN REF ~3.0</div>
              </div>
            </div>

            {/* ISO 10816-3 Severity Bar */}
            <div className="space-y-1.5 font-mono text-xs">
              <div className="flex items-center justify-between text-[10px] text-industrial-400">
                <span>ISO 10816-3 SEVERITY BANDS (CLASS II)</span>
                <span className="font-bold text-white">{latestAssessment?.rmsVelocityMmS || 1.42} mm/s</span>
              </div>
              <div className="grid grid-cols-4 h-3 gap-0.5 bg-black/60 p-0.5 border border-substrate-border">
                <div className="bg-emerald-600/80" title="Zone A: Good (< 1.12 mm/s)" />
                <div className="bg-cyan-600/80" title="Zone B: Satisfactory (1.12 - 2.8 mm/s)" />
                <div className="bg-amber-600/80" title="Zone C: Alert (2.8 - 7.1 mm/s)" />
                <div className="bg-red-600/80" title="Zone D: Critical (> 7.1 mm/s)" />
              </div>
              <div className="flex justify-between text-[9px] text-industrial-500">
                <span>0 mm/s</span>
                <span>1.12</span>
                <span>2.80</span>
                <span>7.10</span>
                <span>15+</span>
              </div>
            </div>

            {/* Diagnostic Summary & Recommendations */}
            <div className="bg-industrial-900/80 border border-substrate-border p-3 rounded space-y-2 text-xs font-mono">
              <div className="flex items-center gap-1.5 text-industrial-300 font-bold uppercase text-[11px]">
                <ShieldCheck size={14} className="text-terminal-green" /> DIAGNOSTIC ASSESSMENT:
              </div>
              <p className="text-industrial-200 text-[11px] leading-relaxed">
                {latestAssessment?.diagnosisSummary || 'Spindle vibration velocity well within ISO 10816 Zone A. Kinematics nominal.'}
              </p>
              {latestAssessment?.recommendedAction && (
                <div className="pt-2 border-t border-substrate-border text-[11px] text-hazard-amber">
                  <span className="font-bold uppercase">PRESCRIPTIVE ACTION: </span>
                  {latestAssessment.recommendedAction}
                </div>
              )}
            </div>

            {/* Active Prescriptive Maintenance Ticket Callout */}
            {latestAssessment?.activeWorkOrder && (
              <div className="bg-amber-950/40 border border-amber-500/80 p-3 rounded space-y-2 text-xs font-mono animate-fade-in">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-amber-300 font-bold uppercase text-[11px]">
                    <Zap size={14} className="text-amber-400 animate-pulse" />
                    <span>PRESCRIPTIVE WORK ORDER DISPATCHED</span>
                  </div>
                  <span className="px-1.5 py-0.5 bg-amber-900 border border-amber-600 text-amber-200 text-[9px] font-bold uppercase">
                    {latestAssessment.activeWorkOrder.priority}
                  </span>
                </div>
                <div className="text-white font-bold text-xs">
                  {latestAssessment.activeWorkOrder.workOrderNumber} // {latestAssessment.activeWorkOrder.status}
                </div>
                {latestAssessment.activeWorkOrder.assignedToName && (
                  <div className="text-[10px] text-industrial-300">
                    ASSIGNED TO: <span className="text-cyan-300 font-bold">{latestAssessment.activeWorkOrder.assignedToName}</span>
                  </div>
                )}
                {latestAssessment.activeWorkOrder.recommendedParts && (
                  <div className="text-[10px] text-industrial-400">
                    PARTS: {latestAssessment.activeWorkOrder.recommendedParts}
                  </div>
                )}
              </div>
            )}
          </IndustrialCard>
        </div>

        {/* Right Column: Fast Fourier Transform (FFT) Frequency Spectrum (8 Columns) */}
        <div className="lg:col-span-8 space-y-6">
          <IndustrialCard className="p-4 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-substrate-border pb-3">
              <div>
                <h2 className="text-xs font-mono font-bold text-white uppercase flex items-center gap-2">
                  <Activity size={14} className="text-hazard-red" />
                  FAST FOURIER TRANSFORM (FFT) VELOCITY SPECTRUM // 0 - 1000 HZ
                </h2>
                <div className="text-[10px] font-mono text-industrial-400">
                  SAMPLE RATE: {latestSpectrum?.sampleRateHz || 2048} HZ // 1X RUN SPEED: {latestSpectrum?.fundamentalFrequencyHz || 50.0} HZ (3000 RPM)
                </div>
              </div>

              {hoveredFrequency && (
                <div className="bg-black/60 border border-cyan-500/50 px-2 py-1 text-[11px] font-mono text-cyan-300 rounded">
                  FREQ: {hoveredFrequency.freq} Hz // AMP: {hoveredFrequency.amp} mm/s
                </div>
              )}
            </div>

            {/* SVG Frequency Spectrum Chart */}
            <div className="relative bg-black/90 border border-substrate-border rounded p-3 h-[240px] flex flex-col justify-end overflow-hidden">
              {/* Grid Lines */}
              <div 
                className="absolute inset-0 opacity-15 pointer-events-none"
                style={{
                  backgroundImage: 'linear-gradient(#38bdf8 1px, transparent 1px), linear-gradient(90deg, #38bdf8 1px, transparent 1px)',
                  backgroundSize: '24px 24px'
                }}
              />

              {/* Spectrum Bars / Polyline */}
              <svg className="w-full h-full" viewBox="0 0 1000 200" preserveAspectRatio="none">
                {/* Horizontal reference lines */}
                <line x1="0" y1="150" x2="1000" y2="150" stroke="#334155" strokeWidth="1" strokeDasharray="4 4" />
                <line x1="0" y1="100" x2="1000" y2="100" stroke="#334155" strokeWidth="1" strokeDasharray="4 4" />
                <line x1="0" y1="50" x2="1000" y2="50" stroke="#334155" strokeWidth="1" strokeDasharray="4 4" />

                {/* Draw spectral curve */}
                {latestSpectrum?.frequencies && latestSpectrum.frequencies.length > 0 && (() => {
                  const maxAmp = Math.max(2.5, ...latestSpectrum.amplitudes);
                  const points = latestSpectrum.frequencies.map((f, i) => {
                    const x = (f / 1000.0) * 1000.0;
                    const y = 190 - (latestSpectrum.amplitudes[i] / maxAmp) * 170;
                    return `${x},${y}`;
                  }).join(' ');

                  return (
                    <g>
                      <polygon
                        points={`0,195 ${points} 1000,195`}
                        fill="rgba(56, 189, 248, 0.15)"
                      />
                      <polyline
                        points={points}
                        fill="none"
                        stroke="#38bdf8"
                        strokeWidth="1.5"
                      />
                    </g>
                  );
                })()}

                {/* Peak Indicators & Harmonic Tags */}
                {latestSpectrum?.peaks?.map((peak, idx) => {
                  const x = (peak.frequencyHz / 1000.0) * 1000.0;
                  return (
                    <g 
                      key={idx} 
                      className="cursor-pointer"
                      onMouseEnter={() => setHoveredFrequency({ freq: peak.frequencyHz, amp: peak.amplitudeMmS })}
                      onMouseLeave={() => setHoveredFrequency(null)}
                    >
                      <line x1={x} y1={25} x2={x} y2={190} stroke="#ef4444" strokeWidth="1" strokeDasharray="2 2" />
                      <circle cx={x} cy={35} r={5} fill="#ef4444" className="hover:scale-125 transition-transform" />
                      <text x={Math.min(920, x + 6)} y={38} fill="#fca5a5" fontSize="10" fontFamily="monospace" fontWeight="bold">
                        {peak.faultHarmonicType !== 'NORMAL' ? peak.faultHarmonicType : `${peak.frequencyHz}Hz`}
                      </text>
                    </g>
                  );
                })}
              </svg>

              {/* Axis markers */}
              <div className="flex justify-between text-[9px] font-mono text-industrial-400 pt-1 border-t border-substrate-border mt-1">
                <span>0 Hz</span>
                <span>200 Hz</span>
                <span>400 Hz</span>
                <span>600 Hz</span>
                <span>800 Hz</span>
                <span>1000 Hz</span>
              </div>
            </div>

            {/* Dominant Peak Harmonics Table */}
            <div className="space-y-2">
              <div className="text-xs font-mono font-bold text-industrial-300 uppercase">
                DOMINANT SPECTRAL HARMONICS & DEFECT IDENTIFICATION:
              </div>

              <div className="border border-substrate-border overflow-x-auto">
                <table className="w-full text-left font-mono text-xs">
                  <thead className="bg-industrial-900 border-b border-substrate-border text-[10px] text-industrial-400 uppercase">
                    <tr>
                      <th className="p-2">FREQUENCY (HZ)</th>
                      <th className="p-2">AMPLITUDE (MM/S)</th>
                      <th className="p-2">ORDER</th>
                      <th className="p-2">KINEMATIC FAULT CLASSIFICATION</th>
                      <th className="p-2 text-right">CONFIDENCE</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-substrate-border">
                    {latestSpectrum?.peaks && latestSpectrum.peaks.length > 0 ? (
                      latestSpectrum.peaks.map((peak, idx) => (
                        <tr key={idx} className="hover:bg-industrial-900/60">
                          <td className="p-2 font-bold text-white">{peak.frequencyHz.toFixed(1)} Hz</td>
                          <td className="p-2 text-cyan-400 font-bold">{peak.amplitudeMmS.toFixed(2)} mm/s</td>
                          <td className="p-2 text-industrial-300">{peak.orderMultiple ? `${peak.orderMultiple.toFixed(2)}X` : '-'}</td>
                          <td className="p-2">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              peak.faultHarmonicType === 'NORMAL'
                                ? 'bg-emerald-950 text-emerald-400 border border-emerald-800'
                                : 'bg-red-950 text-red-400 border border-red-800'
                            }`}>
                              {peak.faultHarmonicType}
                            </span>
                          </td>
                          <td className="p-2 text-right text-industrial-400">{(peak.confidence * 100).toFixed(0)}%</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="p-3 text-center text-industrial-500 text-xs">
                          No significant harmonic peaks detected above noise floor.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </IndustrialCard>

          {/* Synthetic Vibration Burst Rehearsal Simulator */}
          <IndustrialCard className="p-4 space-y-3">
            <div className="flex items-center justify-between border-b border-substrate-border pb-2">
              <span className="text-xs font-mono font-bold text-white uppercase flex items-center gap-1.5">
                <Zap size={14} className="text-hazard-amber" />
                VIBRATION DEFECT SIMULATOR // REHEARSAL & ISO GATING
              </span>
              <span className="text-[10px] font-mono text-industrial-400">
                TEST FAULT HARMONICS ON ACTIVE MACHINE
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              <button
                onClick={() => simulateMutation.mutate('NORMAL')}
                disabled={simulateMutation.isPending}
                className="p-2.5 bg-emerald-950/40 hover:bg-emerald-900/60 border border-emerald-600/60 text-emerald-300 rounded text-xs font-mono font-bold uppercase transition-all flex flex-col items-center gap-1"
              >
                <CheckCircle2 size={16} />
                <span>HEALTHY BASELINE</span>
                <span className="text-[9px] font-normal text-emerald-400/80">Zone A // 100% Score</span>
              </button>

              <button
                onClick={() => simulateMutation.mutate('UNBALANCE_1X')}
                disabled={simulateMutation.isPending}
                className="p-2.5 bg-industrial-900 hover:bg-industrial-800 border border-substrate-border text-industrial-200 rounded text-xs font-mono font-bold uppercase transition-all flex flex-col items-center gap-1"
              >
                <Disc size={16} className="text-cyan-400" />
                <span>1X ROTOR UNBALANCE</span>
                <span className="text-[9px] font-normal text-industrial-400">Elevated 1X Run Speed</span>
              </button>

              <button
                onClick={() => simulateMutation.mutate('MISALIGNMENT_2X')}
                disabled={simulateMutation.isPending}
                className="p-2.5 bg-amber-950/40 hover:bg-amber-900/60 border border-amber-600/60 text-amber-300 rounded text-xs font-mono font-bold uppercase transition-all flex flex-col items-center gap-1"
              >
                <AlertTriangle size={16} />
                <span>2X MISALIGNMENT</span>
                <span className="text-[9px] font-normal text-amber-400/80">Coupling Angular / Offset</span>
              </button>

              <button
                onClick={() => simulateMutation.mutate('BPFO_BEARING_OUTER')}
                disabled={simulateMutation.isPending}
                className="p-2.5 bg-red-950/40 hover:bg-red-900/60 border border-red-600/60 text-red-300 rounded text-xs font-mono font-bold uppercase transition-all flex flex-col items-center gap-1"
              >
                <TrendingDown size={16} />
                <span>BPFO BEARING WEAR</span>
                <span className="text-[9px] font-normal text-red-400/80">Outer Race Spall ~3.58X</span>
              </button>
            </div>
          </IndustrialCard>
        </div>
      </div>
    </div>
  );
};
