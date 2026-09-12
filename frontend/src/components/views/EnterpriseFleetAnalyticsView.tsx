import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { 
  BarChart3, 
  Download, 
  FileText, 
  RefreshCw, 
  ChevronDown, 
  ChevronUp, 
  AlertTriangle, 
  CheckCircle2, 
  TrendingUp, 
  TrendingDown, 
  Layers, 
  Cpu, 
  Factory, 
  Clock, 
  Search, 
  Send
} from 'lucide-react';
import { enterpriseAnalyticsApi } from '../../services/api-client';
import { EnterpriseOeeMatrixDto } from '../../types';

export const EnterpriseFleetAnalyticsView: React.FC = () => {
  const [selectedInterval, setSelectedInterval] = useState<string>('24H');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [expandedPlantIds, setExpandedPlantIds] = useState<Set<string>>(new Set());
  const [exportLoading, setExportLoading] = useState<'csv' | 'pdf' | null>(null);
  const [reportSuccessMsg, setReportSuccessMsg] = useState<string | null>(null);

  const { data: matrix, isLoading, refetch, isFetching } = useQuery<EnterpriseOeeMatrixDto>({
    queryKey: ['enterprise-oee-matrix', selectedInterval, statusFilter],
    queryFn: () => enterpriseAnalyticsApi.getOeeMatrix({
      interval: selectedInterval,
      status: statusFilter !== 'ALL' ? statusFilter : undefined,
    }),
    refetchInterval: 30000,
  });

  const scheduledReportMutation = useMutation({
    mutationFn: (format: 'CSV' | 'PDF') => enterpriseAnalyticsApi.triggerScheduledReport(format, selectedInterval),
    onSuccess: (data) => {
      setReportSuccessMsg(`REPORT DISPATCHED: ${data.message} (ID: ${data.reportId.slice(0, 8)}, Size: ${data.fileSizeBytes} B)`);
      setTimeout(() => setReportSuccessMsg(null), 6000);
    },
    onError: () => {
      setReportSuccessMsg('ERROR: Failed to dispatch executive report.');
      setTimeout(() => setReportSuccessMsg(null), 5000);
    },
  });

  const toggleExpand = (plantId: string) => {
    setExpandedPlantIds(prev => {
      const next = new Set(prev);
      if (next.has(plantId)) {
        next.delete(plantId);
      } else {
        next.add(plantId);
      }
      return next;
    });
  };

  const expandAll = () => {
    if (!matrix?.plantMetrics) return;
    setExpandedPlantIds(new Set(matrix.plantMetrics.map(p => p.plantId)));
  };

  const collapseAll = () => {
    setExpandedPlantIds(new Set());
  };

  const handleExport = async (format: 'csv' | 'pdf') => {
    try {
      setExportLoading(format);
      await enterpriseAnalyticsApi.downloadExport(format, selectedInterval);
    } catch (err) {
      console.error(`Export ${format} error:`, err);
    } finally {
      setExportLoading(null);
    }
  };

  const filteredPlants = (matrix?.plantMetrics || []).filter(plant => {
    if (searchQuery.trim() === '') return true;
    const query = searchQuery.toLowerCase();
    const plantMatch = plant.plantCode.toLowerCase().includes(query) || plant.plantName.toLowerCase().includes(query);
    const lineMatch = plant.lineMetrics?.some(l => 
      l.lineCode.toLowerCase().includes(query) || l.lineName.toLowerCase().includes(query) || (l.areaName && l.areaName.toLowerCase().includes(query))
    );
    return plantMatch || lineMatch;
  });

  const fleet = matrix?.fleetSummary;

  return (
    <div className="space-y-6">
      {/* Top Tactical Header */}
      <div className="border border-substrate-border bg-substrate-card p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-block w-2.5 h-2.5 bg-hazard-green animate-pulse" />
            <span className="text-[10px] font-mono tracking-widest text-hazard-green uppercase font-bold">
              ENTERPRISE FEDERATION // FLEET TELEMETRY BUS ACTIVE
            </span>
          </div>
          <h1 className="text-xl sm:text-2xl font-mono font-black tracking-tight text-white uppercase mt-1">
            [ FLEET BENCHMARK & CROSS-PLANT MATRIX ]
          </h1>
          <p className="text-xs font-mono text-industrial-400 mt-0.5">
            Enterprise: <span className="text-white font-bold">{matrix?.enterpriseName || 'GLOBAL'}</span> ({matrix?.enterpriseCode || 'ENT-GLOBAL'})
          </p>
        </div>

        {/* Interval & Refresh Controls */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex border border-substrate-border bg-substrate-dark p-1">
            {(['24H', '7D', '30D', 'ALL'] as const).map(interval => (
              <button
                key={interval}
                onClick={() => setSelectedInterval(interval)}
                className={`px-3 py-1.5 text-xs font-mono font-bold tracking-wider transition-all select-none ${
                  selectedInterval === interval
                    ? 'bg-industrial-700 text-white shadow-sm'
                    : 'text-industrial-400 hover:text-industrial-200'
                }`}
              >
                {interval}
              </button>
            ))}
          </div>

          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-1.5 px-3 py-2 border border-substrate-border bg-substrate-dark hover:bg-industrial-800 text-industrial-200 text-xs font-mono font-bold transition-all disabled:opacity-50"
            title="Refresh Fleet Data"
          >
            <RefreshCw size={14} className={isFetching ? 'animate-spin text-hazard-red' : ''} />
            <span className="hidden sm:inline">REFRESH</span>
          </button>
        </div>
      </div>

      {reportSuccessMsg && (
        <div className="border-l-4 border-hazard-green bg-industrial-900/90 border border-substrate-border p-3 text-xs font-mono text-hazard-green flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{reportSuccessMsg}</span>
          </div>
          <button onClick={() => setReportSuccessMsg(null)} className="text-industrial-400 hover:text-white font-bold">✕</button>
        </div>
      )}

      {/* Fleet KPI Executive Summary Tiles */}
      {fleet && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-2">
          {/* Tile 1: Fleet Avg OEE */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between relative overflow-hidden">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>FLEET AVG OEE</span>
              <BarChart3 size={14} className="text-hazard-green" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.fleetAvgOee.toFixed(1)}%
              </div>
              <div className="text-[10px] font-mono mt-0.5">
                {fleet.fleetAvgOee >= 85 ? (
                  <span className="text-hazard-green font-bold">[ WORLD CLASS ]</span>
                ) : fleet.fleetAvgOee >= 70 ? (
                  <span className="text-amber-400 font-bold">[ TARGET RANGE ]</span>
                ) : (
                  <span className="text-hazard-red font-bold">[ UNDERPERFORMING ]</span>
                )}
              </div>
            </div>
            <div className="w-full bg-industrial-900 h-1.5 border border-substrate-border overflow-hidden">
              <div
                className={`h-full ${fleet.fleetAvgOee >= 85 ? 'bg-hazard-green' : fleet.fleetAvgOee >= 70 ? 'bg-amber-400' : 'bg-hazard-red'}`}
                style={{ width: `${Math.min(100, fleet.fleetAvgOee)}%` }}
              />
            </div>
          </div>

          {/* Tile 2: Fleet Availability */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>AVAILABILITY</span>
              <Clock size={14} className="text-industrial-300" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.fleetAvgAvailability.toFixed(1)}%
              </div>
              <div className="text-[10px] font-mono text-industrial-400 mt-0.5">
                {fleet.downMachines > 0 ? (
                  <span className="text-hazard-red">{fleet.downMachines} DOWN ASSETS</span>
                ) : (
                  <span className="text-hazard-green">100% UP</span>
                )}
              </div>
            </div>
            <div className="w-full bg-industrial-900 h-1.5 border border-substrate-border overflow-hidden">
              <div className="h-full bg-industrial-300" style={{ width: `${Math.min(100, fleet.fleetAvgAvailability)}%` }} />
            </div>
          </div>

          {/* Tile 3: Fleet Performance */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>PERFORMANCE</span>
              <TrendingUp size={14} className="text-industrial-300" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.fleetAvgPerformance.toFixed(1)}%
              </div>
              <div className="text-[10px] font-mono text-industrial-400 mt-0.5">
                SPEED EFFICIENCY
              </div>
            </div>
            <div className="w-full bg-industrial-900 h-1.5 border border-substrate-border overflow-hidden">
              <div className="h-full bg-industrial-300" style={{ width: `${Math.min(100, fleet.fleetAvgPerformance)}%` }} />
            </div>
          </div>

          {/* Tile 4: Fleet Quality */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>QUALITY</span>
              <CheckCircle2 size={14} className="text-industrial-300" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.fleetAvgQuality.toFixed(1)}%
              </div>
              <div className="text-[10px] font-mono text-industrial-400 mt-0.5">
                SCRAP: {fleet.fleetScrapRate.toFixed(1)}%
              </div>
            </div>
            <div className="w-full bg-industrial-900 h-1.5 border border-substrate-border overflow-hidden">
              <div className="h-full bg-industrial-300" style={{ width: `${Math.min(100, fleet.fleetAvgQuality)}%` }} />
            </div>
          </div>

          {/* Tile 5: Total Good Output */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>TOTAL OUTPUT</span>
              <Layers size={14} className="text-industrial-300" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.totalGoodQuantity.toLocaleString()}
              </div>
              <div className="text-[10px] font-mono text-industrial-400 mt-0.5">
                SCRAP: {fleet.totalScrapQuantity.toLocaleString()} PCS
              </div>
            </div>
            <div className="text-[9px] font-mono text-industrial-500 uppercase">
              {fleet.totalPlants} SITES // {fleet.activeLines} LINES
            </div>
          </div>

          {/* Tile 6: Fleet Machines Allocation */}
          <div className="border border-substrate-border bg-substrate-card p-3 flex flex-col justify-between">
            <div className="text-[10px] font-mono text-industrial-400 tracking-wider uppercase flex items-center justify-between">
              <span>FLEET ASSETS</span>
              <Cpu size={14} className="text-industrial-300" />
            </div>
            <div className="my-2">
              <div className="text-2xl font-mono font-black text-white">
                {fleet.totalMachines}
              </div>
              <div className="text-[10px] font-mono flex items-center gap-1.5 mt-0.5">
                <span className="text-hazard-green">{fleet.runningMachines} RUN</span>
                <span className="text-industrial-500">|</span>
                <span className="text-amber-400">{fleet.idleMachines} IDLE</span>
                <span className="text-industrial-500">|</span>
                <span className="text-hazard-red">{fleet.downMachines} DOWN</span>
              </div>
            </div>
            <div className="text-[9px] font-mono text-industrial-500 uppercase">
              DT: {fleet.totalDowntimeMinutes} MINS
            </div>
          </div>
        </div>
      )}

      {/* Benchmarking Matrix Table Section */}
      <div className="border border-substrate-border bg-substrate-card">
        {/* Table Controls Bar */}
        <div className="p-4 border-b border-substrate-border flex flex-col md:flex-row items-start md:items-center justify-between gap-4 bg-industrial-950/40">
          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            {/* Search Input */}
            <div className="relative flex-1 sm:w-64">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-400" />
              <input
                type="text"
                placeholder="SEARCH PLANT OR LINE..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full bg-substrate-dark border border-substrate-border text-white text-xs font-mono pl-8 pr-3 py-1.5 placeholder:text-industrial-500 focus:outline-none focus:border-hazard-red"
              />
            </div>

            {/* Status Filter */}
            <div className="flex items-center gap-1 text-xs font-mono text-industrial-400">
              <span className="text-[10px] tracking-wider uppercase">STATUS:</span>
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="bg-substrate-dark border border-substrate-border text-white text-xs font-mono px-2 py-1 focus:outline-none focus:border-hazard-red"
              >
                <option value="ALL">ALL SITES</option>
                <option value="ACTIVE">ACTIVE ONLY</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
              </select>
            </div>
          </div>

          {/* Expand/Collapse All and Actions */}
          <div className="flex items-center gap-2">
            <button
              onClick={expandAll}
              className="px-2.5 py-1 text-[10px] font-mono border border-substrate-border bg-substrate-dark hover:bg-industrial-800 text-industrial-300"
            >
              + EXPAND ALL LINES
            </button>
            <button
              onClick={collapseAll}
              className="px-2.5 py-1 text-[10px] font-mono border border-substrate-border bg-substrate-dark hover:bg-industrial-800 text-industrial-300"
            >
              - COLLAPSE ALL
            </button>
          </div>
        </div>

        {/* Matrix Data Grid */}
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900/60 text-[10px] text-industrial-400 uppercase tracking-wider">
                <th className="py-2.5 px-3 w-14">RANK</th>
                <th className="py-2.5 px-3 min-w-[200px]">PLANT / SITE</th>
                <th className="py-2.5 px-3">TIER</th>
                <th className="py-2.5 px-3">OEE (%)</th>
                <th className="py-2.5 px-3">DELTA VS FLEET</th>
                <th className="py-2.5 px-3">AVAIL</th>
                <th className="py-2.5 px-3">PERF</th>
                <th className="py-2.5 px-3">QUAL</th>
                <th className="py-2.5 px-3">OUTPUT (PCS)</th>
                <th className="py-2.5 px-3">SCRAP</th>
                <th className="py-2.5 px-3">MACHINES (R/I/D)</th>
                <th className="py-2.5 px-3 text-right">LINE DRILLDOWN</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={12} className="py-12 text-center text-industrial-400">
                    <div className="inline-flex items-center gap-2 text-white font-mono">
                      <span className="inline-block w-4 h-4 border-2 border-hazard-red border-t-transparent animate-spin" />
                      <span>AGGREGATING CROSS-PLANT BENCHMARKS...</span>
                    </div>
                  </td>
                </tr>
              ) : filteredPlants.length === 0 ? (
                <tr>
                  <td colSpan={12} className="py-8 text-center text-industrial-500 font-mono">
                    NO PLANT BENCHMARKS MATCH THE SPECIFIED CRITERIA.
                  </td>
                </tr>
              ) : (
                filteredPlants.map(plant => {
                  const isExpanded = expandedPlantIds.has(plant.plantId);
                  const isWorldClass = plant.benchmarkTier === 'WORLD_CLASS';
                  const isTarget = plant.benchmarkTier === 'TARGET';

                  return (
                    <React.Fragment key={plant.plantId}>
                      {/* Main Plant Row */}
                      <tr className={`hover:bg-industrial-900/40 transition-colors ${isExpanded ? 'bg-industrial-900/30' : ''}`}>
                        {/* Rank */}
                        <td className="py-3 px-3 font-bold">
                          {plant.rank === 1 ? (
                            <span className="inline-block px-1.5 py-0.5 bg-hazard-green/20 text-hazard-green border border-hazard-green text-[10px]">
                              #01
                            </span>
                          ) : (
                            <span className="text-industrial-300">#{plant.rank.toString().padStart(2, '0')}</span>
                          )}
                        </td>

                        {/* Plant Info */}
                        <td className="py-3 px-3">
                          <div className="font-bold text-white flex items-center gap-1.5">
                            <Factory size={14} className="text-industrial-400 shrink-0" />
                            <span>{plant.plantCode}</span>
                            <span className="text-[10px] font-normal text-industrial-400">({plant.plantName})</span>
                          </div>
                          <div className="text-[10px] text-industrial-500 mt-0.5">
                            TZ: {plant.timezone} {plant.address ? `// ${plant.address}` : ''}
                          </div>
                        </td>

                        {/* Benchmark Tier */}
                        <td className="py-3 px-3">
                          <span
                            className={`inline-block px-2 py-0.5 text-[9px] font-bold tracking-wider uppercase border ${
                              isWorldClass
                                ? 'bg-hazard-green/10 text-hazard-green border-hazard-green/40'
                                : isTarget
                                ? 'bg-amber-400/10 text-amber-400 border-amber-400/40'
                                : 'bg-hazard-red/10 text-hazard-red border-hazard-red/40'
                            }`}
                          >
                            {plant.benchmarkTier.replace('_', ' ')}
                          </span>
                        </td>

                        {/* OEE Metric */}
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-2">
                            <span className={`font-bold text-sm ${isWorldClass ? 'text-hazard-green' : isTarget ? 'text-amber-400' : 'text-hazard-red'}`}>
                              {plant.oee.toFixed(1)}%
                            </span>
                          </div>
                          <div className="w-16 bg-industrial-900 h-1 border border-substrate-border mt-1">
                            <div
                              className={`h-full ${isWorldClass ? 'bg-hazard-green' : isTarget ? 'bg-amber-400' : 'bg-hazard-red'}`}
                              style={{ width: `${Math.min(100, plant.oee)}%` }}
                            />
                          </div>
                        </td>

                        {/* Delta vs Fleet Avg */}
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-1 text-xs">
                            {plant.oeeDeltaVsFleetAvg >= 0 ? (
                              <span className="text-hazard-green font-bold flex items-center">
                                <TrendingUp size={12} className="mr-0.5" />
                                +{plant.oeeDeltaVsFleetAvg.toFixed(1)}%
                              </span>
                            ) : (
                              <span className="text-hazard-red font-bold flex items-center">
                                <TrendingDown size={12} className="mr-0.5" />
                                {plant.oeeDeltaVsFleetAvg.toFixed(1)}%
                              </span>
                            )}
                          </div>
                          <span className="text-[9px] text-industrial-500">vs FLEET</span>
                        </td>

                        {/* Avail */}
                        <td className="py-3 px-3 text-industrial-200">
                          {plant.availability.toFixed(1)}%
                        </td>

                        {/* Perf */}
                        <td className="py-3 px-3 text-industrial-200">
                          {plant.performance.toFixed(1)}%
                        </td>

                        {/* Qual */}
                        <td className="py-3 px-3 text-industrial-200">
                          {plant.quality.toFixed(1)}%
                        </td>

                        {/* Good Output */}
                        <td className="py-3 px-3 text-white font-bold">
                          {plant.totalGoodQuantity.toLocaleString()}
                        </td>

                        {/* Scrap */}
                        <td className="py-3 px-3">
                          <div className="text-industrial-300">
                            {plant.totalScrapQuantity.toLocaleString()}
                          </div>
                          <div className="text-[10px] text-industrial-500">
                            {plant.scrapRate.toFixed(1)}%
                          </div>
                        </td>

                        {/* Machines Breakdown */}
                        <td className="py-3 px-3">
                          <div className="flex items-center gap-1 text-[11px]">
                            <span className="text-hazard-green font-bold">{plant.runningMachines}</span>
                            <span className="text-industrial-500">/</span>
                            <span className="text-amber-400">{plant.idleMachines}</span>
                            <span className="text-industrial-500">/</span>
                            <span className={plant.downMachines > 0 ? 'text-hazard-red font-bold' : 'text-industrial-500'}>
                              {plant.downMachines}
                            </span>
                          </div>
                          <span className="text-[9px] text-industrial-500 uppercase">{plant.totalMachines} TOTAL</span>
                        </td>

                        {/* Expand Button */}
                        <td className="py-3 px-3 text-right">
                          <button
                            onClick={() => toggleExpand(plant.plantId)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 text-[10px] font-mono font-bold tracking-wider uppercase border border-substrate-border bg-substrate-dark hover:bg-industrial-800 text-industrial-200 transition-all"
                          >
                            <span>{plant.lineMetrics?.length || 0} LINES</span>
                            {isExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                          </button>
                        </td>
                      </tr>

                      {/* Expandable Line-Level Drilldown Subtable */}
                      {isExpanded && (
                        <tr className="bg-industrial-950/80 border-b border-substrate-border">
                          <td colSpan={12} className="p-4 pl-8">
                            <div className="border border-substrate-border bg-substrate-dark p-3">
                              <div className="flex items-center justify-between pb-2 mb-2 border-b border-substrate-border text-[10px] text-industrial-400 uppercase tracking-wider">
                                <span className="flex items-center gap-1.5 font-bold text-white">
                                  <Layers size={12} className="text-hazard-red" />
                                  LINE-LEVEL BOTTLENECK & PRODUCTION DRILL-DOWN // {plant.plantCode}
                                </span>
                                <span>{plant.lineMetrics?.length || 0} CONFIGURED PRODUCTION LINES</span>
                              </div>

                              {(!plant.lineMetrics || plant.lineMetrics.length === 0) ? (
                                <div className="py-4 text-center text-industrial-500 text-[11px]">
                                  NO PRODUCTION LINES MAPPED UNDER THIS PLANT.
                                </div>
                              ) : (
                                <div className="overflow-x-auto">
                                  <table className="w-full text-left font-mono text-[11px]">
                                    <thead>
                                      <tr className="text-industrial-400 text-[9px] uppercase border-b border-substrate-border/50">
                                        <th className="py-1.5 px-2">LINE CODE</th>
                                        <th className="py-1.5 px-2">LINE NAME</th>
                                        <th className="py-1.5 px-2">AREA</th>
                                        <th className="py-1.5 px-2">LINE OEE</th>
                                        <th className="py-1.5 px-2">AVAIL</th>
                                        <th className="py-1.5 px-2">PERF</th>
                                        <th className="py-1.5 px-2">QUAL</th>
                                        <th className="py-1.5 px-2">OUTPUT / SCRAP</th>
                                        <th className="py-1.5 px-2">MACHINES (R/I/D)</th>
                                        <th className="py-1.5 px-2">STATUS / BOTTLENECK</th>
                                      </tr>
                                    </thead>
                                    <tbody className="divide-y divide-substrate-border/30">
                                      {plant.lineMetrics.map(line => (
                                        <tr
                                          key={line.lineId}
                                          className={`hover:bg-industrial-900/60 ${
                                            line.isBottleneck ? 'bg-hazard-red/5' : ''
                                          }`}
                                        >
                                          <td className="py-2 px-2 font-bold text-white flex items-center gap-1">
                                            {line.isBottleneck && (
                                              <AlertTriangle size={12} className="text-hazard-red shrink-0" />
                                            )}
                                            <span>{line.lineCode}</span>
                                          </td>
                                          <td className="py-2 px-2 text-industrial-300">{line.lineName}</td>
                                          <td className="py-2 px-2 text-industrial-400">{line.areaName || 'N/A'}</td>
                                          <td className="py-2 px-2">
                                            <span className={`font-bold ${line.oee >= 85 ? 'text-hazard-green' : line.oee >= 70 ? 'text-amber-400' : 'text-hazard-red'}`}>
                                              {line.oee.toFixed(1)}%
                                            </span>
                                          </td>
                                          <td className="py-2 px-2 text-industrial-300">{line.availability.toFixed(1)}%</td>
                                          <td className="py-2 px-2 text-industrial-300">{line.performance.toFixed(1)}%</td>
                                          <td className="py-2 px-2 text-industrial-300">{line.quality.toFixed(1)}%</td>
                                          <td className="py-2 px-2 text-industrial-300">
                                            <span className="text-white font-bold">{line.totalGoodQuantity.toLocaleString()}</span>
                                            <span className="text-industrial-500 text-[9px] ml-1">({line.scrapRate.toFixed(1)}% scrap)</span>
                                          </td>
                                          <td className="py-2 px-2">
                                            <span className="text-hazard-green">{line.runningMachines}</span>
                                            <span className="text-industrial-500">/</span>
                                            <span className="text-amber-400">{line.idleMachines}</span>
                                            <span className="text-industrial-500">/</span>
                                            <span className={line.downMachines > 0 ? 'text-hazard-red font-bold' : 'text-industrial-500'}>
                                              {line.downMachines}
                                            </span>
                                          </td>
                                          <td className="py-2 px-2">
                                            {line.isBottleneck ? (
                                              <span className="inline-block px-1.5 py-0.5 bg-hazard-red/20 text-hazard-red border border-hazard-red text-[9px] font-bold uppercase">
                                                [ ! BOTTLENECK ]
                                              </span>
                                            ) : (
                                              <span className="inline-block px-1.5 py-0.5 bg-hazard-green/10 text-hazard-green border border-hazard-green/30 text-[9px] uppercase">
                                                OPERATIONAL
                                              </span>
                                            )}
                                          </td>
                                        </tr>
                                      ))}
                                    </tbody>
                                  </table>
                                </div>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Executive Export & Scheduled Dispatch Controls */}
      <div className="border border-substrate-border bg-substrate-card p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-sm font-mono font-bold text-white uppercase flex items-center gap-1.5">
            <FileText size={16} className="text-hazard-red" />
            EXECUTIVE FLEET REPORTING & BENCHMARK EXPORT
          </h2>
          <p className="text-[11px] font-mono text-industrial-400 mt-0.5">
            Export standardized compliance and executive summary metrics for C-level review and supply chain coordination.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Export CSV */}
          <button
            onClick={() => handleExport('csv')}
            disabled={exportLoading !== null}
            className="flex items-center gap-1.5 px-3 py-2 border border-substrate-border bg-substrate-dark hover:bg-industrial-800 text-white text-xs font-mono font-bold transition-all disabled:opacity-50"
          >
            <Download size={14} className={exportLoading === 'csv' ? 'animate-bounce text-hazard-green' : 'text-industrial-400'} />
            <span>EXPORT CSV</span>
          </button>

          {/* Export PDF */}
          <button
            onClick={() => handleExport('pdf')}
            disabled={exportLoading !== null}
            className="flex items-center gap-1.5 px-3 py-2 border border-hazard-red/60 bg-hazard-red/10 hover:bg-hazard-red/20 text-white text-xs font-mono font-bold transition-all disabled:opacity-50"
          >
            <FileText size={14} className={exportLoading === 'pdf' ? 'animate-spin text-hazard-red' : 'text-hazard-red'} />
            <span>EXPORT PDF SUMMARY</span>
          </button>

          {/* Dispatch Scheduled Report */}
          <button
            onClick={() => scheduledReportMutation.mutate('PDF')}
            disabled={scheduledReportMutation.isPending}
            className="flex items-center gap-1.5 px-3 py-2 border border-industrial-600 bg-industrial-800 hover:bg-industrial-700 text-industrial-100 text-xs font-mono font-bold transition-all disabled:opacity-50"
            title="Dispatch Scheduled Executive Report with Audit Log"
          >
            <Send size={14} className={scheduledReportMutation.isPending ? 'animate-pulse text-hazard-green' : 'text-industrial-300'} />
            <span>DISPATCH AUDIT REPORT</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default EnterpriseFleetAnalyticsView;
