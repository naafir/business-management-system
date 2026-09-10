import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Supplier, PageResponse } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { SupplierModal } from './SupplierModal';
import { formatCurrency } from '../../lib/utils';
import {
  Truck,
  Plus,
  Search,
  Building2,
  CheckCircle2,
  XCircle,
  Edit2,
  Phone,
  Mail,
  CreditCard,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

export function SuppliersPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>('all');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
  const [isSupplierModalOpen, setIsSupplierModalOpen] = useState(false);

  // Fetch suppliers
  const { data: pageData, isLoading } = useQuery<PageResponse<Supplier>>({
    queryKey: ['suppliers', search, activeFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (activeFilter === 'active') params.set('activeOnly', 'true');
      if (activeFilter === 'inactive') params.set('activeOnly', 'false');
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'name');
      params.set('sortDir', 'asc');
      return api.get<PageResponse<Supplier>>(`/suppliers?${params.toString()}`);
    },
  });

  // Toggle status mutation
  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      api.patch<Supplier>(`/suppliers/${id}/status?active=${active}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    },
  });

  const suppliers = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  // Stats
  const activeCount = suppliers.filter((s) => s.active).length;
  const totalPayables = suppliers.reduce((acc, s) => acc + (s.outstandingBalance || 0), 0);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <Truck className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Supplier Management
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Maintain your vendor directories, contact information, GSTIN details, and accounts payable.
          </p>
        </div>
        <div>
          <Button
            onClick={() => {
              setSelectedSupplier(null);
              setIsSupplierModalOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
          >
            <Plus className="w-4 h-4" />
            Add Supplier
          </Button>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Suppliers
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{totalElements}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-indigo-600/10 text-indigo-600 dark:text-indigo-400">
              <Truck className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-emerald-50/50 to-white dark:from-emerald-950/20 dark:to-slate-900 border-emerald-100 dark:border-emerald-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Active Vendors
              </p>
              <h3 className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">{activeCount}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-emerald-600/10 text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-rose-50/50 to-white dark:from-rose-950/20 dark:to-slate-900 border-rose-100 dark:border-rose-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Payables
              </p>
              <h3 className="text-2xl font-bold text-rose-600 dark:text-rose-400 mt-1">
                {formatCurrency(totalPayables)}
              </h3>
            </div>
            <div className="p-2.5 rounded-xl bg-rose-600/10 text-rose-600 dark:text-rose-400">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Search and Filters */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by vendor name, phone, GSTIN, city..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-10 pr-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            />
          </div>

          <div className="flex items-center gap-2.5 w-full sm:w-auto">
            <select
              value={activeFilter}
              onChange={(e) => {
                setActiveFilter(e.target.value as any);
                setPage(0);
              }}
              className="px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            >
              <option value="all">All Status</option>
              <option value="active">Active Only</option>
              <option value="inactive">Inactive Only</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Suppliers Data Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center p-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : suppliers.length === 0 ? (
          <div className="text-center py-12">
            <Truck className="w-12 h-12 text-slate-400 mx-auto mb-3 stroke-[1.5]" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">No suppliers found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Add your first vendor to record purchase orders, bills, and input tax credits.
            </p>
            <div className="mt-4">
              <Button
                onClick={() => {
                  setSelectedSupplier(null);
                  setIsSupplierModalOpen(true);
                }}
                className="gap-1.5"
              >
                <Plus className="w-4 h-4" /> Add Supplier
              </Button>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-5 py-3.5">Supplier & Business</th>
                  <th className="px-4 py-3.5">GSTIN</th>
                  <th className="px-4 py-3.5">Contact</th>
                  <th className="px-4 py-3.5">State & Code</th>
                  <th className="px-4 py-3.5">Outstanding Payables</th>
                  <th className="px-4 py-3.5">Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {suppliers.map((supplier) => (
                  <tr
                    key={supplier.id}
                    className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-slate-900 dark:text-slate-100">
                        {supplier.name}
                      </div>
                      {supplier.businessName && (
                        <div className="flex items-center gap-1 text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                          <Building2 className="w-3 h-3 text-slate-400" />
                          <span>{supplier.businessName}</span>
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      {supplier.gstin ? (
                        <span className="font-mono text-[11px] font-medium text-slate-900 dark:text-slate-100 px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800">
                          {supplier.gstin}
                        </span>
                      ) : (
                        <span className="text-slate-400 text-[11px]">Unregistered</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="space-y-0.5">
                        {supplier.phone && (
                          <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                            <Phone className="w-3 h-3 text-slate-400" />
                            <span>{supplier.phone}</span>
                          </div>
                        )}
                        {supplier.email && (
                          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400 text-[11px]">
                            <Mail className="w-3 h-3 text-slate-400" />
                            <span>{supplier.email}</span>
                          </div>
                        )}
                        {!supplier.phone && !supplier.email && (
                          <span className="text-slate-400">—</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300">
                      <div>{supplier.stateName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">Code: {supplier.stateCode}</div>
                    </td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900 dark:text-slate-100">
                      {formatCurrency(supplier.outstandingBalance)}
                    </td>
                    <td className="px-4 py-3.5">
                      {supplier.active ? (
                        <Badge variant="success">Active</Badge>
                      ) : (
                        <Badge variant="default">Inactive</Badge>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => {
                            setSelectedSupplier(supplier);
                            setIsSupplierModalOpen(true);
                          }}
                          className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                          title="Edit supplier"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() =>
                            toggleStatusMutation.mutate({
                              id: supplier.id,
                              active: !supplier.active,
                            })
                          }
                          className={`p-1.5 rounded-lg transition-colors ${
                            supplier.active
                              ? 'text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40'
                              : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40'
                          }`}
                          title={supplier.active ? 'Deactivate supplier' : 'Activate supplier'}
                        >
                          {supplier.active ? (
                            <XCircle className="w-4 h-4" />
                          ) : (
                            <CheckCircle2 className="w-4 h-4" />
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
            <span className="text-xs text-slate-500 dark:text-slate-400">
              Showing page {page + 1} of {totalPages} ({totalElements} total suppliers)
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="gap-1 text-xs"
              >
                <ChevronLeft className="w-3.5 h-3.5" /> Prev
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="gap-1 text-xs"
              >
                Next <ChevronRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Supplier Modal */}
      <SupplierModal
        isOpen={isSupplierModalOpen}
        onClose={() => setIsSupplierModalOpen(false)}
        supplier={selectedSupplier}
      />
    </div>
  );
}
