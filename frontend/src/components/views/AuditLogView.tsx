import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api-client';
import { AuditDto, PagedResponse } from '../../types';
import { IndustrialButton } from '../common/IndustrialButton';
import { IndustrialBadge } from '../common/IndustrialBadge';
import { ShieldCheck, Search, ChevronDown, ChevronRight, Hash, Clock, FileJson } from 'lucide-react';

export const AuditLogView: React.FC = () => {
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [actionSearch, setActionSearch] = useState('');
  const [page, setPage] = useState(0);
  const [expandedRows, setExpandedRows] = useState<Record<string, boolean>>({});

  const { data: auditData, isLoading } = useQuery<PagedResponse<AuditDto>>({
    queryKey: ['audit-events', entityTypeFilter, actionSearch, page],
    queryFn: () =>
      api.get<PagedResponse<AuditDto>>('/audit-events', {
        entityType: entityTypeFilter || undefined,
        action: actionSearch || undefined,
        page,
        size: 20,
      }),
  });

  const toggleRow = (id: string) => {
    setExpandedRows((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const events = auditData?.content || [];

  const formatJson = (str?: string) => {
    if (!str) return null;
    try {
      const parsed = JSON.parse(str);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return str;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-substrate-card border border-substrate-border p-4 sm:p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-industrial-500">
            [ COMPLIANCE & GOVERNANCE // IMMUTABLE TRACE ]
          </div>
          <h1 className="text-xl sm:text-2xl font-bold font-mono uppercase text-white tracking-tight flex items-center gap-2 mt-0.5">
            <ShieldCheck size={22} className="text-terminal-green" />
            <span>AUDIT LOG & EVENT STREAM</span>
          </h1>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-substrate-card border border-substrate-border p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs font-mono uppercase text-industrial-400 shrink-0">ENTITY:</span>
          <select
            value={entityTypeFilter}
            onChange={(e) => setEntityTypeFilter(e.target.value)}
            className="bg-industrial-900 border border-substrate-border px-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400 w-full sm:w-56"
          >
            <option value="">ALL DOMAIN ENTITIES</option>
            <option value="Machine">Machine</option>
            <option value="DowntimeEvent">DowntimeEvent</option>
            <option value="ProductionOrder">ProductionOrder</option>
            <option value="MaintenanceWorkOrder">MaintenanceWorkOrder</option>
            <option value="User">User</option>
          </select>
        </div>

        <div className="relative w-full sm:w-72">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-industrial-500" />
          <input
            type="text"
            value={actionSearch}
            onChange={(e) => setActionSearch(e.target.value)}
            placeholder="FILTER ACTION (E.G. CREATED, UPDATED)..."
            className="w-full bg-industrial-900 border border-substrate-border pl-9 pr-3 py-1.5 text-xs text-white font-mono focus:outline-none focus:border-industrial-400"
          />
        </div>
      </div>

      {/* Audit Event Stream */}
      <div className="bg-substrate-card border border-substrate-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left font-mono text-xs border-collapse">
            <thead>
              <tr className="border-b border-substrate-border bg-industrial-900 text-industrial-400 uppercase">
                <th className="p-3 w-8"></th>
                <th className="p-3">TIMESTAMP (UTC)</th>
                <th className="p-3">ACTION EVENT</th>
                <th className="p-3">ENTITY TYPE // ID</th>
                <th className="p-3">TRACE ID</th>
                <th className="p-3 text-right">STATE DIFF</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-substrate-border">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    RETRIEVING IMMUTABLE AUDIT LOGS...
                  </td>
                </tr>
              ) : events.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-6 text-center text-industrial-500">
                    NO AUDIT EVENTS FOUND.
                  </td>
                </tr>
              ) : (
                events.map((ev) => {
                  const isExpanded = !!expandedRows[ev.id];
                  const hasPayload = ev.beforeData || ev.afterData;

                  return (
                    <React.Fragment key={ev.id}>
                      <tr
                        className={`hover:bg-industrial-900/60 transition-colors cursor-pointer ${
                          isExpanded ? 'bg-industrial-900/80' : ''
                        }`}
                        onClick={() => hasPayload && toggleRow(ev.id)}
                      >
                        <td className="p-3 text-industrial-500">
                          {hasPayload ? (
                            isExpanded ? (
                              <ChevronDown size={14} className="text-white" />
                            ) : (
                              <ChevronRight size={14} />
                            )
                          ) : null}
                        </td>
                        <td className="p-3 text-industrial-300 flex items-center gap-1.5 whitespace-nowrap">
                          <Clock size={12} className="text-industrial-500" />
                          <span>{ev.createdAt.replace('T', ' ').substring(0, 19)}</span>
                        </td>
                        <td className="p-3">
                          <IndustrialBadge
                            variant={
                              ev.action.includes('CREATED')
                                ? 'success'
                                : ev.action.includes('DELETED') || ev.action.includes('ARCHIVED') || ev.action.includes('BREAKDOWN')
                                ? 'danger'
                                : 'info'
                            }
                          >
                            {ev.action}
                          </IndustrialBadge>
                        </td>
                        <td className="p-3">
                          <span className="font-bold text-white">{ev.entityType}</span>
                          <span className="text-industrial-500 text-[10px] ml-1.5">
                            ({ev.entityId.substring(0, 8)}...)
                          </span>
                        </td>
                        <td className="p-3 text-industrial-500 text-[10px] flex items-center gap-1">
                          <Hash size={10} />
                          <span>{ev.traceId.substring(0, 16)}</span>
                        </td>
                        <td className="p-3 text-right">
                          {hasPayload ? (
                            <span className="text-[10px] text-terminal-cyan flex items-center justify-end gap-1">
                              <FileJson size={12} />
                              <span>{isExpanded ? 'HIDE DIFF' : 'VIEW DIFF'}</span>
                            </span>
                          ) : (
                            <span className="text-industrial-600 text-[10px]">NO PAYLOAD</span>
                          )}
                        </td>
                      </tr>

                      {/* Expandable JSON State Viewer */}
                      {isExpanded && hasPayload && (
                        <tr>
                          <td colSpan={6} className="p-4 bg-black border-y border-substrate-border">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                              {/* Before State */}
                              <div>
                                <div className="text-[10px] font-mono text-hazard-amber uppercase mb-1 flex items-center gap-1">
                                  <span>[ BEFORE STATE ]</span>
                                </div>
                                <pre className="p-3 bg-industrial-950 border border-substrate-border text-industrial-300 text-[11px] overflow-x-auto">
                                  {formatJson(ev.beforeData) || 'null (No previous state)'}
                                </pre>
                              </div>

                              {/* After State */}
                              <div>
                                <div className="text-[10px] font-mono text-terminal-green uppercase mb-1 flex items-center gap-1">
                                  <span>[ AFTER STATE ]</span>
                                </div>
                                <pre className="p-3 bg-industrial-950 border border-substrate-border text-industrial-200 text-[11px] overflow-x-auto">
                                  {formatJson(ev.afterData) || 'null'}
                                </pre>
                              </div>
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

        {/* Pagination Bar */}
        {auditData && auditData.totalPages > 1 && (
          <div className="p-3 border-t border-substrate-border bg-industrial-900 flex items-center justify-between text-xs font-mono">
            <span className="text-industrial-400">
              PAGE {auditData.page + 1} OF {auditData.totalPages} ({auditData.totalElements} TOTAL)
            </span>
            <div className="flex gap-2">
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={auditData.page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                PREV
              </IndustrialButton>
              <IndustrialButton
                size="sm"
                variant="outline"
                disabled={auditData.last}
                onClick={() => setPage(p => p + 1)}
              >
                NEXT
              </IndustrialButton>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
