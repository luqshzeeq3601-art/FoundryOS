import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { UserDto, RoleType, PagedResponse, CreateUserRequest, UpdateUserRequest, ResetPasswordRequest } from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { Modal } from '../common/Modal';
import { Users, UserPlus, Key, Edit, Trash2, Search, CheckCircle, XCircle } from 'lucide-react';

export const AdminUsersView: React.FC = () => {
  const queryClient = useQueryClient();

  const [roleFilter, setRoleFilter] = useState<RoleType | ''>('');
  const [activeFilter, setActiveFilter] = useState<boolean | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);
  const [resettingUser, setResettingUser] = useState<UserDto | null>(null);

  // Form states
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [temporaryPassword, setTemporaryPassword] = useState('');
  const [role, setRole] = useState<RoleType>('OPERATOR');
  const [isActive, setIsActive] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Fetch Users
  const { data: usersData, isLoading } = useQuery<PagedResponse<UserDto>>({
    queryKey: ['admin-users', roleFilter, activeFilter, searchTerm, page],
    queryFn: () =>
      api.get<PagedResponse<UserDto>>('/users', {
        role: roleFilter || undefined,
        isActive: activeFilter !== undefined ? activeFilter : undefined,
        search: searchTerm || undefined,
        page,
        size: 15,
      }),
  });

  // Create User Mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => api.post<UserDto>('/users', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setIsCreateModalOpen(false);
      resetForm();
      setSuccessMessage('Operator provisioned successfully with temporary credentials.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to provision user.');
    },
  });

  // Update User Mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      api.put<UserDto>(`/users/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setEditingUser(null);
      setSuccessMessage('User privileges and profile updated.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to update user.');
    },
  });

  // Reset Password Mutation
  const resetMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResetPasswordRequest }) =>
      api.post(`/users/${id}/reset-password`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setResettingUser(null);
      setTemporaryPassword('');
      setSuccessMessage('Password reset successfully. User will be forced to rotate upon next login.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to reset password.');
    },
  });

  // Archive Mutation
  const archiveMutation = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      api.delete<UserDto>(`/users/${id}`, { expectedVersion: version }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setSuccessMessage('User identity archived and all active sessions terminated.');
    },
    onError: (err: any) => {
      setErrorMessage(err.response?.data?.error?.message || 'Failed to archive user.');
    },
  });

  const resetForm = () => {
    setEmail('');
    setDisplayName('');
    setTemporaryPassword('');
    setRole('OPERATOR');
    setIsActive(true);
    setErrorMessage(null);
  };

  const handleOpenEdit = (user: UserDto) => {
    setEditingUser(user);
    setDisplayName(user.displayName);
    setRole(user.role);
    setIsActive(user.isActive);
    setErrorMessage(null);
  };

  const handleOpenReset = (user: UserDto) => {
    setResettingUser(user);
    setTemporaryPassword('');
    setErrorMessage(null);
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    createMutation.mutate({
      email,
      displayName,
      temporaryPassword,
      role,
    });
  };

  const handleUpdateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    setErrorMessage(null);
    updateMutation.mutate({
      id: editingUser.id,
      data: {
        displayName,
        role,
        isActive,
        expectedVersion: editingUser.version,
      },
    });
  };

  const handleResetSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resettingUser) return;
    setErrorMessage(null);
    resetMutation.mutate({
      id: resettingUser.id,
      data: {
        temporaryPassword,
        expectedVersion: resettingUser.version,
      },
    });
  };

  const users = usersData?.content || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ IDENTITY & ACCESS MANAGEMENT // IAM ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <Users size={22} className="text-hazard-red" />
            <span>OPERATOR ACCESS DIRECTORY</span>
          </h1>
        </div>

        <IndustrialButton
          variant="hazard"
          size="md"
          onClick={() => {
            resetForm();
            setIsCreateModalOpen(true);
          }}
        >
          <UserPlus size={16} className="mr-1" />
          <span>PROVISION NEW OPERATOR</span>
        </IndustrialButton>
      </div>

      {/* Alert Banners */}
      {errorMessage && (
        <div className="p-3 bg-red-950/80 border border-hazard-red text-hazard-red text-xs font-mono flex items-center justify-between">
          <span>[ ERROR ]: {errorMessage}</span>
          <button onClick={() => setErrorMessage(null)} className="text-white hover:underline">
            DISMISS
          </button>
        </div>
      )}

      {successMessage && (
        <div className="p-3 bg-emerald-950/80 border border-terminal-green text-terminal-green text-xs font-mono flex items-center justify-between">
          <span>[ SUCCESS ]: {successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-white hover:underline">
            DISMISS
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col lg:flex-row items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2 w-full lg:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">ROLE:</span>
          <div className="flex gap-1 overflow-x-auto no-scrollbar">
            {(['', 'ADMIN', 'PRODUCTION_MANAGER', 'ENGINEER', 'TECHNICIAN', 'OPERATOR', 'VIEWER'] as const).map(
              (r) => (
                <button
                  key={r}
                  onClick={() => setRoleFilter(r)}
                  className={`px-2.5 py-1 text-xs font-mono uppercase border transition-colors whitespace-nowrap ${
                    roleFilter === r
                      ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                      : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
                  }`}
                >
                  {r === '' ? 'ALL' : r.replace('_', ' ')}
                </button>
              )
            )}
          </div>

          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0 ml-2">STATUS:</span>
          <div className="flex gap-1">
            <button
              onClick={() => setActiveFilter(undefined)}
              className={`px-2 py-1 text-xs font-mono uppercase border transition-colors ${
                activeFilter === undefined
                  ? 'bg-industrial-700 text-white border-industrial-400 font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              ALL
            </button>
            <button
              onClick={() => setActiveFilter(true)}
              className={`px-2 py-1 text-xs font-mono uppercase border transition-colors ${
                activeFilter === true
                  ? 'bg-emerald-950 text-terminal-green border-terminal-green font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              ACTIVE
            </button>
            <button
              onClick={() => setActiveFilter(false)}
              className={`px-2 py-1 text-xs font-mono uppercase border transition-colors ${
                activeFilter === false
                  ? 'bg-red-950 text-hazard-red border-hazard-red font-bold'
                  : 'bg-industrial-900 text-industrial-400 border-substrate-border hover:text-white'
              }`}
            >
              DISABLED
            </button>
          </div>
        </div>

        <div className="relative w-full lg:w-72">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-500" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="SEARCH BY NAME / EMAIL..."
            className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
          />
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3">DISPLAY NAME</th>
                <th className="p-3">EMAIL ADDRESS</th>
                <th className="p-3">ROLE // PRIVILEGE</th>
                <th className="p-3">STATUS</th>
                <th className="p-3">POLICY FLAGS</th>
                <th className="p-3 text-right">ACTIONS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    SCANNING OPERATOR DIRECTORY...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO MATCHING OPERATOR IDENTITIES FOUND.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id} className="hover:bg-industrial-900/60 transition-colors">
                    <td className="p-3 font-bold text-white">{u.displayName}</td>
                    <td className="p-3 text-industrial-300">{u.email}</td>
                    <td className="p-3">
                      <IndustrialBadge
                        variant={
                          u.role === 'ADMIN'
                            ? 'danger'
                            : u.role === 'PRODUCTION_MANAGER'
                            ? 'info'
                            : u.role === 'TECHNICIAN'
                            ? 'warning'
                            : 'default'
                        }
                      >
                        {u.role}
                      </IndustrialBadge>
                    </td>
                    <td className="p-3">
                      {u.isActive ? (
                        <span className="text-terminal-green flex items-center gap-1">
                          <CheckCircle size={12} />
                          <span>ACTIVE</span>
                        </span>
                      ) : (
                        <span className="text-hazard-red flex items-center gap-1">
                          <XCircle size={12} />
                          <span>DISABLED</span>
                        </span>
                      )}
                    </td>
                    <td className="p-3">
                      {u.mustChangePassword ? (
                        <span className="text-[10px] bg-amber-950/80 text-hazard-amber border border-hazard-amber/60 px-1.5 py-0.5">
                          TEMP CREDENTIALS
                        </span>
                      ) : (
                        <span className="text-industrial-500 text-[10px]">NORMAL</span>
                      )}
                    </td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleOpenReset(u)}
                          className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-hazard-amber text-industrial-300 hover:text-hazard-amber"
                          title="Reset Password"
                        >
                          <Key size={14} />
                        </button>

                        <button
                          onClick={() => handleOpenEdit(u)}
                          className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-industrial-400 text-industrial-300 hover:text-white"
                          title="Edit User"
                        >
                          <Edit size={14} />
                        </button>

                        <button
                          onClick={() => {
                            if (confirm(`Archive user identity ${u.displayName} (${u.email})?`)) {
                              archiveMutation.mutate({ id: u.id, version: u.version });
                            }
                          }}
                          className="p-1.5 bg-industrial-900 border border-substrate-border hover:border-hazard-red text-industrial-400 hover:text-hazard-red"
                          title="Archive User"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {usersData && usersData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {usersData.page + 1} OF {usersData.totalPages} ({usersData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={usersData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={usersData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>

      {/* Create User Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="PROVISION OPERATOR IDENTITY"
        subtitle="Create user account with temporary credentials and RBAC assignment"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Email Address *
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="technician.lead@foundryos.local"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Display Name / Operator ID *
            </label>
            <input
              type="text"
              required
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Marcus Vance [Lead Tech]"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Role & Privilege Class *
            </label>
            <select
              required
              value={role}
              onChange={(e) => setRole(e.target.value as RoleType)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            >
              <option value="OPERATOR">OPERATOR (Shop floor telemetry, production tracking)</option>
              <option value="TECHNICIAN">TECHNICIAN (Maintenance work orders, diagnostics)</option>
              <option value="ENGINEER">ENGINEER (Machine configuration, scheduling, repairs)</option>
              <option value="PRODUCTION_MANAGER">PRODUCTION MANAGER (Production planning, approvals)</option>
              <option value="ADMIN">ADMIN (System configuration, IAM, audit trail)</option>
              <option value="VIEWER">VIEWER (Read-only observation)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Temporary Initialization Password *
            </label>
            <input
              type="password"
              required
              value={temporaryPassword}
              onChange={(e) => setTemporaryPassword(e.target.value)}
              placeholder="Min 8 characters"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div className="text-xs font-mono text-industrial-500 bg-industrial-900 p-3 border border-substrate-border">
            [ POLICY ]: The operator will be forced to rotate this password upon their first successful login.
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
              variant="hazard"
              size="lg"
              isLoading={createMutation.isPending}
            >
              PROVISION ACCOUNT
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Edit User Modal */}
      <Modal
        isOpen={!!editingUser}
        onClose={() => setEditingUser(null)}
        title="UPDATE USER PRIVILEGES"
        subtitle={editingUser ? `${editingUser.displayName} (${editingUser.email})` : ''}
      >
        <form onSubmit={handleUpdateSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Display Name *
            </label>
            <input
              type="text"
              required
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            />
          </div>

          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              Assigned Role *
            </label>
            <select
              required
              value={role}
              onChange={(e) => setRole(e.target.value as RoleType)}
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-industrial-400"
            >
              <option value="OPERATOR">OPERATOR</option>
              <option value="TECHNICIAN">TECHNICIAN</option>
              <option value="ENGINEER">ENGINEER</option>
              <option value="PRODUCTION_MANAGER">PRODUCTION MANAGER</option>
              <option value="ADMIN">ADMIN</option>
              <option value="VIEWER">VIEWER</option>
            </select>
          </div>

          <div className="flex items-center gap-2 p-3 bg-industrial-900 border border-substrate-border">
            <input
              type="checkbox"
              id="isActiveCheck"
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 accent-terminal-green"
            />
            <label htmlFor="isActiveCheck" className="text-xs font-mono text-industrial-200 cursor-pointer">
              Account Enabled (Permit Session Authentications)
            </label>
          </div>

          <div className="text-xs font-mono text-industrial-400 bg-industrial-900 p-3 border border-substrate-border">
            [ INVARIANT ]: Demoting or deactivating the final active Administrator is blocked by system safeguards.
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setEditingUser(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="secondary"
              size="lg"
              isLoading={updateMutation.isPending}
            >
              APPLY CHANGES
            </IndustrialButton>
          </div>
        </form>
      </Modal>

      {/* Reset Password Modal */}
      <Modal
        isOpen={!!resettingUser}
        onClose={() => setResettingUser(null)}
        title="FORCE TEMPORARY PASSWORD RESET"
        subtitle={resettingUser ? `Target Account: ${resettingUser.email}` : ''}
        hazard={true}
      >
        <form onSubmit={handleResetSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-mono uppercase text-industrial-400 mb-1">
              New Temporary Password *
            </label>
            <input
              type="password"
              required
              value={temporaryPassword}
              onChange={(e) => setTemporaryPassword(e.target.value)}
              placeholder="Min 8 characters"
              className="w-full bg-industrial-900 border border-substrate-border px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-hazard-amber"
            />
          </div>

          <div className="text-xs font-mono text-hazard-amber bg-amber-950/40 p-3 border border-hazard-amber/60">
            [ NOTICE ]: Performing this reset will instantly terminate all active sessions for this operator across all terminals and require password update on their next sign-in.
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-substrate-border">
            <IndustrialButton
              type="button"
              variant="outline"
              onClick={() => setResettingUser(null)}
            >
              CANCEL
            </IndustrialButton>
            <IndustrialButton
              type="submit"
              variant="warning"
              size="lg"
              isLoading={resetMutation.isPending}
            >
              EXECUTE FORCE RESET
            </IndustrialButton>
          </div>
        </form>
      </Modal>
    </div>
  );
};
