import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '../../context/AuthContext';
import { 
  Radio, 
  Activity, 
  Cpu, 
  AlertTriangle, 
  Gauge, 
  Zap, 
  Thermometer, 
  ShieldCheck, 
  Plus, 
  Trash2, 
  RefreshCw, 
  CheckCircle2,
  Sliders,
  Database,
  BarChart3,
  Archive,
  Info
} from 'lucide-react';
import { telemetryApi, api } from '../../services/api-client';
import { 
  MachineDto, 
  MachineLiveTelemetry, 
  TagMapping, 
  CreateTagMappingRequest, 
  ProtocolType,
  TimeSeriesResponse,
  TimeSeriesBucket,
  RetentionReport
} from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { StatusBeacon } from '../common/StatusBeacon';
import { Modal } from '../common/Modal';

export const LiveTelemetryView: React.FC = () => {
  const { hasRole } = useAuth();
  const canManageTags = hasRole('ADMIN', 'ENGINEER');
  const canIngest = hasRole('ADMIN', 'ENGINEER');
  const canManageRetention = hasRole('ADMIN');

  const [machines, setMachines] = useState<MachineDto[]>([]);
  const [selectedMachineId, setSelectedMachineId] = useState<string>('');
  const [liveData, setLiveData] = useState<MachineLiveTelemetry | null>(null);
  const [tagMappings, setTagMappings] = useState<TagMapping[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [autoPoll, setAutoPoll] = useState<boolean>(true);
  const [activityLog, setActivityLog] = useState<Array<{ id: string; time: string; msg: string; type: 'info' | 'alert' | 'success' }>>([]);

  // Time-Series Downsampling & Historical Analytics State
  const [selectedTag, setSelectedTag] = useState<string>('SPINDLE_SPEED');
  const [timeRange, setTimeRange] = useState<'1h' | '24h' | '7d' | '30d'>('24h');
  const [seriesData, setSeriesData] = useState<TimeSeriesResponse | null>(null);
  const [isSeriesLoading, setIsSeriesLoading] = useState<boolean>(false);
  const [queryDurationMs, setQueryDurationMs] = useState<number | null>(null);
  const [hoveredBucket, setHoveredBucket] = useState<TimeSeriesBucket | null>(null);

  // Retention Policy Management State
  const [isRetentionModalOpen, setIsRetentionModalOpen] = useState<boolean>(false);
  const [isExecutingRetention, setIsExecutingRetention] = useState<boolean>(false);
  const [retentionReport, setRetentionReport] = useState<RetentionReport | null>(null);
  const [retentionMessage, setRetentionMessage] = useState<string | null>(null);

  // Modal for adding tag mapping
  const [isTagModalOpen, setIsTagModalOpen] = useState<boolean>(false);
  const [newTagName, setNewTagName] = useState<string>('');
  const [newProtocol, setNewProtocol] = useState<ProtocolType>('OPC_UA');
  const [newTagAddress, setNewTagAddress] = useState<string>('');
  const [newUnit, setNewUnit] = useState<string>('RPM');
  const [newScaleFactor, setNewScaleFactor] = useState<number>(1.0);
  const [isSubmittingTag, setIsSubmittingTag] = useState<boolean>(false);
  const [tagError, setTagError] = useState<string | null>(null);

  // Available tags for the asset
  const availableTags = useMemo(() => {
    const baseTags = ['SPINDLE_SPEED', 'VIBRATION_RMS', 'MOTOR_CURRENT', 'BEARING_TEMP'];
    const mappedTags = tagMappings.map((t) => t.tagName);
    return Array.from(new Set([...baseTags, ...mappedTags]));
  }, [tagMappings]);

  // Fetch machine list on mount
  useEffect(() => {
    const fetchMachines = async () => {
      try {
        const res = await api.get<{ content: MachineDto[] }>('/machines', { size: 100 });
        if (res && res.content && res.content.length > 0) {
          setMachines(res.content);
          setSelectedMachineId(res.content[0].id);
        }
      } catch (err) {
        console.error('Failed to load machines:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchMachines();
  }, []);

  // Fetch live telemetry & tags for selected machine
  const fetchTelemetry = useCallback(async (machineId: string, showSpinner = false) => {
    if (!machineId) return;
    if (showSpinner) setIsRefreshing(true);

    try {
      const [live, tags] = await Promise.all([
        telemetryApi.getLiveTelemetry(machineId),
        telemetryApi.getTagMappings(machineId)
      ]);
      setLiveData(live);
      setTagMappings(tags || []);
    } catch (err) {
      console.error('Error fetching live telemetry:', err);
    } finally {
      if (showSpinner) setIsRefreshing(false);
    }
  }, []);

  // Fetch downsampled time-series data
  const fetchTimeSeries = useCallback(async (machineId: string, tag: string, range: '1h' | '24h' | '7d' | '30d') => {
    if (!machineId || !tag) return;
    setIsSeriesLoading(true);
    const startTime = performance.now();

    const now = new Date();
    const fromDate = new Date();
    if (range === '1h') fromDate.setHours(now.getHours() - 1);
    else if (range === '24h') fromDate.setHours(now.getHours() - 24);
    else if (range === '7d') fromDate.setDate(now.getDate() - 7);
    else if (range === '30d') fromDate.setDate(now.getDate() - 30);

    try {
      const res = await telemetryApi.getTimeSeries(machineId, tag, {
        from: fromDate.toISOString(),
        to: now.toISOString(),
        bucket: 'AUTO',
      });
      setSeriesData(res);
      setQueryDurationMs(Math.round(performance.now() - startTime));
    } catch (err) {
      console.error('Failed to fetch time-series:', err);
    } finally {
      setIsSeriesLoading(false);
    }
  }, []);

  // Load telemetry & time-series when selected machine changes
  useEffect(() => {
    if (selectedMachineId) {
      fetchTelemetry(selectedMachineId, true);
      fetchTimeSeries(selectedMachineId, selectedTag, timeRange);
    }
  }, [selectedMachineId, fetchTelemetry, fetchTimeSeries, selectedTag, timeRange]);

  // Periodic auto-polling for live stream
  useEffect(() => {
    if (!autoPoll || !selectedMachineId) return;
    const interval = setInterval(() => {
      fetchTelemetry(selectedMachineId, false);
    }, 3000);
    return () => clearInterval(interval);
  }, [autoPoll, selectedMachineId, fetchTelemetry]);

  // Handle Tag Mapping Creation
  const handleCreateTag = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMachineId || !newTagName || !newTagAddress) return;

    setIsSubmittingTag(true);
    setTagError(null);

    const payload: CreateTagMappingRequest = {
      tagName: newTagName.trim().toUpperCase(),
      protocol: newProtocol,
      tagAddress: newTagAddress.trim(),
      dataType: 'DOUBLE',
      unitOfMeasure: newUnit.trim(),
      scaleFactor: Number(newScaleFactor) || 1.0,
    };

    try {
      await telemetryApi.createTagMapping(selectedMachineId, payload);
      setIsTagModalOpen(false);
      setNewTagName('');
      setNewTagAddress('');
      fetchTelemetry(selectedMachineId, false);
      addLogEntry(`Tag '${payload.tagName}' mapped to ${payload.tagAddress} [${payload.protocol}]`, 'success');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create tag mapping';
      setTagError(msg);
    } finally {
      setIsSubmittingTag(false);
    }
  };

  // Handle Tag Deletion
  const handleDeleteTag = async (mappingId: string, tagName: string) => {
    if (!selectedMachineId || !confirm(`Remove tag mapping '${tagName}'?`)) return;
    try {
      await telemetryApi.deleteTagMapping(selectedMachineId, mappingId);
      fetchTelemetry(selectedMachineId, false);
      addLogEntry(`Tag '${tagName}' removed from controller registry`, 'info');
    } catch (err) {
      console.error('Failed to delete tag mapping:', err);
    }
  };

  // Simulate Edge Gateway Ingestion Pulse
  const handleSimulateEdgePulse = async () => {
    if (!selectedMachineId) return;
    setIsRefreshing(true);

    const simulatedSpeed = Math.floor(7500 + Math.random() * 3000);
    const simulatedVib = +(1.2 + Math.random() * 3.8).toFixed(2);
    const simulatedCurrent = +(18.0 + Math.random() * 15.0).toFixed(1);
    const simulatedTemp = +(45.0 + Math.random() * 38.0).toFixed(1);

    try {
      const res = await telemetryApi.ingestBatch({
        machineId: selectedMachineId,
        gatewayId: 'GW-EDGE-V2-SIMULATOR',
        points: [
          { tagName: 'SPINDLE_SPEED', value: simulatedSpeed, unit: 'RPM', quality: 'GOOD' },
          { tagName: 'VIBRATION_RMS', value: simulatedVib, unit: 'mm/s', quality: 'GOOD' },
          { tagName: 'MOTOR_CURRENT', value: simulatedCurrent, unit: 'A', quality: 'GOOD' },
          { tagName: 'BEARING_TEMP', value: simulatedTemp, unit: '°C', quality: 'GOOD' },
        ],
      });

      addLogEntry(
        `Edge Gateway ingested 4 points: Speed ${simulatedSpeed} RPM, Vib ${simulatedVib} mm/s, Temp ${simulatedTemp} °C`,
        res.alerts && res.alerts.length > 0 ? 'alert' : 'success'
      );

      if (res.alerts && res.alerts.length > 0) {
        res.alerts.forEach((alert) => addLogEntry(alert, 'alert'));
      }

      await fetchTelemetry(selectedMachineId, false);
      await fetchTimeSeries(selectedMachineId, selectedTag, timeRange);
    } catch (err) {
      console.error('Simulation failed:', err);
      addLogEntry('Edge telemetry packet failed to ingest', 'alert');
    } finally {
      setIsRefreshing(false);
    }
  };

  // Execute Retention Pruning Policy
  const handleExecuteRetention = async () => {
    setIsExecutingRetention(true);
    setRetentionMessage(null);
    try {
      const res = await telemetryApi.executeRetention();
      setRetentionReport(res);
      setRetentionMessage(`Pruned ${res.rawPointsPruned} raw points, ${res.rollups1mPruned} 1m rollups, ${res.rollups1hPruned} 1h rollups in ${res.executionTimeMs}ms`);
      addLogEntry(
        `Retention policy executed: ${res.rawPointsPruned} raw, ${res.rollups1mPruned} 1m, ${res.rollups1hPruned} 1h pruned (${res.executionTimeMs}ms)`,
        'info'
      );
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Retention execution failed';
      setRetentionMessage(msg);
    } finally {
      setIsExecutingRetention(false);
    }
  };

  const addLogEntry = (msg: string, type: 'info' | 'alert' | 'success') => {
    const time = new Date().toLocaleTimeString();
    setActivityLog((prev) => [{ id: Math.random().toString(), time, msg, type }, ...prev.slice(0, 19)]);
  };

  // Metric summaries for the active time-series view
  const seriesStats = useMemo(() => {
    if (!seriesData || !seriesData.series || seriesData.series.length === 0) {
      return { min: 0, max: 0, avg: 0, count: 0, latest: 0 };
    }
    const b: TimeSeriesBucket[] = seriesData.series;
    let min = Infinity;
    let max = -Infinity;
    let sum = 0;
    let totalSamples = 0;

    b.forEach((pt: TimeSeriesBucket) => {
      if (pt.min < min) min = pt.min;
      if (pt.max > max) max = pt.max;
      sum += pt.avg * (pt.count || 1);
      totalSamples += (pt.count || 1);
    });

    const avg = totalSamples > 0 ? sum / totalSamples : 0;
    const latest = b[b.length - 1].avg;

    return {
      min: min === Infinity ? 0 : min,
      max: max === -Infinity ? 0 : max,
      avg,
      count: b.length,
      latest
    };
  }, [seriesData]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-12 text-industrial-400 font-mono">
        <RefreshCw className="animate-spin mr-3" size={20} />
        INITIALIZING IIoT PROTOCOL INTERFACE & TIME-SERIES ENGINE...
      </div>
    );
  }

  const selectedMachine = machines.find((m) => m.id === selectedMachineId);
  const healthColor =
    !liveData || liveData.healthScore > 80
      ? 'text-emerald-400 border-emerald-500/40 bg-emerald-950/20'
      : liveData.healthScore > 50
      ? 'text-amber-400 border-amber-500/40 bg-amber-950/20'
      : 'text-rose-400 border-rose-500/40 bg-rose-950/20';

  const vibLimitExceeded = (liveData?.vibrationMmPerSec || 0) > 4.5;
  const tempLimitExceeded = (liveData?.bearingTempCelsius || 0) > 80.0;

  // Render SVG Chart calculations
  const svgWidth = 900;
  const svgHeight = 220;
  const padLeft = 60;
  const padRight = 30;
  const padTop = 20;
  const padBottom = 35;
  const plotW = svgWidth - padLeft - padRight;
  const plotH = svgHeight - padTop - padBottom;

  const buckets: TimeSeriesBucket[] = seriesData?.series || [];
  const minVal = seriesStats.min;
  const maxVal = seriesStats.max;
  const valSpan = maxVal === minVal ? (maxVal === 0 ? 10 : maxVal * 0.2) : (maxVal - minVal);
  const yDomainMin = Math.max(0, minVal - valSpan * 0.08);
  const yDomainMax = maxVal + valSpan * 0.08;
  const yDomainSpan = (yDomainMax - yDomainMin) || 1;

  interface PlotPoint {
    x: number;
    yAvg: number;
    yMin: number;
    yMax: number;
    bucket: TimeSeriesBucket;
  }

  const plotPoints: PlotPoint[] = buckets.map((b: TimeSeriesBucket, idx: number) => {
    const x = padLeft + (buckets.length > 1 ? (idx / (buckets.length - 1)) * plotW : plotW / 2);
    const yAvg = padTop + plotH - ((b.avg - yDomainMin) / yDomainSpan) * plotH;
    const yMin = padTop + plotH - ((b.min - yDomainMin) / yDomainSpan) * plotH;
    const yMax = padTop + plotH - ((b.max - yDomainMin) / yDomainSpan) * plotH;
    return { x, yAvg, yMin, yMax, bucket: b };
  });

  const avgPath = plotPoints.length > 0
    ? `M ${plotPoints.map((p: PlotPoint) => `${p.x.toFixed(1)},${p.yAvg.toFixed(1)}`).join(' L ')}`
    : '';

  const envelopePath = plotPoints.length > 0
    ? `M ${plotPoints.map((p: PlotPoint) => `${p.x.toFixed(1)},${p.yMax.toFixed(1)}`).join(' L ')} L ${plotPoints.slice().reverse().map((p: PlotPoint) => `${p.x.toFixed(1)},${p.yMin.toFixed(1)}`).join(' L ')} Z`
    : '';

  return (
    <div className="space-y-6">
      {/* Header & Machine Selector */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-substrate-border pb-4">
        <div>
          <div className="flex items-center gap-2">
            <Radio className="text-industrial-accent animate-pulse" size={20} />
            <h1 className="text-xl font-bold font-mono tracking-wider uppercase text-white">
              AUTOMATED TELEMETRY & IIoT GATEWAY
            </h1>
            <span className="px-2 py-0.5 text-[10px] font-mono bg-industrial-accent/20 text-industrial-accent border border-industrial-accent/40 font-bold">
              SPRINT 6 // V2.0
            </span>
          </div>
          <p className="text-xs font-mono text-industrial-400 mt-1">
            Real-time high-frequency PLC controller stream ingestion (OPC-UA, MQTT Sparkplug B) & Continuous Downsampling Engine
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Machine selector */}
          <div className="flex items-center gap-2 bg-substrate-card border border-substrate-border px-3 py-1.5">
            <Cpu size={14} className="text-industrial-400" />
            <span className="text-xs font-mono font-bold text-industrial-300">ASSET:</span>
            <select
              value={selectedMachineId}
              onChange={(e) => setSelectedMachineId(e.target.value)}
              className="bg-transparent text-xs font-mono font-bold text-white border-none focus:outline-none cursor-pointer"
            >
              {machines.map((m) => (
                <option key={m.id} value={m.id} className="bg-industrial-900 text-white">
                  {m.name} [{m.serialNumber}]
                </option>
              ))}
            </select>
          </div>

          {/* Auto-poll toggle */}
          <button
            onClick={() => setAutoPoll(!autoPoll)}
            className={`px-3 py-1.5 text-xs font-mono font-bold border flex items-center gap-1.5 transition-all ${
              autoPoll
                ? 'bg-emerald-950/40 text-emerald-300 border-emerald-500/50'
                : 'bg-industrial-800 text-industrial-400 border-substrate-border'
            }`}
          >
            <RefreshCw size={12} className={autoPoll ? 'animate-spin' : ''} />
            {autoPoll ? 'LIVE STREAM (3s)' : 'STREAM PAUSED'}
          </button>

          {/* Retention Policy Modal (Admin) */}
          {canManageRetention && (
            <IndustrialButton
              variant="secondary"
              size="sm"
              onClick={() => setIsRetentionModalOpen(true)}
            >
              <Database size={14} className="text-cyan-400 mr-1" />
              RETENTION
            </IndustrialButton>
          )}

          {/* Ingest Simulation (Available for Admin/Engineer) */}
          {canIngest && (
            <IndustrialButton
              variant="secondary"
              size="sm"
              onClick={handleSimulateEdgePulse}
              disabled={isRefreshing}
            >
              <Zap size={14} className="text-amber-400 mr-1" />
              SIMULATE EDGE PULSE
            </IndustrialButton>
          )}

          {/* Add Tag Mapping */}
          {canManageTags && (
            <IndustrialButton
              variant="primary"
              size="sm"
              onClick={() => setIsTagModalOpen(true)}
            >
              <Plus size={14} className="mr-1" />
              REGISTER PLC TAG
            </IndustrialButton>
          )}
        </div>
      </div>

      {/* Asset Status & Gateway Bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-substrate-card border border-substrate-border p-4 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-mono text-industrial-400 uppercase tracking-wider">Asset Identity</div>
            <div className="text-sm font-mono font-bold text-white mt-1">{selectedMachine?.name || 'N/A'}</div>
            <div className="text-xs font-mono text-industrial-400">{selectedMachine?.serialNumber}</div>
          </div>
          <StatusBeacon
            status={selectedMachine?.status || 'IDLE'}
            showLabel={true}
          />
        </div>

        <div className="bg-substrate-card border border-substrate-border p-4 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-mono text-industrial-400 uppercase tracking-wider">Protocol Bridge</div>
            <div className="text-sm font-mono font-bold text-white mt-1">
              {liveData?.activeProtocol === 'MQTT_SPARKPLUG_B' ? 'MQTT SPARKPLUG B' : 'OPC-UA BINARY'}
            </div>
            <div className="text-xs font-mono text-industrial-400">
              {tagMappings.length} Configured Tag{tagMappings.length === 1 ? '' : 's'}
            </div>
          </div>
          <div className="px-2 py-1 bg-substrate-card border border-industrial-600 text-xs font-mono font-bold text-industrial-accent">
            TCP/IP
          </div>
        </div>

        <div className="bg-substrate-card border border-substrate-border p-4 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-mono text-industrial-400 uppercase tracking-wider">Gateway Stream Status</div>
            <div className="text-sm font-mono font-bold text-white mt-1 flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${liveData?.connectionStatus === 'ONLINE' ? 'bg-emerald-400 animate-ping' : 'bg-rose-500'}`} />
              {liveData?.connectionStatus || 'OFFLINE'}
            </div>
            <div className="text-xs font-mono text-industrial-400">
              Heartbeat: {liveData?.lastHeartbeat ? new Date(liveData.lastHeartbeat).toLocaleTimeString() : 'Awaiting data'}
            </div>
          </div>
          <Activity size={20} className={liveData?.connectionStatus === 'ONLINE' ? 'text-emerald-400' : 'text-rose-400'} />
        </div>

        <div className={`border p-4 flex items-center justify-between ${healthColor}`}>
          <div>
            <div className="text-[10px] font-mono uppercase tracking-wider opacity-80">Health Assessment</div>
            <div className="text-2xl font-mono font-bold mt-0.5">{liveData?.healthScore ?? 100}%</div>
            <div className="text-[10px] font-mono opacity-80">
              {liveData?.healthScore && liveData.healthScore < 60 ? 'CRITICAL DEGRADATION' : 'NOMINAL ENVELOPE'}
            </div>
          </div>
          <ShieldCheck size={28} />
        </div>
      </div>

      {/* Primary Sensor Telemetry Bento Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Spindle Speed */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold tracking-wider text-industrial-300 flex items-center gap-1.5">
              <Gauge size={14} className="text-industrial-accent" />
              SPINDLE SPEED
            </span>
            <span className="text-[10px] font-mono text-industrial-400 uppercase">RPM</span>
          </div>
          <div className="text-3xl font-mono font-bold text-white mt-3">
            {liveData?.spindleSpeedRpm ? Math.round(liveData.spindleSpeedRpm).toLocaleString() : '0'}
          </div>
          <div className="text-[10px] font-mono text-industrial-400 mt-1">Rated: 0 — 15,000 RPM</div>
          <div className="w-full bg-industrial-900 h-2 mt-3 rounded-none overflow-hidden border border-substrate-border">
            <div
              className="bg-cyan-500 h-full transition-all duration-500"
              style={{ width: `${Math.min(100, ((liveData?.spindleSpeedRpm || 0) / 15000) * 100)}%` }}
            />
          </div>
        </div>

        {/* Spindle Vibration Severity */}
        <div className={`bg-substrate-card border p-4 relative overflow-hidden ${vibLimitExceeded ? 'border-rose-500/70' : 'border-substrate-border'}`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold tracking-wider text-industrial-300 flex items-center gap-1.5">
              <Activity size={14} className={vibLimitExceeded ? 'text-rose-400 animate-bounce' : 'text-emerald-400'} />
              VIBRATION RMS
            </span>
            <span className="text-[10px] font-mono text-industrial-400 uppercase">ISO 10816</span>
          </div>
          <div className={`text-3xl font-mono font-bold mt-3 ${vibLimitExceeded ? 'text-rose-400' : 'text-white'}`}>
            {liveData?.vibrationMmPerSec ? liveData.vibrationMmPerSec.toFixed(2) : '0.00'}
            <span className="text-xs text-industrial-400 ml-1">mm/s</span>
          </div>
          <div className="text-[10px] font-mono text-industrial-400 mt-1">
            {vibLimitExceeded ? (
              <span className="text-rose-400 font-bold flex items-center gap-1">
                <AlertTriangle size={10} /> ZONE D: UNACCEPTABLE (&gt; 4.5)
              </span>
            ) : (
              <span className="text-emerald-400">ZONE A/B: NORMAL (&le; 2.8)</span>
            )}
          </div>
          <div className="w-full bg-industrial-900 h-2 mt-3 rounded-none overflow-hidden border border-substrate-border">
            <div
              className={`h-full transition-all duration-500 ${vibLimitExceeded ? 'bg-rose-500' : 'bg-emerald-500'}`}
              style={{ width: `${Math.min(100, ((liveData?.vibrationMmPerSec || 0) / 7.0) * 100)}%` }}
            />
          </div>
        </div>

        {/* Motor Current */}
        <div className="bg-substrate-card border border-substrate-border p-4 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold tracking-wider text-industrial-300 flex items-center gap-1.5">
              <Zap size={14} className="text-amber-400" />
              MOTOR CURRENT
            </span>
            <span className="text-[10px] font-mono text-industrial-400 uppercase">AMPERES</span>
          </div>
          <div className="text-3xl font-mono font-bold text-white mt-3">
            {liveData?.motorCurrentAmps ? liveData.motorCurrentAmps.toFixed(1) : '0.0'}
            <span className="text-xs text-industrial-400 ml-1">A</span>
          </div>
          <div className="text-[10px] font-mono text-industrial-400 mt-1">FLA Rating: 45.0 A Max</div>
          <div className="w-full bg-industrial-900 h-2 mt-3 rounded-none overflow-hidden border border-substrate-border">
            <div
              className="bg-amber-400 h-full transition-all duration-500"
              style={{ width: `${Math.min(100, ((liveData?.motorCurrentAmps || 0) / 50.0) * 100)}%` }}
            />
          </div>
        </div>

        {/* Bearing Temperature */}
        <div className={`bg-substrate-card border p-4 relative overflow-hidden ${tempLimitExceeded ? 'border-rose-500/70' : 'border-substrate-border'}`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-mono font-bold tracking-wider text-industrial-300 flex items-center gap-1.5">
              <Thermometer size={14} className={tempLimitExceeded ? 'text-rose-400' : 'text-orange-400'} />
              BEARING TEMP
            </span>
            <span className="text-[10px] font-mono text-industrial-400 uppercase">CELSIUS</span>
          </div>
          <div className={`text-3xl font-mono font-bold mt-3 ${tempLimitExceeded ? 'text-rose-400' : 'text-white'}`}>
            {liveData?.bearingTempCelsius ? liveData.bearingTempCelsius.toFixed(1) : '24.0'}
            <span className="text-xs text-industrial-400 ml-1">°C</span>
          </div>
          <div className="text-[10px] font-mono text-industrial-400 mt-1">
            {tempLimitExceeded ? (
              <span className="text-rose-400 font-bold flex items-center gap-1">
                <AlertTriangle size={10} /> THERMAL BREACH (&gt; 80°C)
              </span>
            ) : (
              <span className="text-industrial-300">Target Envelope: 20 — 65°C</span>
            )}
          </div>
          <div className="w-full bg-industrial-900 h-2 mt-3 rounded-none overflow-hidden border border-substrate-border">
            <div
              className={`h-full transition-all duration-500 ${tempLimitExceeded ? 'bg-rose-500' : 'bg-orange-500'}`}
              style={{ width: `${Math.min(100, ((liveData?.bearingTempCelsius || 24) / 100.0) * 100)}%` }}
            />
          </div>
        </div>
      </div>

      {/* E4-S2: High-Frequency Time-Series Downsampling & Historical Analytics Section */}
      <div className="bg-substrate-card border border-substrate-border p-5">
        {/* Section Header */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b border-substrate-border pb-4">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <BarChart3 size={18} className="text-cyan-400" />
              <h2 className="text-sm font-mono font-bold uppercase tracking-wider text-white">
                TIME-SERIES DOWNSAMPLING ENGINE // HISTORICAL ANALYTICS
              </h2>
            </div>
            
            {/* Resolution Tag */}
            <span className="px-2 py-0.5 text-[10px] font-mono font-bold bg-cyan-950/40 text-cyan-400 border border-cyan-500/40">
              RESOLUTION: {seriesData?.bucketResolution ? `${seriesData.bucketResolution.toUpperCase()} ROLLUP` : 'AUTO'}
            </span>

            {/* SLA Compliance Badge */}
            <div className="flex items-center gap-1.5 px-2.5 py-0.5 text-[10px] font-mono border border-substrate-border bg-industrial-900">
              <span className={`w-1.5 h-1.5 rounded-full ${queryDurationMs !== null && queryDurationMs < 200 ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
              <span className="text-industrial-400">QUERY SLA:</span>
              <span className={`font-bold ${queryDurationMs !== null && queryDurationMs < 200 ? 'text-emerald-400' : 'text-amber-400'}`}>
                {queryDurationMs !== null ? `${queryDurationMs}ms` : '—'}
              </span>
              <span className="text-industrial-500">
                (TARGET &lt;200ms {queryDurationMs !== null && queryDurationMs < 200 ? '✓ PASS' : ''})
              </span>
            </div>
          </div>

          {/* Controls: Tag Selector & Time Range Selector */}
          <div className="flex flex-wrap items-center gap-3">
            {/* Tag selector */}
            <div className="flex items-center gap-2 bg-industrial-900 border border-substrate-border px-2.5 py-1">
              <Sliders size={12} className="text-industrial-400" />
              <span className="text-[10px] font-mono font-bold text-industrial-300">TAG:</span>
              <select
                value={selectedTag}
                onChange={(e) => setSelectedTag(e.target.value)}
                className="bg-transparent text-xs font-mono font-bold text-white border-none focus:outline-none cursor-pointer"
              >
                {availableTags.map((tag) => (
                  <option key={tag} value={tag} className="bg-industrial-900 text-white">
                    {tag}
                  </option>
                ))}
              </select>
            </div>

            {/* Time Range Selector */}
            <div className="inline-flex border border-substrate-border bg-industrial-900 text-xs font-mono">
              {(['1h', '24h', '7d', '30d'] as const).map((range) => (
                <button
                  key={range}
                  onClick={() => setTimeRange(range)}
                  className={`px-2.5 py-1 uppercase font-bold transition-colors ${
                    timeRange === range
                      ? 'bg-cyan-950 text-cyan-300 border-b-2 border-cyan-400'
                      : 'text-industrial-400 hover:text-white'
                  }`}
                >
                  {range === '1h' ? '1H (1s RAW)' : range === '24h' ? '24H (1m)' : range === '7d' ? '7D (1m)' : '30D (1h)'}
                </button>
              ))}
            </div>

            {/* Refresh Series Button */}
            <button
              onClick={() => fetchTimeSeries(selectedMachineId, selectedTag, timeRange)}
              disabled={isSeriesLoading}
              className="p-1.5 bg-industrial-800 hover:bg-industrial-700 text-industrial-300 border border-substrate-border"
              title="Refresh Time-Series"
            >
              <RefreshCw size={14} className={isSeriesLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>

        {/* Aggregated KPI Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 py-3 border-b border-substrate-border text-xs font-mono">
          <div className="bg-industrial-900/60 p-2.5 border border-substrate-border">
            <span className="text-[10px] text-industrial-400 block uppercase">LATEST MEAN</span>
            <span className="text-lg font-bold text-white">{seriesStats.latest.toFixed(2)}</span>
          </div>
          <div className="bg-industrial-900/60 p-2.5 border border-substrate-border">
            <span className="text-[10px] text-industrial-400 block uppercase">INTERVAL MIN</span>
            <span className="text-lg font-bold text-cyan-400">{seriesStats.min.toFixed(2)}</span>
          </div>
          <div className="bg-industrial-900/60 p-2.5 border border-substrate-border">
            <span className="text-[10px] text-industrial-400 block uppercase">INTERVAL PEAK MAX</span>
            <span className="text-lg font-bold text-rose-400">{seriesStats.max.toFixed(2)}</span>
          </div>
          <div className="bg-industrial-900/60 p-2.5 border border-substrate-border">
            <span className="text-[10px] text-industrial-400 block uppercase">PERIOD AVERAGE</span>
            <span className="text-lg font-bold text-white">{seriesStats.avg.toFixed(2)}</span>
          </div>
          <div className="bg-industrial-900/60 p-2.5 border border-substrate-border col-span-2 sm:col-span-1">
            <span className="text-[10px] text-industrial-400 block uppercase">BUCKET COUNT</span>
            <span className="text-lg font-bold text-emerald-400">{seriesStats.count} BUCKETS</span>
          </div>
        </div>

        {/* SVG Time-Series Chart Container */}
        <div className="relative mt-4 bg-industrial-950 border border-substrate-border p-3 overflow-hidden">
          {isSeriesLoading && (
            <div className="absolute inset-0 bg-black/60 backdrop-blur-[1px] flex items-center justify-center z-10 font-mono text-xs text-cyan-400">
              <RefreshCw className="animate-spin mr-2" size={16} />
              FETCHING DOWNSAMPLED SERIES AGGREGATION...
            </div>
          )}

          {plotPoints.length === 0 ? (
            <div className="h-48 flex flex-col items-center justify-center text-center font-mono p-6">
              <Info size={24} className="text-industrial-500 mb-2" />
              <p className="text-xs text-industrial-400">NO TELEMETRY ROLLUP POINTS IN THE SELECTED INTERVAL FOR THIS TAG.</p>
              <p className="text-[11px] text-industrial-500 mt-1">Click "SIMULATE EDGE PULSE" to ingest high-frequency PLC records.</p>
            </div>
          ) : (
            <div className="relative">
              <svg
                viewBox={`0 0 ${svgWidth} ${svgHeight}`}
                className="w-full h-56 cursor-crosshair select-none"
                onMouseLeave={() => setHoveredBucket(null)}
                onMouseMove={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect();
                  const mouseX = ((e.clientX - rect.left) / rect.width) * svgWidth;
                  if (mouseX >= padLeft && mouseX <= svgWidth - padRight && plotPoints.length > 0) {
                    const relativeRatio = (mouseX - padLeft) / plotW;
                    const closestIndex = Math.min(
                      plotPoints.length - 1,
                      Math.max(0, Math.round(relativeRatio * (plotPoints.length - 1)))
                    );
                    setHoveredBucket(plotPoints[closestIndex].bucket);
                  }
                }}
              >
                {/* Horizontal Reference Grid Lines */}
                {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                  const y = padTop + plotH * (1 - ratio);
                  const val = yDomainMin + yDomainSpan * ratio;
                  return (
                    <g key={ratio}>
                      <line
                        x1={padLeft}
                        y1={y}
                        x2={svgWidth - padRight}
                        y2={y}
                        stroke="#262626"
                        strokeDasharray="2 2"
                      />
                      <text
                        x={padLeft - 8}
                        y={y + 3}
                        fill="#737373"
                        fontSize="9"
                        fontFamily="monospace"
                        textAnchor="end"
                      >
                        {val.toFixed(1)}
                      </text>
                    </g>
                  );
                })}

                {/* Min-to-Max Shaded Envelope */}
                {envelopePath && (
                  <path
                    d={envelopePath}
                    fill="rgba(6, 182, 212, 0.12)"
                    stroke="none"
                  />
                )}

                {/* Peak Max Envelope Border */}
                {plotPoints.length > 1 && (
                  <path
                    d={`M ${plotPoints.map((p: PlotPoint) => `${p.x.toFixed(1)},${p.yMax.toFixed(1)}`).join(' L ')}`}
                    fill="none"
                    stroke="rgba(244, 63, 94, 0.4)"
                    strokeWidth="1"
                    strokeDasharray="3 3"
                  />
                )}

                {/* Trough Min Envelope Border */}
                {plotPoints.length > 1 && (
                  <path
                    d={`M ${plotPoints.map((p: PlotPoint) => `${p.x.toFixed(1)},${p.yMin.toFixed(1)}`).join(' L ')}`}
                    fill="none"
                    stroke="rgba(14, 165, 233, 0.4)"
                    strokeWidth="1"
                    strokeDasharray="3 3"
                  />
                )}

                {/* Mean / Average Continuous Downsampled Trendline */}
                {avgPath && (
                  <path
                    d={avgPath}
                    fill="none"
                    stroke="#06b6d4"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                )}

                {/* Vertical Cursor Crosshair on Hover */}
                {hoveredBucket && (
                  (() => {
                    const idx = buckets.indexOf(hoveredBucket);
                    if (idx < 0) return null;
                    const pt = plotPoints[idx];
                    return (
                      <g>
                        <line
                          x1={pt.x}
                          y1={padTop}
                          x2={pt.x}
                          y2={padTop + plotH}
                          stroke="#f59e0b"
                          strokeWidth="1"
                          strokeDasharray="3 3"
                        />
                        {/* Peak Point Dot */}
                        <circle cx={pt.x} cy={pt.yMax} r="3" fill="#f43f5e" />
                        {/* Avg Point Dot */}
                        <circle cx={pt.x} cy={pt.yAvg} r="4" fill="#06b6d4" stroke="#ffffff" strokeWidth="1" />
                        {/* Min Point Dot */}
                        <circle cx={pt.x} cy={pt.yMin} r="3" fill="#0ea5e9" />
                      </g>
                    );
                  })()
                )}

                {/* X-Axis Horizontal Border */}
                <line
                  x1={padLeft}
                  y1={padTop + plotH}
                  x2={svgWidth - padRight}
                  y2={padTop + plotH}
                  stroke="#525252"
                  strokeWidth="1"
                />

                {/* X-Axis Time Labels */}
                {buckets.length > 0 && (
                  <>
                    <text
                      x={padLeft}
                      y={svgHeight - 10}
                      fill="#737373"
                      fontSize="9"
                      fontFamily="monospace"
                      textAnchor="start"
                    >
                      {new Date(buckets[0].bucket).toLocaleDateString([], { month: 'numeric', day: 'numeric' })}{' '}
                      {new Date(buckets[0].bucket).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </text>
                    {buckets.length > 2 && (
                      <text
                        x={padLeft + plotW / 2}
                        y={svgHeight - 10}
                        fill="#737373"
                        fontSize="9"
                        fontFamily="monospace"
                        textAnchor="middle"
                      >
                        {new Date(buckets[Math.floor(buckets.length / 2)].bucket).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </text>
                    )}
                    <text
                      x={svgWidth - padRight}
                      y={svgHeight - 10}
                      fill="#737373"
                      fontSize="9"
                      fontFamily="monospace"
                      textAnchor="end"
                    >
                      {new Date(buckets[buckets.length - 1].bucket).toLocaleDateString([], { month: 'numeric', day: 'numeric' })}{' '}
                      {new Date(buckets[buckets.length - 1].bucket).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </text>
                  </>
                )}
              </svg>

              {/* Hover Tooltip Box */}
              {hoveredBucket && (
                <div className="absolute top-2 right-4 bg-industrial-900/95 border border-cyan-500/60 p-2 text-xs font-mono text-white pointer-events-none shadow-lg z-20">
                  <div className="text-[10px] text-cyan-400 font-bold border-b border-substrate-border pb-1 mb-1">
                    BUCKET: {new Date(hoveredBucket.bucket).toLocaleString()}
                  </div>
                  <div className="grid grid-cols-2 gap-x-3 gap-y-0.5 text-[11px]">
                    <span className="text-industrial-400">MEAN AVG:</span>
                    <span className="font-bold text-white text-right">{hoveredBucket.avg.toFixed(2)}</span>
                    <span className="text-rose-400">MAX PEAK:</span>
                    <span className="font-bold text-rose-300 text-right">{hoveredBucket.max.toFixed(2)}</span>
                    <span className="text-cyan-400">MIN TROUGH:</span>
                    <span className="font-bold text-cyan-300 text-right">{hoveredBucket.min.toFixed(2)}</span>
                    <span className="text-industrial-400">SAMPLES:</span>
                    <span className="font-bold text-emerald-400 text-right">{hoveredBucket.count}</span>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Chart Subtitle & Legend */}
          <div className="flex flex-wrap items-center justify-between gap-4 mt-3 pt-2 border-t border-substrate-border text-[10px] font-mono text-industrial-400">
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-0.5 bg-cyan-400 inline-block" />
                <span>MEAN AVERAGE</span>
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-2 bg-cyan-500/20 border border-cyan-400/40 inline-block" />
                <span>MIN-MAX DISPERSION ENVELOPE</span>
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2 h-0.5 border-t border-dashed border-rose-400 inline-block" />
                <span>MAX CEILING</span>
              </span>
            </div>
            <div className="text-industrial-500">
              TimescaleDB Storage Adapter // Continuous 1m & 1h Hypertable Rollups
            </div>
          </div>
        </div>
      </div>

      {/* Two-Column Section: Configured PLC Tags & Stream Ingestion Ticker */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Tag Mappings Table */}
        <div className="lg:col-span-2 bg-substrate-card border border-substrate-border p-4">
          <div className="flex items-center justify-between mb-4 border-b border-substrate-border pb-2">
            <div className="flex items-center gap-2">
              <Sliders size={16} className="text-industrial-accent" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-white">
                CONFIGURED PLC TAG MAPPINGS // {selectedMachine?.name}
              </h2>
            </div>
            {canManageTags && (
              <button
                onClick={() => setIsTagModalOpen(true)}
                className="text-[11px] font-mono text-industrial-accent hover:underline flex items-center gap-1"
              >
                <Plus size={12} /> ADD TAG
              </button>
            )}
          </div>

          {tagMappings.length === 0 ? (
            <div className="p-8 text-center border border-dashed border-substrate-border">
              <p className="text-xs font-mono text-industrial-400">
                NO INDUSTRIAL TAGS CONFIGURED FOR THIS ASSET YET.
              </p>
              {canManageTags && (
                <IndustrialButton
                  variant="primary"
                  size="sm"
                  className="mt-3"
                  onClick={() => setIsTagModalOpen(true)}
                >
                  <Plus size={14} className="mr-1" />
                  MAP FIRST CONTROLLER TAG
                </IndustrialButton>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs font-mono">
                <thead>
                  <tr className="border-b border-substrate-border text-[10px] text-industrial-400 uppercase tracking-wider">
                    <th className="py-2 px-3">Tag Name</th>
                    <th className="py-2 px-3">Protocol</th>
                    <th className="py-2 px-3">Controller Address / Node ID</th>
                    <th className="py-2 px-3">Unit</th>
                    <th className="py-2 px-3">Scale</th>
                    {canManageTags && <th className="py-2 px-3 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-substrate-border">
                  {tagMappings.map((tag) => (
                    <tr key={tag.id} className="hover:bg-industrial-900/40">
                      <td className="py-2.5 px-3 font-bold text-white flex items-center gap-1.5">
                        <CheckCircle2 size={12} className="text-emerald-400" />
                        {tag.tagName}
                      </td>
                      <td className="py-2.5 px-3">
                        <span className="px-1.5 py-0.5 text-[10px] bg-industrial-800 text-industrial-300 border border-substrate-border font-bold">
                          {tag.protocol}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 text-industrial-300 font-mono text-[11px] truncate max-w-xs">
                        {tag.tagAddress}
                      </td>
                      <td className="py-2.5 px-3 text-industrial-400">{tag.unitOfMeasure || '—'}</td>
                      <td className="py-2.5 px-3 text-industrial-400">{tag.scaleFactor}x</td>
                      {canManageTags && (
                        <td className="py-2.5 px-3 text-right">
                          <button
                            onClick={() => handleDeleteTag(tag.id, tag.tagName)}
                            className="p-1 text-industrial-400 hover:text-rose-400 transition-colors"
                            title="Remove mapping"
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Right: Real-time Ingestion Stream Activity */}
        <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col h-full">
          <div className="flex items-center justify-between mb-3 border-b border-substrate-border pb-2">
            <div className="flex items-center gap-2">
              <Radio size={14} className="text-emerald-400 animate-pulse" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-white">
                INGESTION PULSE LOG
              </h2>
            </div>
            <span className="text-[10px] font-mono text-industrial-400">EDGE BUFFER</span>
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1 max-h-[320px]">
            {activityLog.length === 0 ? (
              <div className="text-[11px] font-mono text-industrial-500 italic p-4 text-center">
                Waiting for incoming edge packets or manual test pulse...
              </div>
            ) : (
              activityLog.map((entry) => (
                <div
                  key={entry.id}
                  className={`p-2 text-[11px] font-mono border ${
                    entry.type === 'alert'
                      ? 'bg-rose-950/30 border-rose-500/50 text-rose-300'
                      : entry.type === 'success'
                      ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-300'
                      : 'bg-industrial-900 border-substrate-border text-industrial-300'
                  }`}
                >
                  <div className="flex items-center justify-between text-[10px] opacity-75 mb-0.5">
                    <span>{entry.time}</span>
                    <span className="uppercase font-bold">{entry.type}</span>
                  </div>
                  <div className="break-words">{entry.msg}</div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Modal for Tag Mapping Registration */}
      <Modal
        isOpen={isTagModalOpen}
        onClose={() => setIsTagModalOpen(false)}
        title="REGISTER INDUSTRIAL PLC TAG MAPPING"
      >
        <form onSubmit={handleCreateTag} className="space-y-4">
          {tagError && (
            <div className="p-3 bg-rose-950/40 border border-rose-500/50 text-rose-400 text-xs font-mono">
              {tagError}
            </div>
          )}

          <div>
            <label className="block text-xs font-mono font-bold text-industrial-300 mb-1">
              CANONICAL TAG NAME *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. SPINDLE_SPEED, VIBRATION_RMS, HYDRAULIC_PRESSURE"
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white focus:border-industrial-accent outline-none uppercase"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-mono font-bold text-industrial-300 mb-1">
                PROTOCOL *
              </label>
              <select
                value={newProtocol}
                onChange={(e) => setNewProtocol(e.target.value as ProtocolType)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white focus:border-industrial-accent outline-none"
              >
                <option value="OPC_UA">OPC-UA Binary</option>
                <option value="MQTT_SPARKPLUG_B">MQTT Sparkplug B</option>
                <option value="MODBUS_TCP">Modbus TCP</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono font-bold text-industrial-300 mb-1">
                UNIT OF MEASURE
              </label>
              <input
                type="text"
                placeholder="e.g. RPM, mm/s, A, °C, bar"
                value={newUnit}
                onChange={(e) => setNewUnit(e.target.value)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white focus:border-industrial-accent outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-mono font-bold text-industrial-300 mb-1">
              CONTROLLER NODE ID / ADDRESS / TOPIC *
            </label>
            <input
              type="text"
              required
              placeholder="ns=2;s=Device1.SpindleSpeed or spBv1.0/Plant1/DDATA/..."
              value={newTagAddress}
              onChange={(e) => setNewTagAddress(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white focus:border-industrial-accent outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-mono font-bold text-industrial-300 mb-1">
              CALIBRATION SCALE FACTOR
            </label>
            <input
              type="number"
              step="0.0001"
              value={newScaleFactor}
              onChange={(e) => setNewScaleFactor(parseFloat(e.target.value))}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-xs font-mono text-white focus:border-industrial-accent outline-none"
            />
            <span className="text-[10px] font-mono text-industrial-400 mt-1 block">
              Multiplied against raw PLC integer/analog register value during ingestion.
            </span>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="secondary"
              onClick={() => setIsTagModalOpen(false)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              disabled={isSubmittingTag}
            >
              {isSubmittingTag ? 'COMMITTING TO REGISTRY...' : 'SAVE TAG MAPPING'}
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Modal for Telemetry Retention Policy Management */}
      <Modal
        isOpen={isRetentionModalOpen}
        onClose={() => setIsRetentionModalOpen(false)}
        title="STORAGE RETENTION POLICIES & CONTINUOUS ROLLUPS"
      >
        <div className="space-y-4">
          <p className="text-xs font-mono text-industrial-300">
            FoundryOS implements a multi-tier storage policy to guarantee sub-200ms query performance across 30+ day analytical windows.
          </p>

          <div className="border border-substrate-border divide-y divide-substrate-border text-xs font-mono">
            <div className="p-3 bg-industrial-900 flex items-center justify-between">
              <div>
                <span className="font-bold text-white block">TIER 1: RAW HIGH-FREQUENCY (1s)</span>
                <span className="text-[10px] text-industrial-400">Micro-second vibration & spindle diagnostic points</span>
              </div>
              <span className="px-2 py-0.5 bg-emerald-950 text-emerald-400 border border-emerald-500/40 text-[10px] font-bold">
                7 DAYS RETENTION
              </span>
            </div>

            <div className="p-3 bg-industrial-900 flex items-center justify-between">
              <div>
                <span className="font-bold text-white block">TIER 2: 1-MINUTE ROLLUPS (1m)</span>
                <span className="text-[10px] text-industrial-400">Aggregated min, max, avg, and sample counts for shift analysis</span>
              </div>
              <span className="px-2 py-0.5 bg-cyan-950 text-cyan-400 border border-cyan-500/40 text-[10px] font-bold">
                30 DAYS RETENTION
              </span>
            </div>

            <div className="p-3 bg-industrial-900 flex items-center justify-between">
              <div>
                <span className="font-bold text-white block">TIER 3: 1-HOUR ROLLUPS (1h)</span>
                <span className="text-[10px] text-industrial-400">Long-term trending and multi-month operational compliance</span>
              </div>
              <span className="px-2 py-0.5 bg-amber-950 text-amber-400 border border-amber-500/40 text-[10px] font-bold">
                365 DAYS RETENTION
              </span>
            </div>
          </div>

          {retentionMessage && (
            <div className="p-3 bg-industrial-900 border border-cyan-500/50 text-cyan-300 text-xs font-mono">
              <span className="font-bold block mb-1">STATUS:</span>
              {retentionMessage}
            </div>
          )}

          {retentionReport && (
            <div className="p-3 bg-industrial-950 border border-substrate-border text-[11px] font-mono grid grid-cols-2 gap-2">
              <div>Raw Points Pruned: <strong className="text-white">{retentionReport.rawPointsPruned}</strong></div>
              <div>1m Rollups Pruned: <strong className="text-white">{retentionReport.rollups1mPruned}</strong></div>
              <div>1h Rollups Pruned: <strong className="text-white">{retentionReport.rollups1hPruned}</strong></div>
              <div>Execution Latency: <strong className="text-emerald-400">{retentionReport.executionTimeMs} ms</strong></div>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="secondary"
              onClick={() => setIsRetentionModalOpen(false)}
            >
              CLOSE
            </IndustrialButton>
            <IndustrialButton
              type="button"
              variant="primary"
              onClick={handleExecuteRetention}
              disabled={isExecutingRetention}
            >
              {isExecutingRetention ? (
                <>
                  <RefreshCw className="animate-spin mr-1.5" size={14} />
                  PRUNING PARTITIONS...
                </>
              ) : (
                <>
                  <Archive size={14} className="mr-1.5" />
                  EXECUTE PRUNING NOW
                </>
              )}
            </IndustrialButton>
          </div>
        </div>
      </Modal>
    </div>
  );
};
