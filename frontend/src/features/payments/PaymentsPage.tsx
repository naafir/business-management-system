import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Sale, Purchase, PageResponse, PaymentMethod } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { formatCurrency, formatDate } from '../../lib/utils';
import { toast } from '../../hooks/useToast';
import {
  CreditCard,
  TrendingUp,
  TrendingDown,
  IndianRupee,
  CheckCircle2,
  Search,
  ChevronLeft,
  ChevronRight,
  Banknote,
  ArrowDownLeft,
  ArrowUpRight,
} from 'lucide-react';

type RecordType = 'sale' | 'purchase';

interface PaymentFormData {
  amount: string;
  paymentMethod: PaymentMethod;
  notes: string;
}

interface PaymentRecord {
  id: string;
  referenceNumber: string;
  partyName: string;
  type: RecordType;
  date: string;
  grandTotal: number;
  amountPaid: number;
  balanceDue: number;
  paymentStatus: string;
  paymentMethod?: PaymentMethod;
}

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Cash' },
  { value: 'UPI', label: 'UPI / QR' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer (NEFT/RTGS)' },
  { value: 'CARD', label: 'Card' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CREDIT', label: 'Credit (Deferred)' },
];

function PaymentModal({
  record,
  onClose,
}: {
  record: PaymentRecord | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<PaymentFormData>({
    amount: record ? record.balanceDue.toFixed(2) : '',
    paymentMethod: 'CASH',
    notes: '',
  });
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: async (data: PaymentFormData) => {
      if (!record) return;
      const endpoint = record.type === 'sale'
        ? `/sales/${record.id}/payment`
        : `/purchases/${record.id}/payment`;
      return api.post(endpoint, {
        amountPaid: parseFloat(data.amount),
        paymentMethod: data.paymentMethod,
        notes: data.notes.trim() || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments-sales'] });
      queryClient.invalidateQueries({ queryKey: ['payments-purchases'] });
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['purchases'] });
      toast.success('Payment recorded successfully!');
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Failed to record payment');
    },
  });

  if (!record) return null;

  const maxAmount = record.balanceDue;
  const enteredAmount = parseFloat(form.amount) || 0;
  const isOverpayment = enteredAmount > maxAmount + 0.01;

  return (
    <Modal
      isOpen={true}
      onClose={onClose}
      title="Record Payment"
      description={`Record incoming/outgoing payment for ${record.type === 'sale' ? 'Sale' : 'Purchase'} #${record.referenceNumber}`}
      className="max-w-md"
    >
      <div className="space-y-4">
        {/* Summary */}
        <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl p-4 border border-slate-200 dark:border-slate-700 text-xs space-y-2">
          <div className="flex justify-between">
            <span className="text-slate-500 dark:text-slate-400">Party</span>
            <span className="font-semibold text-slate-900 dark:text-white">{record.partyName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500 dark:text-slate-400">Reference</span>
            <span className="font-medium text-indigo-600 dark:text-indigo-400">{record.referenceNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500 dark:text-slate-400">Grand Total</span>
            <span className="font-semibold">{formatCurrency(record.grandTotal)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-500 dark:text-slate-400">Already Paid</span>
            <span className="font-semibold text-emerald-600 dark:text-emerald-400">{formatCurrency(record.amountPaid)}</span>
          </div>
          <div className="flex justify-between border-t border-slate-200 dark:border-slate-700 pt-2 mt-2">
            <span className="text-slate-700 dark:text-slate-300 font-semibold">Balance Due</span>
            <span className="font-bold text-rose-600 dark:text-rose-400">{formatCurrency(record.balanceDue)}</span>
          </div>
        </div>

        {error && (
          <div className="p-3 text-xs rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800">
            {error}
          </div>
        )}

        <Input
          label="Payment Amount (₹) *"
          type="number"
          step="0.01"
          min="0.01"
          max={maxAmount}
          value={form.amount}
          onChange={e => setForm({ ...form, amount: e.target.value })}
          required
        />
        {isOverpayment && (
          <p className="text-xs text-amber-600 dark:text-amber-400 -mt-2">
            ⚠ Amount exceeds balance due of {formatCurrency(maxAmount)}
          </p>
        )}

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Payment Method *
          </label>
          <select
            value={form.paymentMethod}
            onChange={e => setForm({ ...form, paymentMethod: e.target.value as PaymentMethod })}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
          >
            {PAYMENT_METHODS.map(m => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Notes / Reference
          </label>
          <textarea
            rows={2}
            placeholder="UPI transaction ID, cheque number, etc."
            value={form.notes}
            onChange={e => setForm({ ...form, notes: e.target.value })}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all resize-none"
          />
        </div>

        <div className="flex gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
          <Button variant="outline" onClick={onClose} className="flex-1">Cancel</Button>
          <Button
            className="flex-1"
            isLoading={mutation.isPending}
            disabled={isOverpayment || !form.amount}
            onClick={() => {
              setError(null);
              mutation.mutate(form);
            }}
          >
            Record Payment
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export function PaymentsPage() {
  const [tab, setTab] = useState<'sales' | 'purchases'>('sales');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING_AND_PARTIAL');
  const [page, setPage] = useState(0);
  const [selectedRecord, setSelectedRecord] = useState<PaymentRecord | null>(null);
  const pageSize = 15;

  // Fetch outstanding sales
  const { data: salesPage, isLoading: salesLoading } = useQuery<PageResponse<Sale>>({
    queryKey: ['payments-sales', search, statusFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (statusFilter === 'PENDING_AND_PARTIAL') {
        params.set('status', 'PENDING');
      } else if (statusFilter !== 'ALL') {
        params.set('status', statusFilter);
      }
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'saleDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Sale>>(`/sales?${params.toString()}`);
    },
    enabled: tab === 'sales',
  });

  // Fetch outstanding purchases
  const { data: purchasesPage, isLoading: purchasesLoading } = useQuery<PageResponse<Purchase>>({
    queryKey: ['payments-purchases', search, statusFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (statusFilter === 'PENDING_AND_PARTIAL') {
        params.set('status', 'PENDING');
      } else if (statusFilter !== 'ALL') {
        params.set('status', statusFilter);
      }
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'purchaseDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Purchase>>(`/purchases?${params.toString()}`);
    },
    enabled: tab === 'purchases',
  });

  const isLoading = tab === 'sales' ? salesLoading : purchasesLoading;
  const pageData = tab === 'sales' ? salesPage : purchasesPage;
  const totalPages = pageData?.totalPages || 0;
  const totalElements = pageData?.totalElements || 0;

  // Build unified records
  const records: PaymentRecord[] = tab === 'sales'
    ? (salesPage?.content || []).map(s => ({
        id: s.id,
        referenceNumber: s.saleNumber,
        partyName: s.customerName,
        type: 'sale',
        date: s.saleDate,
        grandTotal: s.grandTotal,
        amountPaid: s.amountPaid,
        balanceDue: s.balanceDue,
        paymentStatus: s.paymentStatus,
        paymentMethod: s.paymentMethod,
      }))
    : (purchasesPage?.content || []).map(p => ({
        id: p.id,
        referenceNumber: p.purchaseNumber,
        partyName: p.supplierName,
        type: 'purchase',
        date: p.purchaseDate,
        grandTotal: p.grandTotal,
        amountPaid: p.amountPaid,
        balanceDue: p.balanceDue,
        paymentStatus: p.paymentStatus,
        paymentMethod: p.paymentMethod,
      }));

  const totalOutstanding = records.reduce((sum, r) => sum + r.balanceDue, 0);
  const pendingCount = records.filter(r => r.paymentStatus === 'PENDING').length;
  const partialCount = records.filter(r => r.paymentStatus === 'PARTIALLY_PAID').length;

  const getStatusBadge = (status: string) => {
    if (status === 'PAID') return <Badge variant="success">Paid</Badge>;
    if (status === 'PARTIALLY_PAID') return <Badge variant="warning">Partial</Badge>;
    return <Badge variant="danger">Pending</Badge>;
  };

  const getMethodLabel = (method?: PaymentMethod) => {
    if (!method) return '—';
    return PAYMENT_METHODS.find(m => m.value === method)?.label || method;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <CreditCard className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Payment Records
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Record customer receipts and vendor payments. Track outstanding balances and payment history.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="p-4 bg-gradient-to-br from-rose-50/50 to-white dark:from-rose-950/20 dark:to-slate-900 border-rose-100 dark:border-rose-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Outstanding
              </p>
              <h3 className="text-2xl font-bold text-rose-600 dark:text-rose-400 mt-1">
                {formatCurrency(totalOutstanding)}
              </h3>
              <p className="text-[11px] text-slate-500 mt-1">On current page</p>
            </div>
            <div className="p-2.5 rounded-xl bg-rose-600/10 text-rose-600 dark:text-rose-400">
              <IndianRupee className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 border-amber-100 dark:border-amber-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Fully Pending
              </p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">{pendingCount}</h3>
              <p className="text-[11px] text-slate-500 mt-1">On current page</p>
            </div>
            <div className="p-2.5 rounded-xl bg-amber-600/10 text-amber-600 dark:text-amber-400">
              {tab === 'sales' ? <ArrowDownLeft className="w-5 h-5" /> : <ArrowUpRight className="w-5 h-5" />}
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Partially Paid
              </p>
              <h3 className="text-2xl font-bold text-indigo-600 dark:text-indigo-400 mt-1">{partialCount}</h3>
              <p className="text-[11px] text-slate-500 mt-1">On current page</p>
            </div>
            <div className="p-2.5 rounded-xl bg-indigo-600/10 text-indigo-600 dark:text-indigo-400">
              <Banknote className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Tabs + Filters */}
      <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
        {/* Tab Toggle */}
        <div className="flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1 gap-1">
          <button
            onClick={() => { setTab('sales'); setPage(0); }}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              tab === 'sales'
                ? 'bg-white dark:bg-slate-700 text-indigo-700 dark:text-indigo-300 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <TrendingUp className="w-3.5 h-3.5" /> Customer Receivables
          </button>
          <button
            onClick={() => { setTab('purchases'); setPage(0); }}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              tab === 'purchases'
                ? 'bg-white dark:bg-slate-700 text-indigo-700 dark:text-indigo-300 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <TrendingDown className="w-3.5 h-3.5" /> Vendor Payables
          </button>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder={tab === 'sales' ? 'Search sales...' : 'Search purchases...'}
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(0); }}
              className="w-full pl-9 pr-4 py-2 text-xs rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
            className="px-3 py-2 text-xs rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="PENDING_AND_PARTIAL">Unpaid Only</option>
            <option value="ALL">All Records</option>
            <option value="PAID">Fully Paid</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto mb-2" />
            <p className="text-xs text-slate-500">Loading payment records...</p>
          </div>
        ) : records.length === 0 ? (
          <div className="p-12 text-center">
            <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-white">
              {statusFilter === 'PENDING_AND_PARTIAL' ? 'All Payments Cleared!' : 'No Records Found'}
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              {statusFilter === 'PENDING_AND_PARTIAL'
                ? `No outstanding ${tab === 'sales' ? 'receivables' : 'payables'} at this time.`
                : `No ${tab} records match your filters.`}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">{tab === 'sales' ? 'Sale #' : 'Purchase #'}</th>
                  <th className="py-3 px-4">{tab === 'sales' ? 'Customer' : 'Supplier'}</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-right">Grand Total</th>
                  <th className="py-3 px-4 text-right">Paid</th>
                  <th className="py-3 px-4 text-right">Balance Due</th>
                  <th className="py-3 px-4 text-center">Status</th>
                  <th className="py-3 px-4">Method</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {records.map(r => (
                  <tr key={r.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="py-3 px-4 font-semibold text-indigo-600 dark:text-indigo-400">
                      {r.referenceNumber}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-900 dark:text-white max-w-[160px] truncate">
                      {r.partyName}
                    </td>
                    <td className="py-3 px-4 text-slate-500 dark:text-slate-400 whitespace-nowrap">
                      {formatDate(r.date)}
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-slate-900 dark:text-white">
                      {formatCurrency(r.grandTotal)}
                    </td>
                    <td className="py-3 px-4 text-right text-emerald-600 dark:text-emerald-400 font-medium">
                      {formatCurrency(r.amountPaid)}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-rose-600 dark:text-rose-400">
                      {r.balanceDue > 0 ? formatCurrency(r.balanceDue) : <span className="text-emerald-600">—</span>}
                    </td>
                    <td className="py-3 px-4 text-center">{getStatusBadge(r.paymentStatus)}</td>
                    <td className="py-3 px-4 text-slate-500 dark:text-slate-400 text-[11px]">
                      {getMethodLabel(r.paymentMethod)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {r.balanceDue > 0 ? (
                        <Button
                          size="sm"
                          onClick={() => setSelectedRecord(r)}
                          className="text-xs px-3 h-7"
                        >
                          Record Payment
                        </Button>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                          <CheckCircle2 className="w-3.5 h-3.5" /> Cleared
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-slate-200 dark:border-slate-800 text-xs">
            <span className="text-slate-500">
              Page <span className="font-semibold text-slate-900 dark:text-white">{page + 1}</span> of{' '}
              <span className="font-semibold text-slate-900 dark:text-white">{totalPages}</span> ({totalElements} records)
            </span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
                <ChevronLeft className="w-4 h-4 mr-1" /> Previous
              </Button>
              <Button variant="outline" size="sm" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
                Next <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Payment Modal */}
      {selectedRecord && (
        <PaymentModal
          record={selectedRecord}
          onClose={() => setSelectedRecord(null)}
        />
      )}
    </div>
  );
}
