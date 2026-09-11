import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Purchase, PurchaseSummary, PageResponse, PaymentStatus } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { PurchaseModal } from './PurchaseModal';
import { PurchaseDetailModal } from './PurchaseDetailModal';
import { formatCurrency } from '../../lib/utils';
import {
  ShoppingBag,
  Plus,
  Search,
  Eye,
  ChevronLeft,
  ChevronRight,
  TrendingDown,
  FileCheck2,
  Clock
} from 'lucide-react';

export function PurchasesPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [selectedPurchase, setSelectedPurchase] = useState<Purchase | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Summary statistics query
  const { data: summary } = useQuery<PurchaseSummary>({
    queryKey: ['purchases-summary'],
    queryFn: () => api.get<PurchaseSummary>('/purchases/summary'),
  });

  // Paginated purchases query
  const { data: pageData, isLoading } = useQuery<PageResponse<Purchase>>({
    queryKey: ['purchases', search, statusFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (statusFilter !== 'ALL') params.set('status', statusFilter);
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'purchaseDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Purchase>>(`/purchases?${params.toString()}`);
    },
  });

  const purchases = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  const getStatusBadge = (status: PaymentStatus) => {
    switch (status) {
      case 'PAID':
        return <Badge variant="success">Paid</Badge>;
      case 'PARTIALLY_PAID':
        return <Badge variant="warning">Partially Paid</Badge>;
      case 'PENDING':
        return <Badge variant="danger">Pending</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <ShoppingBag className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Purchase Management
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Record supplier invoice bills, input GST credits, vendor line items, and accounts payable.
          </p>
        </div>
        <div>
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
          >
            <Plus className="w-4 h-4" />
            Record Purchase Bill
          </Button>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Purchases
              </p>
              <h3 className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {formatCurrency(summary?.totalPurchasesAmount || 0)}
              </h3>
            </div>
            <div className="p-2.5 bg-indigo-500/10 rounded-xl text-indigo-600 dark:text-indigo-400">
              <ShoppingBag className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 border-amber-100 dark:border-amber-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Outstanding Payables
              </p>
              <h3 className="text-xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {formatCurrency(summary?.totalOutstandingPayable || 0)}
              </h3>
            </div>
            <div className="p-2.5 bg-amber-500/10 rounded-xl text-amber-600 dark:text-amber-400">
              <TrendingDown className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-blue-50/50 to-white dark:from-blue-950/20 dark:to-slate-900 border-blue-100 dark:border-blue-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Purchase Bills
              </p>
              <h3 className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {summary?.totalPurchasesCount || 0}
              </h3>
            </div>
            <div className="p-2.5 bg-blue-500/10 rounded-xl text-blue-600 dark:text-blue-400">
              <FileCheck2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-rose-50/50 to-white dark:from-rose-950/20 dark:to-slate-900 border-rose-100 dark:border-rose-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Unpaid / Pending Bills
              </p>
              <h3 className="text-xl font-bold text-rose-600 dark:text-rose-400 mt-1">
                {summary?.pendingPurchasesCount || 0}
              </h3>
            </div>
            <div className="p-2.5 bg-rose-500/10 rounded-xl text-rose-600 dark:text-rose-400">
              <Clock className="w-5 h-5" />
            </div>
          </div>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by bill # or supplier name..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full pl-9 pr-4 py-2 text-xs rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs text-slate-500 dark:text-slate-400">Status:</span>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 text-xs rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="PAID">Paid</option>
            <option value="PARTIALLY_PAID">Partially Paid</option>
            <option value="PENDING">Pending</option>
          </select>
        </div>
      </div>

      {/* Purchases Data Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto mb-2"></div>
            Loading purchase records...
          </div>
        ) : purchases.length === 0 ? (
          <div className="p-12 text-center">
            <ShoppingBag className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-white">No Purchase Records Found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Start by recording your first vendor purchase bill to automatically update inventory stock and track input GST.
            </p>
            <Button onClick={() => setIsCreateModalOpen(true)} className="mt-4" size="sm">
              <Plus className="w-4 h-4 mr-1.5" /> Record First Purchase
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Purchase Bill #</th>
                  <th className="py-3 px-4">Supplier</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-right">Taxable Amt</th>
                  <th className="py-3 px-4 text-right">Input GST</th>
                  <th className="py-3 px-4 text-right">Grand Total</th>
                  <th className="py-3 px-4 text-right">Balance Due</th>
                  <th className="py-3 px-4 text-center">Payment</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {purchases.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="py-3 px-4 font-semibold text-indigo-600 dark:text-indigo-400">
                      {p.purchaseNumber}
                      {p.supplierInvoiceNumber && (
                        <span className="block text-[10px] font-normal text-slate-400">Ref: {p.supplierInvoiceNumber}</span>
                      )}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-900 dark:text-white">
                      {p.supplierName}
                      {p.supplierGstin && (
                        <span className="block text-[10px] font-normal text-slate-400">GSTIN: {p.supplierGstin}</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-600 dark:text-slate-400 whitespace-nowrap">
                      {p.purchaseDate}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-700 dark:text-slate-300">
                      {formatCurrency(p.taxableAmount)}
                    </td>
                    <td className="py-3 px-4 text-right text-indigo-600 dark:text-indigo-400 font-medium">
                      {formatCurrency(p.totalTax)}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-slate-900 dark:text-white">
                      {formatCurrency(p.grandTotal)}
                    </td>
                    <td className={`py-3 px-4 text-right font-semibold ${p.balanceDue > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-slate-500'}`}>
                      {formatCurrency(p.balanceDue)}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {getStatusBadge(p.paymentStatus)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setSelectedPurchase(p);
                          setIsDetailModalOpen(true);
                        }}
                        className="h-8 px-2"
                      >
                        <Eye className="w-4 h-4 mr-1 text-slate-500" /> View
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between p-4 border-t border-slate-200 dark:border-slate-800 text-xs">
            <span className="text-slate-500">
              Showing page <span className="font-semibold text-slate-900 dark:text-white">{page + 1}</span> of{' '}
              <span className="font-semibold text-slate-900 dark:text-white">{totalPages}</span> ({totalElements} total bills)
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                disabled={page === 0}
              >
                <ChevronLeft className="w-4 h-4 mr-1" /> Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
                disabled={page >= totalPages - 1}
              >
                Next <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Modals */}
      <PurchaseModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <PurchaseDetailModal
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setSelectedPurchase(null);
        }}
        purchase={selectedPurchase}
      />
    </div>
  );
}
