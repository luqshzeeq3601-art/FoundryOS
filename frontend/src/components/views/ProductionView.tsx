import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { 
  ProductionOrderDto, 
  ProductionOrderStatus, 
  PagedResponse, 
  MachineDto, 
  CreateProductionOrderRequest, 
  UpdateProductionProgressRequest,
  TransitionOrderStatusRequest 
} from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { Modal } from '../common/Modal';
import { BarcodeScannerModal } from '../common/BarcodeScannerModal';
import { useAuth } from '../../context/AuthContext';
import { ClipboardList, Plus, Search, Play, CheckCircle2, XCircle, ArrowRight, Layers, Scan } from 'lucide-react';

export const ProductionView: React.FC = () => {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<ProductionOrderStatus | ''>('');
  const [selectedMachineFilter, setSelectedMachineFilter] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [selectedOrderForScan, setSelectedOrderForScan] = useState<ProductionOrderDto | null>(null);
  const [progressOrder, setProgressOrder] = useState<ProductionOrderDto | null>(null);
  const [transitionOrder, setTransitionOrder] = useState<ProductionOrderDto | null>(null);
  const [targetStatus, setTargetStatus] = useState<ProductionOrderStatus>('RELEASED');
  const [closureNote, setClosureNote] = useState('');

  // Form states
  const [orderNumber, setOrderNumber] = useState('');
  const [machineId, setMachineId] = useState('');
  const [productCode, setProductCode] = useState('');
  const [productDescription, setProductDescription] = useState('');
  const [plannedQuantity, setPlannedQuantity] = useState<number>(1000);
  const [goodQuantity, setGoodQuantity] = useState<number>(0);
  const [scrapQuantity, setScrapQuantity] = useState<number>(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch Machines
  const { data: machinesData } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-list-all'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 100 }),
  });

  // Fetch Production Orders
  const { data: ordersData, isLoading } = useQuery<PagedResponse<ProductionOrderDto>>({
    queryKey: ['production-orders', statusFilter, selectedMachineFilter, searchTerm, page],
    queryFn: () =>
      api.get<PagedResponse<ProductionOrderDto>>('/production-orders', {
        status: statusFilter || undefined,
        machineId: selectedMachineFilter || undefined,
        search: searchTerm || undefined,
        page,
        size: 15,
      }),
    refetchInterval: 5000,
  });

  // Create Order Mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateProductionOrderRequest) =>
      api.post<ProductionOrderDto>('/production-orders', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsCreateModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to create production order.');
    },
  });

  // Update Progress Mutation
  const progressMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateProductionProgressRequest }) =>
      api.patch<ProductionOrderDto>(`/production-orders/${id}/progress`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setProgressOrder(null);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to update progress.');
    },
  });

  // Transition Status Mutation
  const transitionMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: TransitionOrderStatusRequest }) =>
      api.post<ProductionOrderDto>(`/production-orders/${id}/transition`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-orders'] });
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setTransitionOrder(null);
      setClosureNote('');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to transition order status.');
    },
  });

  const resetForm = () => {
    setOrderNumber('');
    setMachineId('');
    setProductCode('');
    setProductDescription('');
    setPlannedQuantity(1000);
    setErrorMessage(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    createMutation.mutate({
      orderNumber,
      machineId,
      productCode,
      productDescription,
      plannedQuantity,
    });
  };

  const handleProgressSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!progressOrder) return;
    setErrorMessage(null);
    progressMutation.mutate({
      id: progressOrder.id,
      data: {
        goodQuantity,
        scrapQuantity,
        expectedVersion: progressOrder.version,
      },
    });
  };

  const handleTransitionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!transitionOrder) return;
    setErrorMessage(null);
    transitionMutation.mutate({
      id: transitionOrder.id,
      data: {
        targetStatus,
        closureNote: closureNote || undefined,
        expectedVersion: transitionOrder.version,
      },
    });
  };

  const openProgressModal = (order: ProductionOrderDto) => {
    setProgressOrder(order);
    setGoodQuantity(order.goodQuantity);
    setScrapQuantity(order.scrapQuantity);
    setErrorMessage(null);
  };

  const openTransitionModal = (order: ProductionOrderDto, nextStatus: ProductionOrderStatus) => {
    setTransitionOrder(order);
    setTargetStatus(nextStatus);
    setClosureNote('');
    setErrorMessage(null);
  };

  const machines = machinesData?.content || [];
  const orders = ordersData?.content || [];
  const canManage = hasRole('ADMIN', 'PRODUCTION_MANAGER');

  const getStatusBadge = (status: ProductionOrderStatus) => {
    switch (status) {
      case 'IN_PROGRESS':
        return <IndustrialBadge variant="success">IN PROGRESS</IndustrialBadge>;
      case 'RELEASED':
        return <IndustrialBadge variant="info">RELEASED</IndustrialBadge>;
      case 'COMPLETED':
        return <IndustrialBadge variant="default">COMPLETED</IndustrialBadge>;
      case 'CANCELLED':
        return <IndustrialBadge variant="danger">CANCELLED</IndustrialBadge>;
      case 'DRAFT':
      default:
        return <IndustrialBadge variant="warning">DRAFT</IndustrialBadge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ MANUFACTURING EXECUTION // PRODUCTION RUNS ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <ClipboardList size={22} className="text-terminal-green" />
            <span>PRODUCTION ORDERS</span>
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <IndustrialButton
            variant="outline"
            size="md"
            onClick={() => {
              setSelectedOrderForScan(null);
              setIsScannerOpen(true);
            }}
            className="border-cyan-500/60 text-cyan-400 hover:bg-cyan-950/40"
          >
            <Scan size={16} className="mr-1 text-cyan-400" />
            <span>SCAN BARCODE / TRAVELER</span>
          </IndustrialButton>

          {canManage && (
            <IndustrialButton
              variant="primary"
              size="md"
              onClick={() => {
                resetForm();
                setIsCreateModalOpen(true);
              }}
            >
              <Plus size={16} className="mr-1" />
              <span>CREATE PRODUCTION ORDER</span>
            </IndustrialButton>
          )}
        </div>
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
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col lg:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full lg:w-auto overflow-x-auto no-scrollbar">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">STATUS:</span>
          <div className="flex gap-1">
            {(['', 'DRAFT', 'RELEASED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 text-xs font-mono uppercase border transition-colors whitespace-nowrap ${
                  statusFilter === st
                    ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                    : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
                }`}
              >
                {st === '' ? 'ALL' : st.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2 w-full lg:w-auto">
          <select
            value={selectedMachineFilter}
            onChange={(e) => setSelectedMachineFilter(e.target.value)}
            className="bg-industrial-900 border border-substrate-border px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400 w-1/2 lg:w-48"
          >
            <option value="">ALL MACHINES</option>
            {machines.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>

          <div className="relative w-1/2 lg:w-56">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-500" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="SEARCH ORDERS..."
              className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">ORDER // ITEM</th>
                <th className="p-3">ASSIGNED ASSET</th>
                <th className="p-3">PROGRESS (GOOD / TARGET)</th>
                <th className="p-3">SCRAP RATE</th>
                <th className="p-3">STATUS</th>
                <th className="p-3 text-right">CONTROLS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    LOADING PRODUCTION SCHEDULE...
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO PRODUCTION ORDERS FOUND.
                  </td>
                </tr>
              ) : (
                orders.map((ord) => {
                  const percent = Math.min(100, Math.round((ord.goodQuantity / ord.plannedQuantity) * 100));
                  const totalMade = ord.goodQuantity + ord.scrapQuantity;
                  const scrapPct = totalMade > 0 ? ((ord.scrapQuantity / totalMade) * 100).toFixed(1) : '0.0';

                  return (
                    <tr
                      key={ord.id}
                      className={`hover:bg-industrial-900/60 transition-colors ${
                        ord.status === 'IN_PROGRESS' ? 'bg-emerald-950/15' : ''
                      }`}
                    >
                      <td className="p-3">
                        <div className="font-bold text-white uppercase">{ord.orderNumber}</div>
                        <div className="text-[11px] text-industrial-300 flex items-center gap-1 mt-0.5">
                          <Layers size={12} className="text-industrial-500" />
                          <span>{ord.productCode}</span>
                        </div>
                      </td>
                      <td className="p-3 text-industrial-200">{ord.machineName}</td>
                      <td className="p-3">
                        <div className="flex items-center justify-between text-[11px] mb-1">
                          <span className="font-bold text-white">
                            {ord.goodQuantity} / {ord.plannedQuantity}
                          </span>
                          <span className="text-industrial-400">{percent}%</span>
                        </div>
                        <div className="w-full bg-industrial-900 h-2 border border-substrate-border overflow-hidden">
                          <div
                            className={`h-full ${
                              ord.status === 'COMPLETED'
                                ? 'bg-terminal-green'
                                : ord.status === 'IN_PROGRESS'
                                ? 'bg-terminal-greenGlow'
                                : 'bg-industrial-600'
                            }`}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                      </td>
                      <td className="p-3">
                        <span
                          className={`font-bold ${
                            parseFloat(scrapPct) > 5.0 ? 'text-hazard-amber' : 'text-industrial-300'
                          }`}
                        >
                          {ord.scrapQuantity} ({scrapPct}%)
                        </span>
                      </td>
                      <td className="p-3">{getStatusBadge(ord.status)}</td>
                      <td className="p-3 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Live Progress Counter Trigger */}
                          {(ord.status === 'IN_PROGRESS' || ord.status === 'RELEASED') && (
                            <IndustrialButton
                              variant="secondary"
                              size="sm"
                              onClick={() => openProgressModal(ord)}
                              title="Update Actual Produced / Scrap Counts"
                            >
                              + COUNTS
                            </IndustrialButton>
                          )}

                          {/* State Transition Actions */}
                          {ord.status === 'DRAFT' && canManage && (
                            <IndustrialButton
                              variant="outline"
                              size="sm"
                              onClick={() => openTransitionModal(ord, 'RELEASED')}
                            >
                              RELEASE
                            </IndustrialButton>
                          )}

                          {ord.status === 'RELEASED' && (
                            <IndustrialButton
                              variant="primary"
                              size="sm"
                              onClick={() => openTransitionModal(ord, 'IN_PROGRESS')}
                            >
                              <Play size={12} className="mr-1" />
                              START RUN
                            </IndustrialButton>
                          )}

                          {ord.status === 'IN_PROGRESS' && (
                            <IndustrialButton
                              variant="primary"
                              size="sm"
                              onClick={() => openTransitionModal(ord, 'COMPLETED')}
                            >
                              <CheckCircle2 size={12} className="mr-1" />
                              COMPLETE
                            </IndustrialButton>
                          )}

                          {/* Scan Material Lot for Active Orders */}
                          {(ord.status === 'IN_PROGRESS' || ord.status === 'RELEASED') && (
                            <button
                              onClick={() => {
                                setSelectedOrderForScan(ord);
                                setIsScannerOpen(true);
                              }}
                              className="p-1.5 bg-industrial-900 border border-cyan-800 hover:border-cyan-400 text-cyan-400 hover:text-white"
                              title="Scan Raw Material Lot for BOM Validation"
                            >
                              <Scan size={14} />
                            </button>
                          )}

                          {ord.status !== 'COMPLETED' && ord.status !== 'CANCELLED' && canManage && (
                            <button
                              onClick={() => openTransitionModal(ord, 'CANCELLED')}
                              className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-hazard-red text-industrial-400 hover:text-hazard-red"
                              title="Cancel Order"
                            >
                              <XCircle size={14} />
                            </button>
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
        {ordersData && ordersData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {ordersData.page + 1} OF {ordersData.totalPages} ({ordersData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={ordersData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={ordersData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>

      {/* Create Production Order Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="SCHEDULE PRODUCTION RUN"
        subtitle="Issue new production batch order and assign machine slot"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Order Number (Unique) *
            </label>
            <input
              type="text"
              required
              value={orderNumber}
              onChange={(e) => setOrderNumber(e.target.value.toUpperCase())}
              placeholder="PO-2026-0901"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono uppercase focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Target Machine Line *
            </label>
            <select
              required
              value={machineId}
              onChange={(e) => setMachineId(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            >
              <option value="">-- SELECT MACHINE ASSET --</option>
              {machines.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.serialNumber}) — Current Status: {m.status}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Product SKU / Part Code *
            </label>
            <input
              type="text"
              required
              value={productCode}
              onChange={(e) => setProductCode(e.target.value)}
              placeholder="PART-AEROSPACE-X42"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Planned Output Batch Quantity *
            </label>
            <input
              type="number"
              min={1}
              required
              value={plannedQuantity}
              onChange={(e) => setPlannedQuantity(parseInt(e.target.value) || 0)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Order Description / Notes
            </label>
            <textarea
              rows={2}
              value={productDescription}
              onChange={(e) => setProductDescription(e.target.value)}
              placeholder="Material batch lot, customer reference, quality tolerances..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-400 resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              size="lg"
              isLoading={createMutation.isPending}
            >
              CREATE ORDER
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Live Production Progress Counter Modal */}
      <Modal
        isOpen={!!progressOrder}
        onClose={() => setProgressOrder(null)}
        title="UPDATE LIVE PRODUCTION COUNTS"
        subtitle={progressOrder ? `Order: ${progressOrder.orderNumber} // Target: ${progressOrder.plannedQuantity}` : ''}
      >
        <form onSubmit={handleProgressSubmit} className="space-y-6">
          {/* Good Units Section */}
          <div className="p-4 bg-industrial-900 border border-substrate-border">
            <div className="text-xs font-mono uppercase text-terminal-green font-bold mb-2">
              GOOD FINISHED UNITS
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setGoodQuantity(q => Math.max(0, q - 10))}
                className="w-12 h-12 bg-industrial-800 border border-substrate-border text-white text-lg font-bold flex items-center justify-center hover:bg-industrial-700"
              >
                -10
              </button>
              <input
                type="number"
                min={0}
                value={goodQuantity}
                onChange={(e) => setGoodQuantity(Math.max(0, parseInt(e.target.value) || 0))}
                className="flex-1 bg-substrate-dark border-2 border-terminal-green p-3 text-2xl font-bold font-mono text-center text-white focus:outline-none"
              />
              <button
                type="button"
                onClick={() => setGoodQuantity(q => q + 10)}
                className="w-12 h-12 bg-industrial-800 border border-substrate-border text-white text-lg font-bold flex items-center justify-center hover:bg-industrial-700"
              >
                +10
              </button>
            </div>
          </div>

          {/* Scrap Units Section */}
          <div className="p-4 bg-industrial-900 border border-substrate-border">
            <div className="text-xs font-mono uppercase text-hazard-amber font-bold mb-2">
              SCRAP / DEFECT UNITS
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setScrapQuantity(q => Math.max(0, q - 1))}
                className="w-12 h-12 bg-industrial-800 border border-substrate-border text-white text-lg font-bold flex items-center justify-center hover:bg-industrial-700"
              >
                -1
              </button>
              <input
                type="number"
                min={0}
                value={scrapQuantity}
                onChange={(e) => setScrapQuantity(Math.max(0, parseInt(e.target.value) || 0))}
                className="flex-1 bg-substrate-dark border-2 border-hazard-amber p-3 text-2xl font-bold font-mono text-center text-white focus:outline-none"
              />
              <button
                type="button"
                onClick={() => setScrapQuantity(q => q + 1)}
                className="w-12 h-12 bg-industrial-800 border border-substrate-border text-white text-lg font-bold flex items-center justify-center hover:bg-industrial-700"
              >
                +1
              </button>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setProgressOrder(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              size="lg"
              isLoading={progressMutation.isPending}
            >
              RECORD TELEMETRY COUNTS
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* State Transition Modal */}
      <Modal
        isOpen={!!transitionOrder}
        onClose={() => setTransitionOrder(null)}
        title="CONFIRM ORDER STATE TRANSITION"
        subtitle={transitionOrder ? `Transitioning ${transitionOrder.orderNumber} to ${targetStatus}` : ''}
      >
        <form onSubmit={handleTransitionSubmit} className="space-y-4">
          <div className="p-3 bg-industrial-900 border border-substrate-border text-xs font-mono text-industrial-300">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-industrial-400">CURRENT STATUS:</span>
              {transitionOrder && getStatusBadge(transitionOrder.status)}
              <ArrowRight size={14} className="text-industrial-500" />
              {getStatusBadge(targetStatus)}
            </div>
            {targetStatus === 'IN_PROGRESS' && (
              <p className="text-terminal-green mt-1">
                Starting this order will synchronize machine &quot;{transitionOrder?.machineName}&quot; to RUNNING status.
              </p>
            )}
            {targetStatus === 'COMPLETED' && (
              <p className="text-white mt-1">
                Completing this run will timestamp batch closeout and return machine &quot;{transitionOrder?.machineName}&quot; to IDLE status.
              </p>
            )}
          </div>

          {(targetStatus === 'COMPLETED' || targetStatus === 'CANCELLED') && (
            <div>
              <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
                Closure / Quality Sign-off Notes
              </label>
              <textarea
                rows={3}
                value={closureNote}
                onChange={(e) => setClosureNote(e.target.value)}
                placeholder="Supervisor signoff, shift yield notes, rework instructions..."
                className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-400 resize-none"
              />
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setTransitionOrder(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="secondary"
              size="lg"
              isLoading={transitionMutation.isPending}
            >
              CONFIRM TRANSITION
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Barcode & Material Traceability Scanner Modal */}
      <BarcodeScannerModal
        isOpen={isScannerOpen}
        onClose={() => {
          setIsScannerOpen(false);
          setSelectedOrderForScan(null);
        }}
        targetMachineId={selectedOrderForScan ? selectedOrderForScan.machineId : undefined}
        targetProductionOrderId={selectedOrderForScan ? selectedOrderForScan.id : undefined}
        title={
          selectedOrderForScan
            ? `VERIFY MATERIAL LOT (ORDER: ${selectedOrderForScan.orderNumber} // PRODUCT: ${selectedOrderForScan.productCode})`
            : '2D BARCODE & MATERIAL TRACEABILITY SCANNER'
        }
        onScanSuccess={(res) => {
          if (res.barcodeType === 'TRAVELER' && res.entityData?.orderNumber) {
            setSearchTerm(res.entityData.orderNumber);
          }
        }}
      />
    </div>
  );
};
