import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Supplier, Purchase, PageResponse } from '../../types';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { formatCurrency, formatDate } from '../../lib/utils';
import {
  BookOpen, ChevronLeft, ChevronRight, CheckCircle2,
  Phone, Mail, MapPin, Building2, FileText, IndianRupee,
} from 'lucide-react';

interface SupplierLedgerProps {
  supplier: Supplier;
  onClose: () => void;
}

export function SupplierLedgerModal({ supplier, onClose }: SupplierLedgerProps) {
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const { data: purchasesPage, isLoading } = useQuery<PageResponse<Purchase>>({
    queryKey: ['supplier-ledger', supplier.id, page],
    queryFn: () => {
      const params = new URLSearchParams();
      params.set('supplierId', supplier.id);
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'purchaseDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Purchase>>(`/purchases?${params.toString()}`);
    },
  });

  const purchases = purchasesPage?.content ?? [];
  const totalPages = purchasesPage?.totalPages ?? 0;
  const totalElements = purchasesPage?.totalElements ?? 0;

  // Compute totals across all records shown
  const totalBilled = purchases.reduce((s, x) => s + x.grandTotal, 0);
  const totalPaid = purchases.reduce((s, x) => s + x.amountPaid, 0);
  const totalDue = purchases.reduce((s, x) => s + x.balanceDue, 0);

  const getStatusBadge = (status: string) => {
    if (status === 'PAID') return <Badge variant="success">Paid</Badge>;
    if (status === 'PARTIALLY_PAID') return <Badge variant="warning">Partial</Badge>;
    return <Badge variant="danger">Pending</Badge>;
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={`Supplier Ledger — ${supplier.name}`}
      description="Complete purchase bills and payment history for this vendor"
      className="max-w-4xl"
    >
      {/* Supplier Info Strip */}
      <div className="flex flex-wrap gap-4 p-4 mb-5 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs">
        <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
          <Building2 className="w-3.5 h-3.5 text-indigo-500" />
          <span className="font-semibold">{supplier.name}</span>
        </div>
        {supplier.phone && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <Phone className="w-3.5 h-3.5" /> {supplier.phone}
          </div>
        )}
        {supplier.email && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <Mail className="w-3.5 h-3.5" /> {supplier.email}
          </div>
        )}
        {supplier.gstin && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <FileText className="w-3.5 h-3.5" /> GSTIN: <span className="font-mono">{supplier.gstin}</span>
          </div>
        )}
        {(supplier.city || supplier.stateName) && (
          <div className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
            <MapPin className="w-3.5 h-3.5" /> {[supplier.city, supplier.stateName].filter(Boolean).join(', ')}
          </div>
        )}
      </div>

      {/* Summary KPIs */}
      <div className="grid grid-cols-3 gap-3 mb-5">
        <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-center">
          <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-1">Total Purchases</p>
          <p className="text-lg font-bold text-slate-900 dark:text-white tabular-nums">{formatCurrency(totalBilled)}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">{totalElements} bill(s) on this page</p>
        </div>
        <div className="p-3.5 rounded-xl border border-emerald-100 dark:border-emerald-900/50 bg-emerald-50/50 dark:bg-emerald-950/20 text-center">
          <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-500 mb-1">Amount Paid Out</p>
          <p className="text-lg font-bold text-emerald-700 dark:text-emerald-300 tabular-nums">{formatCurrency(totalPaid)}</p>
          <p className="text-[10px] text-emerald-600/60 dark:text-emerald-400/60 mt-0.5">Payments cleared</p>
        </div>
        <div className={`p-3.5 rounded-xl border text-center ${totalDue > 0 ? 'border-rose-100 dark:border-rose-900/50 bg-rose-50/50 dark:bg-rose-950/20' : 'border-emerald-100 dark:border-emerald-900/50 bg-emerald-50/50 dark:bg-emerald-950/20'}`}>
          <p className={`text-[10px] font-bold uppercase tracking-wider mb-1 ${totalDue > 0 ? 'text-rose-500' : 'text-emerald-500'}`}>Payable to Vendor</p>
          <p className={`text-lg font-bold tabular-nums ${totalDue > 0 ? 'text-rose-700 dark:text-rose-300' : 'text-emerald-700 dark:text-emerald-300'}`}>{formatCurrency(totalDue)}</p>
          <p className="text-[10px] text-slate-400 mt-0.5">{totalDue > 0 ? 'Pending payout' : 'All settled!'}</p>
        </div>
      </div>

      {/* Lifetime Outstanding Balance from Supplier entity */}
      {supplier.outstandingBalance > 0 && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 text-xs">
          <IndianRupee className="w-4 h-4 text-amber-600 shrink-0" />
          <span className="text-amber-700 dark:text-amber-300">
            <strong>Total lifetime balance payable:</strong> {formatCurrency(Number(supplier.outstandingBalance))} — Go to{' '}
            <span className="font-semibold">Payments → Vendor Payables</span> to record an outgoing payment.
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
        ) : purchases.length === 0 ? (
          <div className="p-10 text-center">
            <BookOpen className="w-10 h-10 text-slate-300 dark:text-slate-700 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-600 dark:text-slate-400">No Purchases Found</p>
            <p className="text-xs text-slate-400 mt-1">This vendor has no recorded purchase orders or bills yet.</p>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider text-left">
              <tr>
                <th className="py-3 px-4">Purchase #</th>
                <th className="py-3 px-4">Date</th>
                <th className="py-3 px-4 text-right">Total</th>
                <th className="py-3 px-4 text-right">Paid</th>
                <th className="py-3 px-4 text-right">Balance</th>
                <th className="py-3 px-4 text-center">Status</th>
                <th className="py-3 px-4">Method</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {purchases.map(p => (
                <tr key={p.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 font-semibold text-indigo-600 dark:text-indigo-400">
                    {p.purchaseNumber}
                  </td>
                  <td className="py-3 px-4 text-slate-500 dark:text-slate-400 whitespace-nowrap">
                    {formatDate(p.purchaseDate)}
                  </td>
                  <td className="py-3 px-4 text-right font-semibold text-slate-900 dark:text-white">
                    {formatCurrency(p.grandTotal)}
                  </td>
                  <td className="py-3 px-4 text-right text-emerald-600 dark:text-emerald-400 font-medium">
                    {formatCurrency(p.amountPaid)}
                  </td>
                  <td className="py-3 px-4 text-right font-bold">
                    {p.balanceDue > 0
                      ? <span className="text-rose-600 dark:text-rose-400">{formatCurrency(p.balanceDue)}</span>
                      : <span className="text-emerald-600 dark:text-emerald-400 flex items-center justify-end gap-0.5"><CheckCircle2 className="w-3 h-3" /> Settled</span>
                    }
                  </td>
                  <td className="py-3 px-4 text-center">{getStatusBadge(p.paymentStatus)}</td>
                  <td className="py-3 px-4 text-slate-500 dark:text-slate-400">
                    {p.paymentMethod?.replace('_', ' ') || '—'}
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
