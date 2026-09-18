import { useState } from 'react';
import { toast } from '../../hooks/useToast';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Invoice, InvoiceSummary, PageResponse, InvoiceStatus, PaymentStatus } from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { InvoiceDetailModal } from './InvoiceDetailModal';
import { CreateInvoiceModal } from './CreateInvoiceModal';
import { formatCurrency } from '../../lib/utils';
import {
  FileText,
  Plus,
  Search,
  Eye,
  Download,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  DollarSign,
  TrendingUp,
  Clock,
  Loader2
} from 'lucide-react';

export function InvoicesPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // Summary statistics query
  const { data: summary } = useQuery<InvoiceSummary>({
    queryKey: ['invoices-summary'],
    queryFn: () => api.get<InvoiceSummary>('/invoices/summary'),
  });

  // Paginated invoices query
  const { data: pageData, isLoading } = useQuery<PageResponse<Invoice>>({
    queryKey: ['invoices', search, statusFilter, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (search.trim()) params.set('search', search.trim());
      if (statusFilter !== 'ALL') params.set('status', statusFilter);
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      params.set('sortBy', 'invoiceDate');
      params.set('sortDir', 'desc');
      return api.get<PageResponse<Invoice>>(`/invoices?${params.toString()}`);
    },
  });

  const invoices = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  const getStatusBadge = (status: InvoiceStatus) => {
    switch (status) {
      case 'FINALIZED':
        return <Badge variant="success">Finalized</Badge>;
      case 'DRAFT':
        return <Badge variant="warning">Draft</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">Cancelled</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  const getPaymentBadge = (status: PaymentStatus) => {
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

  const handleDownloadPdf = async (e: React.MouseEvent, inv: Invoice) => {
    e.stopPropagation();
    if (downloadingId) return;
    setDownloadingId(inv.id);
    try {
      const token = localStorage.getItem('auth_token');
      const response = await fetch(`/api/v1/invoices/${inv.id}/pdf`, {
        headers: {
          Authorization: token ? `Bearer ${token}` : '',
        },
      });

      if (!response.ok) throw new Error('PDF generation failed');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice-${inv.invoiceNumber}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success(`Invoice ${inv.invoiceNumber} downloaded successfully!`);
    } catch (err: any) {
      toast.error(err.message || 'Could not download PDF invoice. Please try again.');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <FileText className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Invoice & PDF Generator
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Generate immutable, GST-compliant B2B & B2C tax invoices and download print-ready server-side PDFs.
          </p>
        </div>
        <div>
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            className="flex items-center gap-1.5 shadow-md shadow-indigo-600/20"
          >
            <Plus className="w-4 h-4" />
            Create Tax Invoice
          </Button>
        </div>
      </div>

      {/* KPI Stats Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-indigo-50/50 to-white dark:from-indigo-950/20 dark:to-slate-900 border-indigo-100 dark:border-indigo-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Invoiced
              </p>
              <h3 className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {formatCurrency(summary?.totalInvoicedAmount || 0)}
              </h3>
            </div>
            <div className="p-2.5 bg-indigo-500/10 rounded-xl text-indigo-600 dark:text-indigo-400">
              <DollarSign className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-amber-50/50 to-white dark:from-amber-950/20 dark:to-slate-900 border-amber-100 dark:border-amber-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Outstanding Receivable
              </p>
              <h3 className="text-xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {formatCurrency(summary?.totalOutstandingReceivable || 0)}
              </h3>
            </div>
            <div className="p-2.5 bg-amber-500/10 rounded-xl text-amber-600 dark:text-amber-400">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-emerald-50/50 to-white dark:from-emerald-950/20 dark:to-slate-900 border-emerald-100 dark:border-emerald-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Finalized Invoices
              </p>
              <h3 className="text-xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {summary?.finalizedInvoicesCount || 0}
              </h3>
            </div>
            <div className="p-2.5 bg-emerald-500/10 rounded-xl text-emerald-600 dark:text-emerald-400">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-rose-50/50 to-white dark:from-rose-950/20 dark:to-slate-900 border-rose-100 dark:border-rose-950">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Draft Invoices
              </p>
              <h3 className="text-xl font-bold text-rose-600 dark:text-rose-400 mt-1">
                {summary?.draftInvoicesCount || 0}
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
            placeholder="Search by invoice # or customer..."
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
            <option value="ALL">All Invoices</option>
            <option value="FINALIZED">Finalized</option>
            <option value="DRAFT">Draft</option>
          </select>
        </div>
      </div>

      {/* Invoices Data Table */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-slate-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto mb-2"></div>
            Loading tax invoices...
          </div>
        ) : invoices.length === 0 ? (
          <div className="p-12 text-center">
            <FileText className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-900 dark:text-white">No Tax Invoices Found</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm mx-auto">
              Generate tax invoices from your sales orders or create custom invoices to download official PDF bills.
            </p>
            <Button onClick={() => setIsCreateModalOpen(true)} className="mt-4" size="sm">
              <Plus className="w-4 h-4 mr-1.5" /> Create First Invoice
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Invoice #</th>
                  <th className="py-3 px-4">Customer</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-center">Invoice Status</th>
                  <th className="py-3 px-4 text-right">Taxable Amt</th>
                  <th className="py-3 px-4 text-right">GST Total</th>
                  <th className="py-3 px-4 text-right">Grand Total</th>
                  <th className="py-3 px-4 text-center">Payment</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                {invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="py-3 px-4 font-semibold text-indigo-600 dark:text-indigo-400">
                      {inv.invoiceNumber}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-900 dark:text-white">
                      {inv.customerName}
                      {inv.customerGstin && (
                        <span className="block text-[10px] font-normal text-slate-400">GSTIN: {inv.customerGstin}</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-600 dark:text-slate-400 whitespace-nowrap">
                      {inv.invoiceDate}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {getStatusBadge(inv.status)}
                    </td>
                    <td className="py-3 px-4 text-right text-slate-700 dark:text-slate-300">
                      {formatCurrency(inv.taxableAmount)}
                    </td>
                    <td className="py-3 px-4 text-right text-indigo-600 dark:text-indigo-400 font-medium">
                      {formatCurrency(inv.totalTax)}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-slate-900 dark:text-white">
                      {formatCurrency(inv.grandTotal)}
                    </td>
                    <td className="py-3 px-4 text-center">
                      {getPaymentBadge(inv.paymentStatus)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setSelectedInvoice(inv);
                            setIsDetailModalOpen(true);
                          }}
                          className="h-8 px-2"
                        >
                          <Eye className="w-4 h-4 text-slate-500" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => handleDownloadPdf(e, inv)}
                          className="h-8 px-2 text-indigo-600"
                          disabled={downloadingId === inv.id}
                          title="Download PDF Invoice"
                        >
                          {downloadingId === inv.id
                            ? <Loader2 className="w-4 h-4 animate-spin" />
                            : <Download className="w-4 h-4" />}
                        </Button>
                      </div>
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
              <span className="font-semibold text-slate-900 dark:text-white">{totalPages}</span> ({totalElements} total invoices)
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
      <CreateInvoiceModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <InvoiceDetailModal
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setSelectedInvoice(null);
        }}
        invoice={selectedInvoice}
      />
    </div>
  );
}
