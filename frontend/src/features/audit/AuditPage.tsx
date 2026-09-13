import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { AuditLogEntry, PageResponse } from '../../types';
import { Card, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Shield, User, Clock, Filter, Tag } from 'lucide-react';

const ENTITY_TYPES = [
  'All', 'Sale', 'Purchase', 'Invoice', 'Product', 'Customer',
  'Supplier', 'Expense', 'Inventory', 'BusinessSettings',
];

const ACTION_COLORS: Record<string, string> = {
  CREATE: 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800',
  CREATE_SALE: 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800',
  CREATE_PURCHASE: 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800',
  CREATE_INVOICE: 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800',
  CREATE_EXPENSE: 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800',
  UPDATE: 'bg-blue-100 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800',
  UPDATE_SETTINGS: 'bg-blue-100 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800',
  FINALIZE_INVOICE: 'bg-indigo-100 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800',
  STOCK_ADJUSTMENT: 'bg-violet-100 dark:bg-violet-950/60 text-violet-700 dark:text-violet-300 border-violet-200 dark:border-violet-800',
  DEACTIVATE: 'bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800',
  DELETE: 'bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800',
  DELETE_EXPENSE: 'bg-rose-100 dark:bg-rose-950/60 text-rose-700 dark:text-rose-300 border-rose-200 dark:border-rose-800',
};

function getActionColor(action: string): string {
  if (ACTION_COLORS[action]) return ACTION_COLORS[action];
  if (action.startsWith('CREATE')) return ACTION_COLORS['CREATE'];
  if (action.startsWith('UPDATE')) return ACTION_COLORS['UPDATE'];
  if (action.startsWith('DELETE') || action.startsWith('DEACTIVATE')) return ACTION_COLORS['DELETE'];
  return 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700';
}

function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(isoString).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function AuditPage() {
  const [entityType, setEntityType] = useState('All');
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<string | null>(null);

  const { data: auditPage, isLoading } = useQuery<PageResponse<AuditLogEntry>>({
    queryKey: ['audit', entityType, page],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: '50' });
      if (entityType !== 'All') params.set('entityType', entityType);
      return api.get<PageResponse<AuditLogEntry>>(`/audit?${params}`);
    },
    staleTime: 30_000,
  });

  const entries = auditPage?.content ?? [];
  const totalPages = auditPage?.totalPages ?? 1;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
            <Shield className="w-6 h-6 text-indigo-600" />
            Audit Trail
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
            Immutable record of every create, update, and delete action
          </p>
        </div>
        <div className="flex items-center gap-1.5 flex-wrap">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          {ENTITY_TYPES.map(t => (
            <button
              key={t}
              onClick={() => { setEntityType(t); setPage(0); }}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                entityType === t
                  ? 'bg-indigo-600 border-indigo-600 text-white'
                  : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:border-indigo-300 dark:hover:border-indigo-700 hover:text-indigo-600 dark:hover:text-indigo-400'
              }`}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* Timeline */}
      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="flex gap-4 animate-pulse">
                  <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-slate-700 shrink-0" />
                  <div className="flex-1 space-y-2">
                    <div className="h-4 bg-slate-200 dark:bg-slate-700 rounded w-3/4" />
                    <div className="h-3 bg-slate-200 dark:bg-slate-700 rounded w-1/2" />
                  </div>
                </div>
              ))}
            </div>
          ) : entries.length === 0 ? (
            <div className="py-20 text-center">
              <Shield className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
              <p className="text-slate-500 dark:text-slate-400 font-medium">No audit events found</p>
              <p className="text-xs text-slate-400 mt-1">Events appear here as users take actions</p>
            </div>
          ) : (
            <div className="relative">
              {/* Vertical line */}
              <div className="absolute left-[2.75rem] top-0 bottom-0 w-px bg-slate-100 dark:bg-slate-800" />
              <ul className="divide-y divide-slate-100 dark:divide-slate-800">
                {entries.map(entry => (
                  <li key={entry.id} className="flex gap-4 px-4 py-4 hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors">
                    {/* Icon bubble */}
                    <div className="w-8 h-8 rounded-full bg-indigo-100 dark:bg-indigo-950/60 border-2 border-white dark:border-slate-900 flex items-center justify-center shrink-0 z-10 mt-0.5">
                      <User className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        {/* Action badge */}
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider border ${getActionColor(entry.action)}`}>
                          {entry.action.replace(/_/g, ' ')}
                        </span>
                        {/* Entity type */}
                        <span className="inline-flex items-center gap-1 text-xs text-slate-500 dark:text-slate-400">
                          <Tag className="w-3 h-3" />
                          {entry.entityType}
                          {entry.entityId && <span className="font-mono text-[10px] text-slate-400">{entry.entityId.substring(0, 8)}…</span>}
                        </span>
                      </div>

                      {/* Details */}
                      {entry.details && (
                        <p
                          className={`text-xs text-slate-600 dark:text-slate-400 mt-1 cursor-pointer transition-all ${expanded === entry.id ? '' : 'line-clamp-1'}`}
                          onClick={() => setExpanded(expanded === entry.id ? null : entry.id)}
                        >
                          {entry.details}
                          {entry.details.length > 80 && expanded !== entry.id && (
                            <span className="ml-1 text-indigo-500 hover:underline">more</span>
                          )}
                        </p>
                      )}

                      <div className="flex items-center gap-3 mt-1.5">
                        <span className="text-[11px] text-slate-400 flex items-center gap-1">
                          <User className="w-2.5 h-2.5" />
                          {entry.username ?? 'System'}
                        </span>
                        <span className="text-[11px] text-slate-400 flex items-center gap-1">
                          <Clock className="w-2.5 h-2.5" />
                          {formatRelativeTime(entry.createdAt)}
                        </span>
                        <span className="text-[11px] text-slate-300 dark:text-slate-600 hidden sm:inline">
                          {new Date(entry.createdAt).toLocaleString('en-IN')}
                        </span>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-slate-500">Page {page + 1} of {totalPages} — {auditPage?.totalElements ?? 0} events total</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  );
}
