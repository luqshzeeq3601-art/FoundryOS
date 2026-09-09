import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { MachineDto, MachineStatus, PagedResponse, CreateMachineRequest, UpdateMachineRequest, UpdateMachineStatusRequest } from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { StatusBeacon } from '../common/StatusBeacon';
import { Modal } from '../common/Modal';
import { useAuth } from '../../context/AuthContext';
import { Plus, Search, Edit2, Power, Trash2, Cpu } from 'lucide-react';

export const MachinesView: React.FC = () => {
  const { hasRole, isAdmin } = useAuth();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<MachineStatus | ''>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingMachine, setEditingMachine] = useState<MachineDto | null>(null);
  const [statusChangeMachine, setStatusChangeMachine] = useState<MachineDto | null>(null);
  const [newStatus, setNewStatus] = useState<MachineStatus>('IDLE');

  // Form states
  const [serialNumber, setSerialNumber] = useState('');
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const [description, setDescription] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch Machines
  const { data: machinesData, isLoading } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines', statusFilter, searchTerm, page],
    queryFn: () =>
      api.get<PagedResponse<MachineDto>>('/machines', {
        status: statusFilter || undefined,
        search: searchTerm || undefined,
        page,
        size: 15,
      }),
  });

  // Create Machine Mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateMachineRequest) => api.post<MachineDto>('/machines', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setIsCreateModalOpen(false);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to create machine asset.');
    },
  });

  // Update Machine Mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMachineRequest }) =>
      api.put<MachineDto>(`/machines/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      setEditingMachine(null);
      resetForm();
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to update machine asset.');
    },
  });

  // Update Status Mutation
  const statusMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateMachineStatusRequest }) =>
      api.patch<MachineDto>(`/machines/${id}/status`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      setStatusChangeMachine(null);
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to update machine status.');
    },
  });

  // Archive Mutation
  const archiveMutation = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      api.delete<MachineDto>(`/machines/${id}`, { expectedVersion: version }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['machines'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to archive machine.');
    },
  });

  const resetForm = () => {
    setSerialNumber('');
    setName('');
    setLocation('');
    setDescription('');
    setErrorMessage(null);
  };

  const handleOpenEdit = (machine: MachineDto) => {
    setEditingMachine(machine);
    setName(machine.name);
    setLocation(machine.location);
    setDescription(machine.description || '');
    setErrorMessage(null);
  };

  const handleOpenStatus = (machine: MachineDto) => {
    setStatusChangeMachine(machine);
    setNewStatus(machine.status);
    setErrorMessage(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    createMutation.mutate({
      serialNumber,
      name,
      location,
      description,
      status: 'IDLE',
    });
  };

  const handleUpdateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMachine) return;
    setErrorMessage(null);
    updateMutation.mutate({
      id: editingMachine.id,
      data: {
        name,
        location,
        description,
        expectedVersion: editingMachine.version,
      },
    });
  };

  const handleStatusSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!statusChangeMachine) return;
    setErrorMessage(null);
    statusMutation.mutate({
      id: statusChangeMachine.id,
      data: {
        status: newStatus,
        expectedVersion: statusChangeMachine.version,
      },
    });
  };

  const machines = machinesData?.content || [];
  const canManage = hasRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER');

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ ASSET REGISTRY // MACHINERY ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <Cpu size={22} className="text-industrial-400" />
            <span>MACHINE FLEET INVENTORY</span>
          </h1>
        </div>

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
            <span>REGISTER NEW MACHINE</span>
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

      {/* Filter & Search Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">STATUS:</span>
          <div className="flex gap-1">
            {(['', 'IDLE', 'RUNNING', 'DOWN'] as const).map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 text-xs font-mono uppercase border transition-colors ${
                  statusFilter === st
                    ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                    : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
                }`}
              >
                {st === '' ? 'ALL' : st}
              </button>
            ))}
          </div>
        </div>

        <div className="relative w-full sm:w-72">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-500" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="SEARCH ASSETS / SERIAL..."
            className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
          />
        </div>
      </div>

      {/* Machine Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">SERIAL NUMBER</th>
                <th className="p-3">MACHINE NAME</th>
                <th className="p-3">LOCATION</th>
                <th className="p-3">STATUS</th>
                <th className="p-3">VERSION</th>
                <th className="p-3 text-right">ACTIONS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    SCANNING ASSETS...
                  </td>
                </tr>
              ) : machines.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO MATCHING MACHINE ASSETS FOUND.
                  </td>
                </tr>
              ) : (
                machines.map((m) => (
                  <tr key={m.id} className="hover:bg-industrial-900/60 transition-colors">
                    <td className="p-3 font-bold text-white uppercase">{m.serialNumber}</td>
                    <td className="p-3 text-industrial-200">
                      <div>{m.name}</div>
                      {m.description && (
                        <div className="text-[10px] text-industrial-500 truncate max-w-xs">
                          {m.description}
                        </div>
                      )}
                    </td>
                    <td className="p-3 text-industrial-400">{m.location}</td>
                    <td className="p-3">
                      <StatusBeacon status={m.status} />
                    </td>
                    <td className="p-3 text-industrial-500">v{m.version}</td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleOpenStatus(m)}
                          className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-industrial-400 text-industrial-300 hover:text-white"
                          title="Toggle Status"
                        >
                          <Power size={14} />
                        </button>

                        {canManage && (
                          <button
                            onClick={() => handleOpenEdit(m)}
                            className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-industrial-400 text-industrial-300 hover:text-white"
                            title="Edit Machine"
                          >
                            <Edit2 size={14} />
                          </button>
                        )}

                        {isAdmin && (
                          <button
                            onClick={() => {
                              if (confirm(`Archive machine ${m.name} (${m.serialNumber})?`)) {
                                archiveMutation.mutate({ id: m.id, version: m.version });
                              }
                            }}
                            className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-hazard-red text-industrial-400 hover:text-hazard-red"
                            title="Archive Machine"
                          >
                            <Trash2 size={14} />
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
        {machinesData && machinesData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {machinesData.page + 1} OF {machinesData.totalPages} ({machinesData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={machinesData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={machinesData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>

      {/* Create Machine Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="REGISTER MACHINE ASSET"
        subtitle="Provision physical factory floor machinery into operating registry"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Serial Number (Unique Identifier) *
            </label>
            <input
              type="text"
              required
              value={serialNumber}
              onChange={(e) => setSerialNumber(e.target.value.toUpperCase())}
              placeholder="CNC-PLANT1-004"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono uppercase focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Machine Model / Designation *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="5-Axis Precision CNC Mill"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Floor Location / Bay *
            </label>
            <input
              type="text"
              required
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Bay 3 - Machining Line B"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Technical Specifications / Notes
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Operating tolerances, spindle specs, preventive maintenance schedules..."
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
              REGISTER ASSET
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Edit Machine Modal */}
      <Modal
        isOpen={!!editingMachine}
        onClose={() => setEditingMachine(null)}
        title="UPDATE MACHINE SPECIFICATION"
        subtitle={editingMachine ? `Serial: ${editingMachine.serialNumber} (v${editingMachine.version})` : ''}
      >
        <form onSubmit={handleUpdateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Machine Designation *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Floor Location *
            </label>
            <input
              type="text"
              required
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Technical Description
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border p-2.5 text-sm text-white font-mono focus:outline-none focus:border-industrial-400 resize-none"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setEditingMachine(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="secondary"
              size="lg"
              isLoading={updateMutation.isPending}
            >
              SAVE CHANGES
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Status Change Modal */}
      <Modal
        isOpen={!!statusChangeMachine}
        onClose={() => setStatusChangeMachine(null)}
        title="OVERRIDE MACHINE OPERATIONAL STATE"
        subtitle={statusChangeMachine ? `${statusChangeMachine.name} (${statusChangeMachine.serialNumber})` : ''}
      >
        <form onSubmit={handleStatusSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-2">
              Select Target Operating State
            </label>
            <div className="grid grid-cols-3 gap-3">
              {(['IDLE', 'RUNNING', 'DOWN'] as const).map((st) => (
                <button
                  type="button"
                  key={st}
                  onClick={() => setNewStatus(st)}
                  className={`p-3 border text-xs font-mono uppercase font-bold flex flex-col items-center justify-center gap-1.5 transition-all ${
                    newStatus === st
                      ? st === 'DOWN'
                        ? 'bg-red-950/80 border-hazard-red text-hazard-red ring-1 ring-hazard-red'
                        : st === 'RUNNING'
                        ? 'bg-emerald-950/80 border-terminal-green text-terminal-green ring-1 ring-terminal-green'
                        : 'bg-amber-950/80 border-hazard-amber text-hazard-amber ring-1 ring-hazard-amber'
                      : 'bg-industrial-900 border-substrate-border text-industrial-400 hover:text-white'
                  }`}
                >
                  <StatusBeacon status={st} showLabel={false} />
                  <span>{st}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="text-xs font-mono text-industrial-500 bg-industrial-900 p-3 border border-substrate-border">
            [ NOTE ]: Transitioning state will record an immutable audit trace under your operator identity.
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setStatusChangeMachine(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="secondary"
              size="lg"
              isLoading={statusMutation.isPending}
            >
              COMMIT STATE OVERRIDE
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
