import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Customer, PageResponse } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { CustomerModal } from './CustomerModal';
import { CustomerLedgerModal } from './CustomerLedgerModal';
import { formatCurrency } from '../../lib/utils';
import {
  Users,
  Plus,
  Search,
  Building2,
  User,
  CheckCircle2,
  XCircle,
  Edit2,
  Phone,
  Mail,
  CreditCard,
  ChevronLeft,
  ChevronRight,
  BookOpen,
} from 'lucide-react';

export function CustomersPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [customerType, setCustomerType] = useState<'ALL' | 'B2B' | 'B2C'>('ALL');
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>('all');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false);
  const [ledgerCustomer, setLedgerCustomer] = useState<Customer | null>(null);

  // Fetch customers
  const { data: pageData, isLoading } = useQuery<PageResponse<Customer>>({
    queryKey: ['customers', search, customerType, activeFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (customerType !== 'ALL') params.set('customerType', customerType);
      if (activeFilter === 'active') params.set('activeOnly', 'true');
      if (activeFilter === 'inactive') params.set('activeOnly', 'false');
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'name');
      params.set('sortDir', 'asc');
      return api.get<PageResponse<Customer>>(`/customers?${params.toString()}`);
    },
  });

  // Toggle status mutation
  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      api.patch<Customer>(`/customers/${id}/status?active=${active}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
  });

  const customers = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  // Stats
  const b2bCount = customers.filter((c) => c.customerType === 'B2B').length;
  const b2cCount = customers.filter((c) => c.customerType === 'B2C').length;
  const totalReceivables = customers.reduce((acc, c) => acc + (c.outstandingBalance || 0), 0);

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <Users className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Customer Management
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Maintain your B2B corporate clients and B2C retail accounts with GSTIN validation and ledger tracking.
          </p>
        </div>
        <div>
          <Button
            onClick={() => {
              setSelectedCustomer(null);
              setIsCustomerModalOpen(true);
            }}
            className="flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
          >
            <Plus className="w-4 h-4" />
            Add Customer
          </Button>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Customers
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{totalElements}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-indigo-600/10 text-indigo-600 dark:text-indigo-400">
              <Users className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-blue-50/50 to-white dark:from-blue-950/20 dark:to-slate-900 border-blue-100 dark:border-blue-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                B2B Enterprises
              </p>
              <h3 className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">{b2bCount}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-blue-600/10 text-blue-600 dark:text-blue-400">
              <Building2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-emerald-50/50 to-white dark:from-emerald-950/20 dark:to-slate-900 border-emerald-100 dark:border-emerald-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                B2C Consumers
              </p>
              <h3 className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">{b2cCount}</h3>
            </div>
            <div className="p-2.5 rounded-xl bg-emerald-600/10 text-emerald-600 dark:text-emerald-400">
              <User className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 border-amber-100 dark:border-amber-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Receivables
              </p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {formatCurrency(totalReceivables)}
              </h3>
            </div>
            <div className="p-2.5 rounded-xl bg-amber-600/10 text-amber-600 dark:text-amber-400">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by name, phone, GSTIN, city..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full pl-10 pr-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto">
            {/* Customer Type Filter */}
            <div className="flex rounded-xl border border-slate-200 dark:border-slate-800 p-0.5 bg-slate-50 dark:bg-slate-950">
              <button
                onClick={() => {
                  setCustomerType('ALL');
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  customerType === 'ALL'
                    ? 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white shadow-sm'
                    : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
                }`}
              >
                All Types
              </button>
              <button
                onClick={() => {
                  setCustomerType('B2B');
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  customerType === 'B2B'
                    ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
                }`}
              >
                B2B Only
              </button>
              <button
                onClick={() => {
                  setCustomerType('B2C');
                  setPage(0);
                }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  customerType === 'B2C'
                    ? 'bg-white dark:bg-slate-800 text-emerald-600 dark:text-emerald-400 shadow-sm'
                    : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
                }`}
              >
                B2C Only
              </button>
            </div>

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

      {/* Customers Data Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center p-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : customers.length === 0 ? (
          <div className="text-center py-12">
            <Users className="w-12 h-12 text-slate-400 mx-auto mb-3 stroke-[1.5]" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">No customers found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Add your first customer to start issuing sales orders and GST tax invoices.
            </p>
            <div className="mt-4">
              <Button
                onClick={() => {
                  setSelectedCustomer(null);
                  setIsCustomerModalOpen(true);
                }}
                className="gap-1.5"
              >
                <Plus className="w-4 h-4" /> Add Customer
              </Button>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-5 py-3.5">Customer & Type</th>
                  <th className="px-4 py-3.5">GSTIN / Identification</th>
                  <th className="px-4 py-3.5">Contact</th>
                  <th className="px-4 py-3.5">State & Code</th>
                  <th className="px-4 py-3.5">Credit Limit</th>
                  <th className="px-4 py-3.5">Outstanding Balance</th>
                  <th className="px-4 py-3.5">Status</th>
                  <th className="px-5 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {customers.map((customer) => (
                  <tr
                    key={customer.id}
                    className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="font-semibold text-slate-900 dark:text-slate-100">
                          {customer.name}
                        </div>
                        <Badge variant={customer.customerType === 'B2B' ? 'info' : 'success'}>
                          {customer.customerType}
                        </Badge>
                      </div>
                      {customer.businessName && (
                        <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                          {customer.businessName}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      {customer.gstin ? (
                        <div className="flex items-center gap-1.5">
                          <span className="font-mono text-[11px] font-medium text-slate-900 dark:text-slate-100 px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800">
                            {customer.gstin}
                          </span>
                        </div>
                      ) : (
                        <span className="text-slate-400 text-[11px]">Unregistered</span>
                      )}
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="space-y-0.5">
                        {customer.phone && (
                          <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                            <Phone className="w-3 h-3 text-slate-400" />
                            <span>{customer.phone}</span>
                          </div>
                        )}
                        {customer.email && (
                          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400 text-[11px]">
                            <Mail className="w-3 h-3 text-slate-400" />
                            <span>{customer.email}</span>
                          </div>
                        )}
                        {!customer.phone && !customer.email && (
                          <span className="text-slate-400">—</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300">
                      <div>{customer.stateName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">Code: {customer.stateCode}</div>
                    </td>
                    <td className="px-4 py-3.5 text-slate-600 dark:text-slate-300">
                      {formatCurrency(customer.creditLimit)}
                    </td>
                    <td className="px-4 py-3.5 font-semibold text-slate-900 dark:text-slate-100">
                      {formatCurrency(customer.outstandingBalance)}
                    </td>
                    <td className="px-4 py-3.5">
                      {customer.active ? (
                        <Badge variant="success">Active</Badge>
                      ) : (
                        <Badge variant="default">Inactive</Badge>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => setLedgerCustomer(customer)}
                          className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                          title="View Customer Ledger & Sales"
                        >
                          <BookOpen className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => {
                            setSelectedCustomer(customer);
                            setIsCustomerModalOpen(true);
                          }}
                          className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                          title="Edit customer"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() =>
                            toggleStatusMutation.mutate({
                              id: customer.id,
                              active: !customer.active,
                            })
                          }
                          className={`p-1.5 rounded-lg transition-colors ${
                            customer.active
                              ? 'text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40'
                              : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/40'
                          }`}
                          title={customer.active ? 'Deactivate customer' : 'Activate customer'}
                        >
                          {customer.active ? (
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
              Showing page {page + 1} of {totalPages} ({totalElements} total customers)
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

      {/* Customer Modal */}
      <CustomerModal
        isOpen={isCustomerModalOpen}
        onClose={() => setIsCustomerModalOpen(false)}
        customer={selectedCustomer}
      />

      {/* Customer Ledger Modal */}
      {ledgerCustomer && (
        <CustomerLedgerModal
          customer={ledgerCustomer}
          onClose={() => setLedgerCustomer(null)}
        />
      )}
    </div>
  );
}
