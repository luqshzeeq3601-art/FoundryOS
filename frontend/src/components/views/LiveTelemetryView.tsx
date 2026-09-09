import React, { useState, useEffect, useCallback } from 'react';
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
  Sliders
} from 'lucide-react';
import { telemetryApi, api } from '../../services/api-client';
import { 
  MachineDto, 
  MachineLiveTelemetry, 
  TagMapping, 
  CreateTagMappingRequest, 
  ProtocolType 
} from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { StatusBeacon } from '../common/StatusBeacon';
import { Modal } from '../common/Modal';

export const LiveTelemetryView: React.FC = () => {
  const { hasRole } = useAuth();
  const canManageTags = hasRole('ADMIN', 'ENGINEER');
  const canIngest = hasRole('ADMIN', 'ENGINEER');

  const [machines, setMachines] = useState<MachineDto[]>([]);
  const [selectedMachineId, setSelectedMachineId] = useState<string>('');
  const [liveData, setLiveData] = useState<MachineLiveTelemetry | null>(null);
  const [tagMappings, setTagMappings] = useState<TagMapping[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [autoPoll, setAutoPoll] = useState<boolean>(true);
  const [activityLog, setActivityLog] = useState<Array<{ id: string; time: string; msg: string; type: 'info' | 'alert' | 'success' }>>([]);

  // Modal for adding tag mapping
  const [isTagModalOpen, setIsTagModalOpen] = useState<boolean>(false);
  const [newTagName, setNewTagName] = useState<string>('');
  const [newProtocol, setNewProtocol] = useState<ProtocolType>('OPC_UA');
  const [newTagAddress, setNewTagAddress] = useState<string>('');
  const [newUnit, setNewUnit] = useState<string>('RPM');
  const [newScaleFactor, setNewScaleFactor] = useState<number>(1.0);
  const [isSubmittingTag, setIsSubmittingTag] = useState<boolean>(false);
  const [tagError, setTagError] = useState<string | null>(null);

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

  // Effect to load when selected machine changes
  useEffect(() => {
    if (selectedMachineId) {
      fetchTelemetry(selectedMachineId, true);
    }
  }, [selectedMachineId, fetchTelemetry]);

  // Periodic auto-polling
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

    // Generate realistic fluctuating PLC readings
    const simulatedSpeed = Math.floor(7500 + Math.random() * 3000);
    const simulatedVib = +(1.2 + Math.random() * 3.8).toFixed(2); // occasionally exceeds 4.5
    const simulatedCurrent = +(18.0 + Math.random() * 15.0).toFixed(1);
    const simulatedTemp = +(45.0 + Math.random() * 38.0).toFixed(1); // occasionally exceeds 80

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
    } catch (err) {
      console.error('Simulation failed:', err);
      addLogEntry('Edge telemetry packet failed to ingest', 'alert');
    } finally {
      setIsRefreshing(false);
    }
  };

  const addLogEntry = (msg: string, type: 'info' | 'alert' | 'success') => {
    const time = new Date().toLocaleTimeString();
    setActivityLog((prev) => [{ id: Math.random().toString(), time, msg, type }, ...prev.slice(0, 19)]);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-12 text-industrial-400 font-mono">
        <RefreshCw className="animate-spin mr-3" size={20} />
        INITIALIZING IIoT PROTOCOL INTERFACE...
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
            Real-time high-frequency PLC controller stream ingestion (OPC-UA, MQTT Sparkplug B, Modbus TCP)
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
          {/* Progress gauge bar */}
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
          {/* Gauge bar */}
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
          {/* Gauge bar */}
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
          {/* Gauge bar */}
          <div className="w-full bg-industrial-900 h-2 mt-3 rounded-none overflow-hidden border border-substrate-border">
            <div
              className={`h-full transition-all duration-500 ${tempLimitExceeded ? 'bg-rose-500' : 'bg-orange-500'}`}
              style={{ width: `${Math.min(100, ((liveData?.bearingTempCelsius || 24) / 100.0) * 100)}%` }}
            />
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
    </div>
  );
};
