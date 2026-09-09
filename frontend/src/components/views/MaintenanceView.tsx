import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { 
  WorkOrderDto, 
  MaintenancePriority, 
  MaintenanceStatus, 
  PagedResponse, 
  MachineDto, 
  UserDto, 
  CreateWorkOrderRequest, 
  AssignWorkOrderRequest, 
  CompleteWorkOrderRequest, 
  CancelWorkOrderRequest 
} from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { Modal } from '../common/Modal';
import { useAuth } from '../../context/AuthContext';
import { Wrench, Plus, Search, UserCheck, Play, CheckCircle, XCircle, User, Calendar, Cpu } from 'lucide-react';

export const MaintenanceView: React.FC = () => {
  const { hasRole } = useAuth();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<MaintenanceStatus | ''>('');
  const [priorityFilter, setPriorityFilter] = useState<MaintenancePriority | ''>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [assigningOrder, setAssigningOrder] = useState<WorkOrderDto | null>(null);
  const [completingOrder, setCompletingOrder] = useState<WorkOrderDto | null>(null);
  const [cancellingOrder, setCancellingOrder] = useState<WorkOrderDto | null>(null);

  // Form states
  const [workOrderNumber, setWorkOrderNumber] = useState('');
  const [machineId, setMachineId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<MaintenancePriority>('MEDIUM');
  const [assignedTo, setAssignedTo] = useState('');
  const [dueAt, setDueAt] = useState('');
  const [completionNote, setCompletionNote] = useState('');
  const [cancellationNote, setCancellationNote] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch Machines
  const { data: machinesData } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines-list-all'],
    queryFn: () => api.get<PagedResponse<MachineDto>>('/machines', { size: 100 }),
  });

  // Fetch Users (for technician assignment)
  const { data: usersData } = useQuery<PagedResponse<UserDto>>({
    queryKey: ['users-technicians'],
    queryFn: () => api.get<PagedResponse<UserDto>>('/users', { size: 100 }),
  });

  // Fetch Work Orders
  const { data: workOrdersData, isLoading } = useQuery<PagedResponse<WorkOrderDto>>({
    queryKey: ['maintenance-work-orders', statusFilter, priorityFilter, searchTerm, page],
    queryFn: () =>
      api.get<PagedResponse<WorkOrderDto>>('/maintenance-work-orders', {
        status: statusFilter || undefined,
        priority: priorityFilter || undefined,
        search: searchTerm || undefined,
        page,
        size: 15,
      }),
    refetchInterval: 5000,
  });

  // Create Work Order Mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateWorkOrderRequest) =>
      api.post<WorkOrderDto>('/maintenance-work-orders', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance-work-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsCreateModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to create work order.');
    },
  });

  // Assign Mutation
  const assignMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: AssignWorkOrderRequest }) =>
      api.post<WorkOrderDto>(`/maintenance-work-orders/${id}/assign`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance-work-orders'] });
      setAssigningOrder(null);
      setAssignedTo('');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to assign work order.');
    },
  });

  // Start Mutation
  const startMutation = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      api.post<WorkOrderDto>(`/maintenance-work-orders/${id}/start?expectedVersion=${version}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance-work-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to start work order.');
    },
  });

  // Complete Mutation
  const completeMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CompleteWorkOrderRequest }) =>
      api.post<WorkOrderDto>(`/maintenance-work-orders/${id}/complete`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance-work-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setCompletingOrder(null);
      setCompletionNote('');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to complete work order.');
    },
  });

  // Cancel Mutation
  const cancelMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CancelWorkOrderRequest }) =>
      api.post<WorkOrderDto>(`/maintenance-work-orders/${id}/cancel`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance-work-orders'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setCancellingOrder(null);
      setCancellationNote('');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to cancel work order.');
    },
  });

  const resetForm = () => {
    setWorkOrderNumber('');
    setMachineId('');
    setTitle('');
    setDescription('');
    setPriority('MEDIUM');
    setAssignedTo('');
    setDueAt('');
    setErrorMessage(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    createMutation.mutate({
      workOrderNumber,
      machineId,
      title,
      description,
      priority,
      assignedTo: assignedTo || undefined,
      dueAt: dueAt ? new Date(dueAt).toISOString() : undefined,
    });
  };

  const handleAssignSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!assigningOrder || !assignedTo) return;
    setErrorMessage(null);
    assignMutation.mutate({
      id: assigningOrder.id,
      data: {
        assignedTo,
        expectedVersion: assigningOrder.version,
      },
    });
  };

  const handleCompleteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!completingOrder) return;
    setErrorMessage(null);
    completeMutation.mutate({
      id: completingOrder.id,
      data: {
        completionNote,
        expectedVersion: completingOrder.version,
      },
    });
  };

  const handleCancelSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!cancellingOrder) return;
    setErrorMessage(null);
    cancelMutation.mutate({
      id: cancellingOrder.id,
      data: {
        cancellationNote,
        expectedVersion: cancellingOrder.version,
      },
    });
  };

  const machines = machinesData?.content || [];
  const users = usersData?.content || [];
  const workOrders = workOrdersData?.content || [];
  const canCreate = hasRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER');

  const getPriorityBadge = (p: MaintenancePriority) => {
    switch (p) {
      case 'CRITICAL':
        return <IndustrialBadge variant="critical">CRITICAL</IndustrialBadge>;
      case 'HIGH':
        return <IndustrialBadge variant="danger">HIGH</IndustrialBadge>;
      case 'MEDIUM':
        return <IndustrialBadge variant="warning">MEDIUM</IndustrialBadge>;
      case 'LOW':
      default:
        return <IndustrialBadge variant="default">LOW</IndustrialBadge>;
    }
  };

  const getStatusBadge = (s: MaintenanceStatus) => {
    switch (s) {
      case 'IN_PROGRESS':
        return <IndustrialBadge variant="success">IN PROGRESS</IndustrialBadge>;
      case 'ASSIGNED':
        return <IndustrialBadge variant="info">ASSIGNED</IndustrialBadge>;
      case 'COMPLETED':
        return <IndustrialBadge variant="default">COMPLETED</IndustrialBadge>;
      case 'CANCELLED':
        return <IndustrialBadge variant="danger">CANCELLED</IndustrialBadge>;
      case 'OPEN':
      default:
        return <IndustrialBadge variant="warning">OPEN</IndustrialBadge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ MAINTENANCE, REPAIR & OVERHAUL // MRO ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <Wrench size={22} className="text-hazard-amber" />
            <span>MAINTENANCE WORK ORDERS</span>
          </h1>
        </div>

        {canCreate && (
          <IndustrialButton
            variant="warning"
            size="md"
            onClick={() => {
              resetForm();
              setIsCreateModalOpen(true);
            }}
          >
            <Plus size={16} className="mr-1" />
            <span>CREATE WORK ORDER</span>
          </IndustrialButton>
        )}
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
        <div className="flex flex-wrap items-center gap-2 w-full lg:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">STATUS:</span>
          <div className="flex gap-1 overflow-x-auto no-scrollbar">
            {(['', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as const).map((st) => (
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
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value as MaintenancePriority | '')}
            className="bg-industrial-900 border border-substrate-border px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400 w-1/2 lg:w-36"
          >
            <option value="">ALL PRIORITIES</option>
            <option value="CRITICAL">CRITICAL</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>

          <div className="relative w-1/2 lg:w-56">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-500" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="SEARCH WORK ORDERS..."
              className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>
        </div>
      </div>

      {/* Work Orders Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">WO NUMBER // TITLE</th>
                <th className="p-3">TARGET MACHINE</th>
                <th className="p-3">PRIORITY</th>
                <th className="p-3">STATUS</th>
                <th className="p-3">ASSIGNED TECHNICIAN</th>
                <th className="p-3 text-right">ACTIONS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    SCANNING WORK ORDER REPOSITORY...
                  </td>
                </tr>
              ) : workOrders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO MAINTENANCE WORK ORDERS FOUND.
                  </td>
                </tr>
              ) : (
                workOrders.map((wo) => (
                  <tr
                    key={wo.id}
                    className={`hover:bg-industrial-900/60 transition-colors ${
                      wo.priority === 'CRITICAL' ? 'bg-red-950/20' : ''
                    }`}
                  >
                    <td className="p-3">
                      <div className="font-bold text-white uppercase">{wo.workOrderNumber}</div>
                      <div className="text-industrial-200 mt-0.5">{wo.title}</div>
                      <div className="text-[10px] text-industrial-500 truncate max-w-xs">
                        {wo.description}
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="flex items-center gap-1.5 text-white">
                        <Cpu size={14} className="text-industrial-400" />
                        <span>{wo.machineName}</span>
                      </div>
                    </td>
                    <td className="p-3">{getPriorityBadge(wo.priority)}</td>
                    <td className="p-3">{getStatusBadge(wo.status)}</td>
                    <td className="p-3">
                      {wo.assignedToName ? (
                        <div className="flex items-center gap-1.5 text-white">
                          <User size={12} className="text-industrial-400" />
                          <span>{wo.assignedToName}</span>
                        </div>
                      ) : (
                        <span className="text-industrial-500 italic">UNASSIGNED</span>
                      )}
                      {wo.dueAt && (
                        <div className="text-[10px] text-industrial-400 flex items-center gap-1 mt-0.5">
                          <Calendar size={10} />
                          <span>DUE: {wo.dueAt.substring(0, 10)}</span>
                        </div>
                      )}
                    </td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {wo.status === 'OPEN' && (
                          <IndustrialButton
                            variant="secondary"
                            size="sm"
                            onClick={() => {
                              setAssigningOrder(wo);
                              setAssignedTo('');
                              setErrorMessage(null);
                            }}
                          >
                            <UserCheck size={12} className="mr-1" />
                            ASSIGN
                          </IndustrialButton>
                        )}

                        {(wo.status === 'OPEN' || wo.status === 'ASSIGNED') && (
                          <IndustrialButton
                            variant="primary"
                            size="sm"
                            onClick={() =>
                              startMutation.mutate({ id: wo.id, version: wo.version })
                            }
                          >
                            <Play size={12} className="mr-1" />
                            START
                          </IndustrialButton>
                        )}

                        {wo.status === 'IN_PROGRESS' && (
                          <IndustrialButton
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              setCompletingOrder(wo);
                              setCompletionNote('');
                              setErrorMessage(null);
                            }}
                          >
                            <CheckCircle size={12} className="mr-1" />
                            COMPLETE
                          </IndustrialButton>
                        )}

                        {wo.status !== 'COMPLETED' && wo.status !== 'CANCELLED' && (
                          <button
                            onClick={() => {
                              setCancellingOrder(wo);
                              setCancellationNote('');
                              setErrorMessage(null);
                            }}
                            className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-hazard-red text-industrial-400 hover:text-hazard-red"
                            title="Cancel Work Order"
                          >
                            <XCircle size={14} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {workOrdersData && workOrdersData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {workOrdersData.page + 1} OF {workOrdersData.totalPages} ({workOrdersData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={workOrdersData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={workOrdersData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>

      {/* Create Work Order Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="CREATE MAINTENANCE WORK ORDER"
        subtitle="Schedule preventative maintenance or log corrective repair task"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Work Order Number *
            </label>
            <input
              type="text"
              required
              value={workOrderNumber}
              onChange={(e) => setWorkOrderNumber(e.target.value.toUpperCase())}
              placeholder="WO-2026-M042"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono uppercase focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Machine Asset *
            </label>
            <select
              required
              value={machineId}
              onChange={(e) => setMachineId(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            >
              <option value="">-- SELECT MACHINE --</option>
              {machines.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.serialNumber})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Task Title *
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Replace Spindle Bearings & Calibrate Z-Axis"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
                Priority Level *
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as MaintenancePriority)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
                Target Due Date
              </label>
              <input
                type="date"
                value={dueAt}
                onChange={(e) => setDueAt(e.target.value)}
                className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Assigned Technician (Optional)
            </label>
            <select
              value={assignedTo}
              onChange={(e) => setAssignedTo(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            >
              <option value="">-- UNASSIGNED (LEAVE OPEN) --</option>
              {users
                .filter((u) => u.isActive)
                .map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName} ({u.role})
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Detailed Scope of Work & Procedures *
            </label>
            <textarea
              rows={3}
              required
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Safety lock-out tag-out steps, torque specifications, lubricant specs..."
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
              variant="warning"
              size="lg"
              isLoading={createMutation.isPending}
            >
              CREATE WORK ORDER
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Assign Technician Modal */}
      <Modal
        isOpen={!!assigningOrder}
        onClose={() => setAssigningOrder(null)}
        title="ASSIGN TECHNICIAN"
        subtitle={assigningOrder ? `Work Order: ${assigningOrder.workOrderNumber}` : ''}
      >
        <form onSubmit={handleAssignSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Select Active Technician *
            </label>
            <select
              required
              value={assignedTo}
              onChange={(e) => setAssignedTo(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2.5 text-sm text-white font-mono focus:outline-none focus:border-terminal-green"
            >
              <option value="">-- SELECT TECHNICIAN --</option>
              {users
                .filter((u) => u.isActive && (u.role === 'TECHNICIAN' || u.role === 'ENGINEER' || u.role === 'ADMIN'))
                .map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName} — {u.role} ({u.email})
                  </option>
                ))}
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setAssigningOrder(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              size="lg"
              isLoading={assignMutation.isPending}
            >
              DISPATCH ASSIGNMENT
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Complete Work Order Modal */}
      <Modal
        isOpen={!!completingOrder}
        onClose={() => setCompletingOrder(null)}
        title="COMPLETE MAINTENANCE WORK ORDER"
        subtitle={completingOrder ? `Work Order: ${completingOrder.workOrderNumber}` : ''}
      >
        <form onSubmit={handleCompleteSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Completion Notes & Post-Repair Validation *
            </label>
            <textarea
              rows={4}
              required
              value={completionNote}
              onChange={(e) => setCompletionNote(e.target.value)}
              placeholder="Confirm parts installed, test cycle results, clearance checks..."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-terminal-green resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setCompletingOrder(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="primary"
              size="lg"
              isLoading={completeMutation.isPending}
            >
              SIGN OFF & CLOSE ORDER
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Cancel Work Order Modal */}
      <Modal
        isOpen={!!cancellingOrder}
        onClose={() => setCancellingOrder(null)}
        title="CANCEL WORK ORDER"
        subtitle={cancellingOrder ? `Work Order: ${cancellingOrder.workOrderNumber}` : ''}
        hazard={true}
      >
        <form onSubmit={handleCancelSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Reason for Cancellation *
            </label>
            <textarea
              rows={3}
              required
              value={cancellationNote}
              onChange={(e) => setCancellationNote(e.target.value)}
              placeholder="Duplicate ticket, superseded by engineering change order, etc."
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-hazard-red resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setCancellingOrder(null)}
            >
              GO BACK
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="hazard"
              size="lg"
              isLoading={cancelMutation.isPending}
            >
              CONFIRM CANCELLATION
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
