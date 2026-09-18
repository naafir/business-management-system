import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Customer, Sale, PageResponse } from '../../types';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { formatCurrency, formatDate } from '../../lib/utils';
import {
  BookOpen, ChevronLeft, ChevronRight, CheckCircle2,
  Phone, Mail, MapPin, Building2, FileText, IndianRupee,
} from 'lucide-react';

interface CustomerLedgerProps {
  customer: Customer;
  onClose: () => void;
}

export function CustomerLedgerModal({ customer, onClose }: CustomerLedgerProps) {
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const { data: salesPage, isLoading } = useQuery<PageResponse<Sale>>({
    queryKey: ['customer-ledger', customer.id, page],
    queryFn: () => {
      const params = new URLSearchParams();
      params.set('customerId', customer.id);
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'saleDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Sale>>(`/sales?${params.toString()}`);
    },
  });

  const sales = salesPage?.content ?? [];
  const totalPages = salesPage?.totalPages ?? 0;
  const totalElements = salesPage?.totalElements ?? 0;

  // Compute totals across all records shown
  const totalBilled = sales.reduce((s, x) => s + x.grandTotal, 0);
  const totalPaid = sales.reduce((s, x) => s + x.amountPaid, 0);
  const totalDue = sales.reduce((s, x) => s + x.balanceDue, 0);

  const getStatusBadge = (status: string) => {
    if (status === 'PAID') return <Badge variant="success">Paid</Badge>;
    if (status === 'PARTIALLY_PAID') return <Badge variant="warning">Partial</Badge>;
    return <Badge variant="danger">Pending</Badge>;
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={`Customer Ledger — ${customer.name}`}
      description="Complete sales and payment history for this customer"
      className="max-w-4xl"
    >
      {/* Customer Info Strip */}
      <div className="flex flex-wrap gap-4 p-4 mb-5 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs">
        <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
          <Building2 className="w-3.5 h-3.5 text-indigo-500" />
          <span className="font-semibold">{customer.businessName || customer.name}</span>
          <Badge variant={customer.customerType === 'B2B' ? 'info' : 'default'} className="ml-1">
            {customer.customerType}
          </Badge>
        </div>
        {customer.phone && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <Phone className="w-3.5 h-3.5" /> {customer.phone}
          </div>
        )}
        {customer.email && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <Mail className="w-3.5 h-3.5" /> {customer.email}
          </div>
        )}
        {customer.gstin && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <FileText className="w-3.5 h-3.5" /> GSTIN: <span className="font-mono">{customer.gstin}</span>
          </div>
        )}
        {(customer.city || customer.stateName) && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <MapPin className="w-3.5 h-3.5" /> {[customer.city, customer.stateName].filter(Boolean).join(', ')}
          </div>
        )}
      </div>

      {/* Summary KPIs */}
      <div className="grid grid-cols-3 gap-3 mb-5">
        <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-center">
          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-1">Total Billed</p>
          <p className="text-lg font-bold text-slate-900 dark:text-white tabular-nums">{formatCurrency(totalBilled)}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">{totalElements} sale(s) on this page</p>
        </div>
        <div className="p-3.5 rounded-xl border border-emerald-100 dark:border-emerald-900/50 bg-emerald-50/50 dark:bg-emerald-950/20 text-center">
          <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-500 mb-1">Amount Received</p>
          <p className="text-lg font-bold text-emerald-700 dark:text-emerald-300 tabular-nums">{formatCurrency(totalPaid)}</p>
          <p className="text-[10px] text-emerald-600/60 dark:text-emerald-400/60 mt-0.5">Payments recorded</p>
        </div>
        <div className={`p-3.5 rounded-xl border text-center ${totalDue > 0 ? 'border-rose-100 dark:border-rose-900/50 bg-rose-50/50 dark:bg-rose-950/20' : 'border-emerald-100 dark:border-emerald-900/50 bg-emerald-50/50 dark:bg-emerald-950/20'}`}>
          <p className={`text-[10px] font-bold uppercase tracking-wider mb-1 ${totalDue > 0 ? 'text-rose-500' : 'text-emerald-500'}`}>Balance Due</p>
          <p className={`text-lg font-bold tabular-nums ${totalDue > 0 ? 'text-rose-700 dark:text-rose-300' : 'text-emerald-700 dark:text-emerald-300'}`}>{formatCurrency(totalDue)}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">{totalDue > 0 ? 'Outstanding balance' : 'All cleared!'}</p>
        </div>
      </div>

      {/* Outstanding Balance from Customer entity */}
      {customer.outstandingBalance > 0 && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 text-xs">
          <IndianRupee className="w-4 h-4 text-amber-600 shrink-0" />
          <span className="text-amber-700 dark:text-amber-300">
            <strong>Total lifetime outstanding balance:</strong> {formatCurrency(Number(customer.outstandingBalance))} — Go to{' '}
            <span className="font-semibold">Payments → Customer Receivables</span> to record a payment.
          </span>
        </div>
      )}

      {/* Transactions Table */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden">
        {isLoading ? (
          <div className="p-10 text-center">
            <div className="animate-spin w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full mx-auto mb-2" />
            <p className="text-xs text-slate-500">Loading ledger...</p>
          </div>
        ) : sales.length === 0 ? (
          <div className="p-10 text-center">
            <BookOpen className="w-10 h-10 text-slate-300 dark:text-slate-700 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-600 dark:text-slate-400">No Sales Found</p>
            <p className="text-xs text-slate-400 mt-1">This customer has no recorded sales yet.</p>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider text-left">
              <tr>
                <th className="py-3 px-4">Sale #</th>
                <th className="py-3 px-4">Date</th>
                <th className="py-3 px-4 text-right">Total</th>
                <th className="py-3 px-4 text-right">Paid</th>
                <th className="py-3 px-4 text-right">Balance</th>
                <th className="py-3 px-4 text-center">Status</th>
                <th className="py-3 px-4">Method</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {sales.map(s => (
                <tr key={s.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 font-semibold text-indigo-600 dark:text-indigo-400">
                    {s.saleNumber}
                  </td>
                  <td className="py-3 px-4 text-slate-500 dark:text-slate-400 whitespace-nowrap">
                    {formatDate(s.saleDate)}
                  </td>
                  <td className="py-3 px-4 text-right font-semibold text-slate-900 dark:text-white">
                    {formatCurrency(s.grandTotal)}
                  </td>
                  <td className="py-3 px-4 text-right text-emerald-600 dark:text-emerald-400 font-medium">
                    {formatCurrency(s.amountPaid)}
                  </td>
                  <td className="py-3 px-4 text-right font-bold">
                    {s.balanceDue > 0
                      ? <span className="text-rose-600 dark:text-rose-400">{formatCurrency(s.balanceDue)}</span>
                      : <span className="text-emerald-600 dark:text-emerald-400 flex items-center justify-end gap-0.5"><CheckCircle2 className="w-3 h-3" /> Cleared</span>
                    }
                  </td>
                  <td className="py-3 px-4 text-center">{getStatusBadge(s.paymentStatus)}</td>
                  <td className="py-3 px-4 text-slate-500 dark:text-slate-400">
                    {s.paymentMethod?.replace('_', ' ') || '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 text-xs">
          <span className="text-slate-500">
            Page <span className="font-semibold">{page + 1}</span> of{' '}
            <span className="font-semibold">{totalPages}</span> ({totalElements} entries)
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setPage(p => p - 1)} disabled={page === 0}>
              <ChevronLeft className="w-3.5 h-3.5 mr-1" /> Previous
            </Button>
            <Button variant="outline" size="sm" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>
              Next <ChevronRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </div>
        </div>
      )}

      {/* Footer CTA */}
      <div className="flex justify-end mt-5 pt-4 border-t border-slate-100 dark:border-slate-800">
        <Button variant="outline" onClick={onClose}>Close Ledger</Button>
      </div>
    </Modal>
  );
}
