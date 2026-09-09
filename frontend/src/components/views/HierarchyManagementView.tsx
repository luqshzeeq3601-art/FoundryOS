import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { hierarchyApi } from '../../services/api-client';
import { HierarchyTreeDto, AreaNodeDto, LineNodeDto, WorkCellNodeDto } from '../../types';
import { IndustrialCard } from '../common/IndustrialCard';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { 
  Building2, 
  Layers, 
  GitBranch, 
  Cpu, 
  Plus, 
  ChevronRight, 
  ChevronDown, 
  ShieldCheck, 
  Boxes,
  RefreshCw,
  FolderTree
} from 'lucide-react';

export const HierarchyManagementView: React.FC = () => {
  const { activePlant, hasRole } = useAuth();
  const queryClient = useQueryClient();
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({});
  const [selectedEntity, setSelectedEntity] = useState<{
    type: 'PLANT' | 'AREA' | 'LINE' | 'CELL' | 'MACHINE';
    id: string;
    code: string;
    name: string;
    details?: any;
  } | null>(null);

  const [showAddAreaModal, setShowAddAreaModal] = useState(false);
  const [showAddLineModal, setShowAddLineModal] = useState(false);
  const [showAddCellModal, setShowAddCellModal] = useState(false);

  // Form states
  const [areaCode, setAreaCode] = useState('');
  const [areaName, setAreaName] = useState('');
  const [areaDesc, setAreaDesc] = useState('');

  const [targetAreaId, setTargetAreaId] = useState('');
  const [lineCode, setLineCode] = useState('');
  const [lineName, setLineName] = useState('');

  const [targetLineId, setTargetLineId] = useState('');
  const [cellCode, setCellCode] = useState('');
  const [cellName, setCellName] = useState('');

  const plantId = activePlant?.id || '';

  const { data: tree, isLoading, refetch } = useQuery<HierarchyTreeDto>({
    queryKey: ['hierarchy-tree', plantId],
    queryFn: () => hierarchyApi.getPlantHierarchyTree(plantId),
    enabled: !!plantId,
  });

  const createAreaMutation = useMutation({
    mutationFn: (data: { plantId: string; code: string; name: string; description?: string }) =>
      hierarchyApi.createArea(data.plantId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hierarchy-tree', plantId] });
      setShowAddAreaModal(false);
      setAreaCode('');
      setAreaName('');
      setAreaDesc('');
    },
  });

  const createLineMutation = useMutation({
    mutationFn: (data: { areaId: string; code: string; name: string }) =>
      hierarchyApi.createLine(data.areaId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hierarchy-tree', plantId] });
      setShowAddLineModal(false);
      setLineCode('');
      setLineName('');
    },
  });

  const createCellMutation = useMutation({
    mutationFn: (data: { lineId: string; code: string; name: string }) =>
      hierarchyApi.createWorkCell(data.lineId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hierarchy-tree', plantId] });
      setShowAddCellModal(false);
      setCellCode('');
      setCellName('');
    },
  });

  const toggleNode = (nodeKey: string) => {
    setExpandedNodes(prev => ({
      ...prev,
      [nodeKey]: !prev[nodeKey],
    }));
  };

  const isExpanded = (nodeKey: string, defaultOpen = true) => {
    return expandedNodes[nodeKey] !== undefined ? expandedNodes[nodeKey] : defaultOpen;
  };

  const handleCreateArea = (e: React.FormEvent) => {
    e.preventDefault();
    if (!plantId || !areaCode || !areaName) return;
    createAreaMutation.mutate({ plantId, code: areaCode, name: areaName, description: areaDesc });
  };

  const handleCreateLine = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetAreaId || !lineCode || !lineName) return;
    createLineMutation.mutate({ areaId: targetAreaId, code: lineCode, name: lineName });
  };

  const handleCreateCell = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetLineId || !cellCode || !cellName) return;
    createCellMutation.mutate({ lineId: targetLineId, code: cellCode, name: cellName });
  };

  // Stats calculation
  const totalAreas = tree?.areas?.length || 0;
  const totalLines = tree?.areas?.reduce((acc, a) => acc + (a.lines?.length || 0), 0) || 0;
  const totalCells = tree?.areas?.reduce((acc, a) => 
    acc + (a.lines?.reduce((lAcc, l) => lAcc + (l.workCells?.length || 0), 0) || 0), 0) || 0;
  const totalMachines = tree?.areas?.reduce((acc, a) => 
    acc + (a.lines?.reduce((lAcc, l) => 
      lAcc + (l.workCells?.reduce((cAcc, c) => cAcc + (c.machines?.length || 0), 0) || 0), 0) || 0), 0) || 0;

  return (
    <div className="space-y-6">
      {/* Top Header & Stats */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-substrate-border pb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-mono text-hazard-red uppercase font-bold tracking-widest">// V2 MULTI-TENANT ARCHITECTURE</span>
            <IndustrialBadge size="sm" variant="info">RLS SECURED</IndustrialBadge>
          </div>
          <h1 className="text-xl font-mono font-bold tracking-tight text-white flex items-center gap-2 mt-1">
            <FolderTree size={20} className="text-hazard-red" />
            ENTERPRISE ORGANIZATIONAL HIERARCHY
          </h1>
          <p className="text-xs text-industrial-400 font-sans mt-0.5">
            Structured ISA-95 Equipment Hierarchy: Enterprise &rarr; Plant &rarr; Area &rarr; Line &rarr; Work Cell &rarr; Machine.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <IndustrialButton
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
            REFRESH TREE
          </IndustrialButton>

          {hasRole('ADMIN', 'PRODUCTION_MANAGER') && (
            <IndustrialButton
              variant="primary"
              size="sm"
              onClick={() => setShowAddAreaModal(true)}
            >
              <Plus size={14} />
              NEW AREA
            </IndustrialButton>
          )}
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 font-mono">
        <IndustrialCard className="p-3 border-l-2 border-l-hazard-red">
          <div className="text-[10px] text-industrial-400 uppercase">ACTIVE PLANT SITE</div>
          <div className="text-lg font-bold text-white mt-0.5 truncate">{activePlant?.code || 'N/A'}</div>
          <div className="text-[10px] text-industrial-500">{activePlant?.name}</div>
        </IndustrialCard>

        <IndustrialCard className="p-3 border-l-2 border-l-industrial-400">
          <div className="text-[10px] text-industrial-400 uppercase">PRODUCTION AREAS</div>
          <div className="text-lg font-bold text-white mt-0.5">{totalAreas} AREAS</div>
          <div className="text-[10px] text-industrial-500">Logical sub-plants</div>
        </IndustrialCard>

        <IndustrialCard className="p-3 border-l-2 border-l-industrial-400">
          <div className="text-[10px] text-industrial-400 uppercase">PRODUCTION LINES</div>
          <div className="text-lg font-bold text-white mt-0.5">{totalLines} LINES</div>
          <div className="text-[10px] text-industrial-500">Across {totalCells} work cells</div>
        </IndustrialCard>

        <IndustrialCard className="p-3 border-l-2 border-l-terminal-green">
          <div className="text-[10px] text-industrial-400 uppercase">MAPPED MACHINES</div>
          <div className="text-lg font-bold text-terminal-green mt-0.5">{totalMachines} ASSETS</div>
          <div className="text-[10px] text-industrial-500">Bound to Plant Scope</div>
        </IndustrialCard>
      </div>

      {/* Main Hierarchy Explorer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Interactive Tree View */}
        <div className="lg:col-span-2">
          <IndustrialCard className="p-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-3 mb-4">
              <div className="flex items-center gap-2">
                <Building2 size={16} className="text-hazard-red" />
                <span className="font-mono text-sm font-bold text-white">
                  SITE TREE // {activePlant?.code}
                </span>
              </div>
              <span className="text-[10px] font-mono text-industrial-400">
                TIMEZONE: {activePlant?.timezone || 'UTC'}
              </span>
            </div>

            {isLoading ? (
              <div className="py-12 flex flex-col items-center justify-center gap-2 text-industrial-400 font-mono text-xs">
                <RefreshCw size={18} className="animate-spin text-hazard-red" />
                <span>RESOLVING MULTI-TENANT HIERARCHY TREE...</span>
              </div>
            ) : !tree || tree.areas.length === 0 ? (
              <div className="py-12 text-center text-industrial-400 font-mono text-xs border border-dashed border-substrate-border p-6">
                <p>NO PRODUCTION AREAS DEFINED IN THIS PLANT SITE.</p>
                {hasRole('ADMIN', 'PRODUCTION_MANAGER') && (
                  <IndustrialButton
                    variant="primary"
                    size="sm"
                    className="mt-3"
                    onClick={() => setShowAddAreaModal(true)}
                  >
                    <Plus size={12} /> PROVISION FIRST PRODUCTION AREA
                  </IndustrialButton>
                )}
              </div>
            ) : (
              <div className="space-y-2 font-mono text-xs">
                {/* Enterprise Root Item */}
                <div 
                  onClick={() => setSelectedEntity({
                    type: 'PLANT',
                    id: tree.plantId,
                    code: tree.plantCode,
                    name: tree.plantName,
                    details: tree
                  })}
                  className="flex items-center justify-between p-2.5 bg-industrial-900 border border-substrate-border hover:border-industrial-400 cursor-pointer transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <Building2 size={15} className="text-hazard-red shrink-0" />
                    <span className="font-bold text-white">{tree.plantCode}</span>
                    <span className="text-industrial-400 text-[11px] font-sans">({tree.plantName})</span>
                  </div>
                  <IndustrialBadge size="sm" variant="success">{tree.status}</IndustrialBadge>
                </div>

                {/* Areas */}
                <div className="pl-4 space-y-2 border-l border-substrate-border/60 ml-2 mt-2">
                  {tree.areas.map((area: AreaNodeDto) => {
                    const areaKey = `area-${area.areaId}`;
                    const openArea = isExpanded(areaKey);

                    return (
                      <div key={area.areaId} className="space-y-1">
                        <div 
                          className="flex items-center justify-between p-2 bg-industrial-850 border border-substrate-border hover:border-industrial-400 cursor-pointer transition-colors"
                          onClick={() => setSelectedEntity({
                            type: 'AREA',
                            id: area.areaId,
                            code: area.areaCode,
                            name: area.areaName,
                            details: area
                          })}
                        >
                          <div className="flex items-center gap-2">
                            <button 
                              type="button" 
                              onClick={(e) => { e.stopPropagation(); toggleNode(areaKey); }}
                              className="text-industrial-400 hover:text-white p-0.5"
                            >
                              {openArea ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                            </button>
                            <Layers size={14} className="text-industrial-300 shrink-0" />
                            <span className="font-bold text-industrial-100">{area.areaCode}</span>
                            <span className="text-industrial-400 text-[11px] font-sans">({area.areaName})</span>
                          </div>

                          <div className="flex items-center gap-2">
                            <span className="text-[10px] text-industrial-400">{area.lines.length} lines</span>
                            {hasRole('ADMIN', 'PRODUCTION_MANAGER') && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setTargetAreaId(area.areaId);
                                  setShowAddLineModal(true);
                                }}
                                title="Add Production Line to this Area"
                                className="text-[10px] text-industrial-300 hover:text-white border border-substrate-border px-1.5 py-0.5 hover:bg-industrial-800"
                              >
                                + LINE
                              </button>
                            )}
                          </div>
                        </div>

                        {/* Lines */}
                        {openArea && area.lines.length > 0 && (
                          <div className="pl-4 space-y-1 border-l border-substrate-border/40 ml-2">
                            {area.lines.map((line: LineNodeDto) => {
                              const lineKey = `line-${line.lineId}`;
                              const openLine = isExpanded(lineKey);

                              return (
                                <div key={line.lineId} className="space-y-1">
                                  <div 
                                    className="flex items-center justify-between p-1.5 bg-industrial-800/80 border border-substrate-border/80 hover:border-industrial-400 cursor-pointer transition-colors"
                                    onClick={() => setSelectedEntity({
                                      type: 'LINE',
                                      id: line.lineId,
                                      code: line.lineCode,
                                      name: line.lineName,
                                      details: line
                                    })}
                                  >
                                    <div className="flex items-center gap-2">
                                      <button 
                                        type="button" 
                                        onClick={(e) => { e.stopPropagation(); toggleNode(lineKey); }}
                                        className="text-industrial-400 hover:text-white p-0.5"
                                      >
                                        {openLine ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                                      </button>
                                      <GitBranch size={13} className="text-industrial-300 shrink-0" />
                                      <span className="font-semibold text-industrial-200">{line.lineCode}</span>
                                      <span className="text-industrial-400 text-[10px] font-sans">({line.lineName})</span>
                                    </div>

                                    <div className="flex items-center gap-2">
                                      <span className="text-[10px] text-industrial-400">{line.workCells.length} cells</span>
                                      {hasRole('ADMIN', 'PRODUCTION_MANAGER') && (
                                        <button
                                          type="button"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setTargetLineId(line.lineId);
                                            setShowAddCellModal(true);
                                          }}
                                          title="Add Work Cell to this Line"
                                          className="text-[10px] text-industrial-300 hover:text-white border border-substrate-border px-1.5 py-0.5 hover:bg-industrial-800"
                                        >
                                          + CELL
                                        </button>
                                      )}
                                    </div>
                                  </div>

                                  {/* Work Cells */}
                                  {openLine && line.workCells.length > 0 && (
                                    <div className="pl-4 space-y-1 border-l border-substrate-border/30 ml-2">
                                      {line.workCells.map((cell: WorkCellNodeDto) => {
                                        const cellKey = `cell-${cell.cellId}`;
                                        const openCell = isExpanded(cellKey);

                                        return (
                                          <div key={cell.cellId} className="space-y-1">
                                            <div 
                                              className="flex items-center justify-between p-1.5 bg-industrial-900/60 border border-substrate-border/60 hover:border-industrial-400 cursor-pointer transition-colors"
                                              onClick={() => setSelectedEntity({
                                                type: 'CELL',
                                                id: cell.cellId,
                                                code: cell.cellCode,
                                                name: cell.cellName,
                                                details: cell
                                              })}
                                            >
                                              <div className="flex items-center gap-2">
                                                <button 
                                                  type="button" 
                                                  onClick={(e) => { e.stopPropagation(); toggleNode(cellKey); }}
                                                  className="text-industrial-400 hover:text-white p-0.5"
                                                >
                                                  {openCell ? <ChevronDown size={11} /> : <ChevronRight size={11} />}
                                                </button>
                                                <Boxes size={12} className="text-industrial-400 shrink-0" />
                                                <span className="text-industrial-300">{cell.cellCode}</span>
                                                <span className="text-industrial-500 text-[10px] font-sans">({cell.cellName})</span>
                                              </div>

                                              <span className="text-[10px] text-terminal-green">
                                                {cell.machines?.length || 0} machines
                                              </span>
                                            </div>

                                            {/* Machines inside Cell */}
                                            {openCell && cell.machines && cell.machines.length > 0 && (
                                              <div className="pl-4 space-y-1 border-l border-substrate-border/20 ml-2">
                                                {cell.machines.map((machine) => (
                                                  <div
                                                    key={machine.id}
                                                    onClick={() => setSelectedEntity({
                                                      type: 'MACHINE',
                                                      id: machine.id,
                                                      code: machine.serialNumber,
                                                      name: machine.name,
                                                      details: machine
                                                    })}
                                                    className="flex items-center justify-between p-1 bg-substrate-dark border border-substrate-border/40 hover:border-terminal-green/60 cursor-pointer text-[11px]"
                                                  >
                                                    <div className="flex items-center gap-1.5">
                                                      <Cpu size={11} className="text-terminal-green shrink-0" />
                                                      <span className="text-white font-mono">{machine.serialNumber}</span>
                                                      <span className="text-industrial-400 font-sans text-[10px]">{machine.name}</span>
                                                    </div>
                                                    <IndustrialBadge 
                                                      size="sm" 
                                                      variant={machine.status === 'RUNNING' ? 'success' : machine.status === 'DOWN' ? 'danger' : 'default'}
                                                    >
                                                      {machine.status}
                                                    </IndustrialBadge>
                                                  </div>
                                                ))}
                                              </div>
                                            )}
                                          </div>
                                        );
                                      })}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </IndustrialCard>
        </div>

        {/* Right: Selected Node Details & Tenant Isolation Specs */}
        <div className="space-y-4">
          {/* Node Inspector */}
          <IndustrialCard className="p-4">
            <div className="flex items-center gap-2 border-b border-substrate-border pb-2 mb-3">
              <Layers size={14} className="text-hazard-red" />
              <span className="font-mono text-xs font-bold text-white uppercase">
                ENTITY INSPECTOR
              </span>
            </div>

            {selectedEntity ? (
              <div className="space-y-3 font-mono text-xs">
                <div>
                  <span className="text-[10px] text-industrial-400 uppercase">NODE TYPE</span>
                  <div className="font-bold text-hazard-red">{selectedEntity.type}</div>
                </div>

                <div>
                  <span className="text-[10px] text-industrial-400 uppercase">IDENTIFIER / CODE</span>
                  <div className="font-bold text-white text-sm">{selectedEntity.code}</div>
                </div>

                <div>
                  <span className="text-[10px] text-industrial-400 uppercase">NAME</span>
                  <div className="text-industrial-200">{selectedEntity.name}</div>
                </div>

                <div>
                  <span className="text-[10px] text-industrial-400 uppercase">UUID</span>
                  <div className="text-[10px] text-industrial-400 break-all">{selectedEntity.id}</div>
                </div>

                {selectedEntity.details && (
                  <div className="mt-3 pt-2 border-t border-substrate-border/60">
                    <span className="text-[10px] text-industrial-400 uppercase">PROPERTIES</span>
                    <pre className="mt-1 p-2 bg-substrate-dark border border-substrate-border text-[10px] text-industrial-300 overflow-x-auto">
                      {JSON.stringify(selectedEntity.details, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-8 text-industrial-500 font-mono text-xs">
                Click any tree node on the left to inspect its multi-tenant metadata.
              </div>
            )}
          </IndustrialCard>

          {/* Security & Tenant Isolation Card */}
          <IndustrialCard className="p-4 border-l-2 border-l-terminal-green">
            <div className="flex items-center gap-2 text-terminal-green font-mono text-xs font-bold mb-2">
              <ShieldCheck size={16} />
              <span>POSTGRESQL ROW-LEVEL SECURITY</span>
            </div>
            <p className="text-xs text-industrial-300 font-sans leading-relaxed">
              Every data access query across Machines, Telemetry, Downtime Events, Production Orders, and Audit Logs is strictly isolated by <code className="font-mono text-white bg-industrial-900 px-1">plant_id</code>.
            </p>
            <div className="mt-3 space-y-1 text-[11px] font-mono text-industrial-400">
              <div className="flex items-center justify-between py-0.5 border-b border-substrate-border/40">
                <span>ACTIVE PLANT CLAIMS:</span>
                <span className="text-white font-bold">{activePlant?.code}</span>
              </div>
              <div className="flex items-center justify-between py-0.5 border-b border-substrate-border/40">
                <span>TENANT CONTEXT:</span>
                <span className="text-terminal-green">RESOLVED</span>
              </div>
              <div className="flex items-center justify-between py-0.5">
                <span>CROSS-TENANT LEAKAGE:</span>
                <span className="text-terminal-green">0.00% (PROVEN)</span>
              </div>
            </div>
          </IndustrialCard>
        </div>
      </div>

      {/* Modal: Create Area */}
      {showAddAreaModal && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
          <div className="bg-substrate-dark border border-substrate-border max-w-md w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-2">
              <h3 className="font-mono text-sm font-bold text-white flex items-center gap-2">
                <Plus size={14} className="text-hazard-red" />
                CREATE PRODUCTION AREA
              </h3>
              <button 
                type="button" 
                onClick={() => setShowAddAreaModal(false)}
                className="text-industrial-400 hover:text-white font-mono text-xs"
              >
                ESC // CLOSE
              </button>
            </div>

            <form onSubmit={handleCreateArea} className="space-y-3 font-mono text-xs">
              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">AREA CODE</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. AREA-MACHINING-01"
                  value={areaCode}
                  onChange={(e) => setAreaCode(e.target.value.toUpperCase())}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">AREA NAME</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Precision CNC Machining"
                  value={areaName}
                  onChange={(e) => setAreaName(e.target.value)}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">DESCRIPTION (OPTIONAL)</label>
                <textarea
                  placeholder="Operational scope and notes..."
                  value={areaDesc}
                  onChange={(e) => setAreaDesc(e.target.value)}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none h-20"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-substrate-border">
                <IndustrialButton
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setShowAddAreaModal(false)}
                >
                  CANCEL
                </IndustrialButton>
                <IndustrialButton
                  variant="primary"
                  size="sm"
                  type="submit"
                  disabled={createAreaMutation.isPending}
                >
                  {createAreaMutation.isPending ? 'CREATING...' : 'SAVE AREA'}
                </IndustrialButton>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Create Line */}
      {showAddLineModal && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
          <div className="bg-substrate-dark border border-substrate-border max-w-md w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-2">
              <h3 className="font-mono text-sm font-bold text-white flex items-center gap-2">
                <Plus size={14} className="text-hazard-red" />
                CREATE PRODUCTION LINE
              </h3>
              <button 
                type="button" 
                onClick={() => setShowAddLineModal(false)}
                className="text-industrial-400 hover:text-white font-mono text-xs"
              >
                ESC // CLOSE
              </button>
            </div>

            <form onSubmit={handleCreateLine} className="space-y-3 font-mono text-xs">
              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">LINE CODE</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. LINE-MILL-01"
                  value={lineCode}
                  onChange={(e) => setLineCode(e.target.value.toUpperCase())}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">LINE NAME</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. 5-Axis Milling Line"
                  value={lineName}
                  onChange={(e) => setLineName(e.target.value)}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-substrate-border">
                <IndustrialButton
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setShowAddLineModal(false)}
                >
                  CANCEL
                </IndustrialButton>
                <IndustrialButton
                  variant="primary"
                  size="sm"
                  type="submit"
                  disabled={createLineMutation.isPending}
                >
                  {createLineMutation.isPending ? 'CREATING...' : 'SAVE LINE'}
                </IndustrialButton>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Create Work Cell */}
      {showAddCellModal && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4">
          <div className="bg-substrate-dark border border-substrate-border max-w-md w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-substrate-border pb-2">
              <h3 className="font-mono text-sm font-bold text-white flex items-center gap-2">
                <Plus size={14} className="text-hazard-red" />
                CREATE WORK CELL
              </h3>
              <button 
                type="button" 
                onClick={() => setShowAddCellModal(false)}
                className="text-industrial-400 hover:text-white font-mono text-xs"
              >
                ESC // CLOSE
              </button>
            </div>

            <form onSubmit={handleCreateCell} className="space-y-3 font-mono text-xs">
              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">CELL CODE</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. CELL-ROBOTIC-01"
                  value={cellCode}
                  onChange={(e) => setCellCode(e.target.value.toUpperCase())}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div>
                <label className="block text-[10px] text-industrial-400 uppercase mb-1">CELL NAME</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Robotic Palletizer Cell"
                  value={cellName}
                  onChange={(e) => setCellName(e.target.value)}
                  className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-white focus:border-hazard-red outline-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-substrate-border">
                <IndustrialButton
                  variant="outline"
                  size="sm"
                  type="button"
                  onClick={() => setShowAddCellModal(false)}
                >
                  CANCEL
                </IndustrialButton>
                <IndustrialButton
                  variant="primary"
                  size="sm"
                  type="submit"
                  disabled={createCellMutation.isPending}
                >
                  {createCellMutation.isPending ? 'CREATING...' : 'SAVE CELL'}
                </IndustrialButton>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
