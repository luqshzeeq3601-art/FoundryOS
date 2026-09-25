import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { MachineDto, MachineStatus, PagedResponse, CreateMachineRequest, UpdateMachineRequest, UpdateMachineStatusRequest } from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { StatusBeacon } from '../common/StatusBeacon';
import { Modal } from '../common/Modal';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { ErrorState } from '../common/ErrorState';
import { Banner } from '../common/Banner';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { useAuth } from '../../context/AuthContext';
import { Plus, Search, Edit2, Power, Trash2, Cpu } from 'lucide-react';

const ROW_ACTION_CLASS =
  'w-11 h-11 flex items-center justify-center bg-industrial-900 border border-substrate-border hover:border-industrial-400 text-industrial-300 hover:text-white';

const FIELD_CLASS =
  'w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-300';

const DialogError: React.FC<{ message: string | null }> = ({ message }) =>
  message ? (
    <div role="alert" className="p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono">
      {message}
    </div>
  ) : null;

export const MachinesView: React.FC = () => {
  const { hasRole, isAdmin } = useAuth();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<MachineStatus | ''>('');
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearch = useDebouncedValue(searchTerm.trim());
  const [page, setPage] = useState(0);
  const [archiveTarget, setArchiveTarget] = useState<MachineDto | null>(null);

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
  const {
    data: machinesData,
    isLoading,
    isError,
    error: loadError,
    refetch,
    isFetching,
  } = useQuery<PagedResponse<MachineDto>>({
    queryKey: ['machines', statusFilter, debouncedSearch, page],
    queryFn: () =>
      api.get<PagedResponse<MachineDto>>('/machines', {
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
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
      setArchiveTarget(null);
    },
    onError: (err: any) => {
      setArchiveTarget(null);
      setErrorMessage(err.response?.data?.error?.message || 'Machine could not be archived.');
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
  const anyDialogOpen = isCreateModalOpen || !!editingMachine || !!statusChangeMachine;
  const canManage = hasRole('ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER');

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-xs font-mono uppercase tracking-widest text-industrial-500">
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
            <Plus size={16} className="mr-1" aria-hidden="true" />
            <span>REGISTER MACHINE</span>
          </IndustrialButton>
        )}
      </div>

      {/* Page-level errors only; dialog errors render inside their dialog */}
      {errorMessage && !anyDialogOpen && (
        <Banner tone="error" onDismiss={() => setErrorMessage(null)}>
          {errorMessage}
        </Banner>
      )}

      {/* Filter & Search Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span id="machine-status-filter" className="text-xs font-mono uppercase text-industrial-400 shrink-0">STATUS:</span>
          <div className="flex gap-1 overflow-x-auto no-scrollbar" role="group" aria-labelledby="machine-status-filter">
            {(['', 'IDLE', 'RUNNING', 'DOWN'] as const).map((st) => (
              <button
                key={st}
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
                aria-pressed={statusFilter === st}
                className={`px-3 min-h-[44px] text-xs font-mono uppercase border transition-colors ${
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
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-400" aria-hidden="true" />
          <input
            type="search"
            aria-label="Search machines by name or serial number"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            placeholder="Search name or serial"
            className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 min-h-[44px] text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
          />
        </div>
      </div>

      {isError && (
        <ErrorState title="Machines could not be loaded" error={loadError} onRetry={() => refetch()} isRetrying={isFetching} />
      )}

      {/* Machine Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse" aria-busy={isLoading}>
            <caption className="sr-only">Registered machines</caption>
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th scope="col" className="p-3">SERIAL NUMBER</th>
                <th scope="col" className="p-3">MACHINE NAME</th>
                <th scope="col" className="p-3">LOCATION</th>
                <th scope="col" className="p-3">STATUS</th>
                <th scope="col" className="p-3">VERSION</th>
                <th scope="col" className="p-3 text-right">ACTIONS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="p-3">
                      <div className="h-6 bg-industrial-900 animate-pulse" />
                    </td>
                  </tr>
                ))
              ) : machines.length === 0 && !isError ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-sm text-industrial-300">
                    {debouncedSearch || statusFilter
                      ? 'No machines match these filters.'
                      : canManage
                      ? 'No machines registered yet. Use Register machine to add the first one.'
                      : 'No machines registered yet.'}
                  </td>
                </tr>
              ) : (
                machines.map((m) => (
                  <tr key={m.id} className="hover:bg-industrial-900/60 transition-colors">
                    <td className="p-3 font-bold text-white uppercase">{m.serialNumber}</td>
                    <td className="p-3 text-industrial-200">
                      <div>{m.name}</div>
                      {m.description && (
                        <div className="text-xs text-industrial-500 truncate max-w-xs">
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
                          type="button"
                          onClick={() => handleOpenStatus(m)}
                          className={ROW_ACTION_CLASS}
                          aria-label={`Change status of ${m.name}`}
                        >
                          <Power size={16} aria-hidden="true" />
                        </button>

                        {canManage && (
                          <button
                            type="button"
                            onClick={() => handleOpenEdit(m)}
                            className={ROW_ACTION_CLASS}
                            aria-label={`Edit ${m.name}`}
                          >
                            <Edit2 size={16} aria-hidden="true" />
                          </button>
                        )}

                        {isAdmin && (
                          <button
                            type="button"
                            onClick={() => setArchiveTarget(m)}
                            className={`${ROW_ACTION_CLASS} hover:border-hazard-red hover:text-hazard-red`}
                            aria-label={`Archive ${m.name}`}
                          >
                            <Trash2 size={16} aria-hidden="true" />
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
                size="md"
                variant="outline"
                aria-label="Previous page"
                disabled={machinesData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="md"
                variant="outline"
                aria-label="Next page"
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
        title="Register machine"
        subtitle="Add a machine to this plant's asset list."
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <DialogError message={errorMessage} />
          <div>
            <label htmlFor="machine-serial" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Serial number *
            </label>
            <input
              id="machine-serial"
              type="text"
              required
              value={serialNumber}
              onChange={(e) => setSerialNumber(e.target.value.toUpperCase())}
              placeholder="CNC-PLANT1-004"
              className={`${FIELD_CLASS} uppercase`}
            />
          </div>

          <div>
            <label htmlFor="machine-name" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Machine name *
            </label>
            <input
              id="machine-name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="5-Axis Precision CNC Mill"
              className={FIELD_CLASS}
            />
          </div>

          <div>
            <label htmlFor="machine-location" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Location *
            </label>
            <input
              id="machine-location"
              type="text"
              required
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Bay 3 - Machining Line B"
              className={FIELD_CLASS}
            />
          </div>

          <div>
            <label htmlFor="machine-notes" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Notes
            </label>
            <textarea
              id="machine-notes"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Operating tolerances, spindle specs, preventive maintenance schedules..."
              className={`${FIELD_CLASS} resize-none`}
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
        title="Edit machine"
        subtitle={editingMachine ? `Serial: ${editingMachine.serialNumber} (v${editingMachine.version})` : ''}
      >
        <form onSubmit={handleUpdateSubmit} className="space-y-4">
          <DialogError message={errorMessage} />
          <div>
            <label htmlFor="edit-machine-name" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Machine name *
            </label>
            <input
              id="edit-machine-name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={FIELD_CLASS}
            />
          </div>

          <div>
            <label htmlFor="edit-machine-location" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Location *
            </label>
            <input
              id="edit-machine-location"
              type="text"
              required
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className={FIELD_CLASS}
            />
          </div>

          <div>
            <label htmlFor="edit-machine-notes" className="block text-xs font-mono uppercase text-industrial-300 mb-1">
              Notes
            </label>
            <textarea
              id="edit-machine-notes"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={`${FIELD_CLASS} resize-none`}
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

      <ConfirmDialog
        isOpen={!!archiveTarget}
        title="Archive machine?"
        message={
          archiveTarget
            ? `${archiveTarget.name} (${archiveTarget.serialNumber}) will be removed from active lists. Its history stays in the audit log.`
            : ''
        }
        confirmLabel="Archive machine"
        isLoading={archiveMutation.isPending}
        onCancel={() => setArchiveTarget(null)}
        onConfirm={() => archiveTarget && archiveMutation.mutate({ id: archiveTarget.id, version: archiveTarget.version })}
      />

      {/* Status Change Modal */}
      <Modal
        isOpen={!!statusChangeMachine}
        onClose={() => setStatusChangeMachine(null)}
        title="Change machine status"
        subtitle={statusChangeMachine ? `${statusChangeMachine.name} (${statusChangeMachine.serialNumber})` : ''}
      >
        <form onSubmit={handleStatusSubmit} className="space-y-4">
          <DialogError message={errorMessage} />
          <div>
            <div id="status-choice-label" className="block text-xs font-mono uppercase text-industrial-300 mb-2">
              New status
            </div>
            <div className="grid grid-cols-3 gap-3" role="radiogroup" aria-labelledby="status-choice-label">
              {(['IDLE', 'RUNNING', 'DOWN'] as const).map((st) => (
                <button
                  type="button"
                  key={st}
                  role="radio"
                  aria-checked={newStatus === st}
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

          {newStatus === 'DOWN' && statusChangeMachine?.status !== 'DOWN' && (
            <div role="note" className="text-xs font-mono text-hazard-amber bg-amber-950/40 p-3 border border-hazard-amber/60">
              Marking a machine down stops it counting as available. Use Report breakdown on the dashboard if maintenance is needed.
            </div>
          )}
          <p className="text-xs font-mono text-industrial-400">
            This change is recorded in the audit log under your name.
          </p>

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
              Save status
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
