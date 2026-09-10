import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import {
  InventoryTransaction,
  InventorySummary,
  InventoryTransactionType,
  PageResponse,
  Product,
} from '../../types';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { StockAdjustmentModal } from './StockAdjustmentModal';
import { formatCurrency } from '../../lib/utils';
import {
  Boxes,
  ArrowUpDown,
  Search,
  AlertTriangle,
  AlertOctagon,
  ArrowUpRight,
  ArrowDownRight,
  RefreshCw,
  Clock,
  Layers,
  TrendingUp,
  History,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

const TYPE_CONFIG: Record<
  InventoryTransactionType,
  { label: string; badgeVariant: 'success' | 'info' | 'warning' | 'danger' | 'default'; isInflow: boolean }
> = {
  PURCHASE: { label: 'Purchase In', badgeVariant: 'success', isInflow: true },
  SALE: { label: 'Sale Out', badgeVariant: 'info', isInflow: false },
  RETURN_IN: { label: 'Customer Return', badgeVariant: 'success', isInflow: true },
  RETURN_OUT: { label: 'Supplier Return', badgeVariant: 'warning', isInflow: false },
  ADJUSTMENT: { label: 'Adjustment', badgeVariant: 'warning', isInflow: true },
  OPENING_STOCK: { label: 'Opening Balance', badgeVariant: 'default', isInflow: true },
};

export function InventoryPage() {
  const [activeTab, setActiveTab] = useState<'ledger' | 'alerts'>('ledger');
  const [search, setSearch] = useState('');
  const [selectedType, setSelectedType] = useState<string>('');
  const [page, setPage] = useState(0);
  const pageSize = 12;

  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [selectedProductForAdjust, setSelectedProductForAdjust] = useState<Product | null>(null);

  // 1. Fetch Inventory Summary KPIs
  const { data: summary, isLoading: isSummaryLoading } = useQuery<InventorySummary>({
    queryKey: ['inventory', 'summary'],
    queryFn: () => api.get<InventorySummary>('/inventory/summary'),
  });

  // 2. Fetch All Products (for adjustment selector & alerts)
  const { data: productsData } = useQuery<PageResponse<Product>>({
    queryKey: ['products', 'all-inventory'],
    queryFn: () => api.get<PageResponse<Product>>('/products?size=100&activeOnly=true'),
  });
  const allProducts = productsData?.content || [];

  // 3. Fetch Inventory Ledger Transactions
  const { data: ledgerData, isLoading: isLedgerLoading } = useQuery<PageResponse<InventoryTransaction>>({
    queryKey: ['inventory', 'ledger', selectedType, page],
    queryFn: () => {
      const params = new URLSearchParams();
      if (selectedType) params.set('type', selectedType);
      params.set('page', page.toString());
      params.set('size', pageSize.toString());
      return api.get<PageResponse<InventoryTransaction>>(`/inventory/ledger?${params.toString()}`);
    },
    enabled: activeTab === 'ledger',
  });

  // 4. Fetch Low Stock Alerts
  const { data: lowStockData, isLoading: isAlertsLoading } = useQuery<PageResponse<Product>>({
    queryKey: ['inventory', 'low-stock', page],
    queryFn: () => api.get<PageResponse<Product>>(`/inventory/alerts/low-stock?page=${page}&size=${pageSize}`),
    enabled: activeTab === 'alerts',
  });

  const transactions = ledgerData?.content || [];
  const alertProducts = lowStockData?.content || [];

  // Client-side search filtering on ledger product name / sku / ref
  const filteredTransactions = transactions.filter((tx) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      tx.productName.toLowerCase().includes(q) ||
      tx.productSku.toLowerCase().includes(q) ||
      (tx.referenceNumber && tx.referenceNumber.toLowerCase().includes(q)) ||
      (tx.notes && tx.notes.toLowerCase().includes(q))
    );
  });

  const handleOpenAdjustModal = (product?: Product) => {
    setSelectedProductForAdjust(product || null);
    setIsAdjustModalOpen(true);
  };

  const formatDate = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white flex items-center gap-2.5">
            <Boxes className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Inventory & Stock Ledger
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Real-time stock movement audit trail, transactional ledger, and automated replenishment alerts.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            onClick={() => handleOpenAdjustModal()}
            className="flex items-center gap-1.5 shadow-sm"
          >
            <ArrowUpDown className="w-4 h-4" />
            Adjust Stock
          </Button>
        </div>
      </div>

      {/* KPI Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-900/50 border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Inventory Valuation
            </span>
            <div className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-slate-900 dark:text-white">
              {isSummaryLoading ? '...' : formatCurrency(summary?.totalValuation || 0)}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">
              Across {summary?.totalProductsCount || 0} catalog products
            </div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-900/50 border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Total Units In Stock
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-slate-900 dark:text-white">
              {isSummaryLoading ? '...' : (summary?.totalUnitsInStock || 0).toLocaleString('en-IN')}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Physical items on hand</div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-900/50 border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Low Stock Warnings
            </span>
            <div className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
              {isSummaryLoading ? '...' : summary?.lowStockCount || 0}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Items at or below minimum threshold</div>
          </div>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-white to-slate-50 dark:from-slate-900 dark:to-slate-900/50 border-slate-200 dark:border-slate-800">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
              Out of Stock
            </span>
            <div className="p-2 rounded-xl bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400">
              <AlertOctagon className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-rose-600 dark:text-rose-400">
              {isSummaryLoading ? '...' : summary?.outOfStockCount || 0}
            </div>
            <div className="text-[11px] text-slate-500 mt-0.5">Items needing urgent procurement</div>
          </div>
        </Card>
      </div>

      {/* Tabs Bar */}
      <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800">
        <button
          onClick={() => {
            setActiveTab('ledger');
            setPage(0);
          }}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 transition-all relative ${
            activeTab === 'ledger'
              ? 'text-indigo-600 dark:text-indigo-400 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-indigo-600 dark:after:bg-indigo-400'
              : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <History className="w-4 h-4" />
          Stock Movement Ledger
        </button>

        <button
          onClick={() => {
            setActiveTab('alerts');
            setPage(0);
          }}
          className={`pb-3 text-xs font-semibold flex items-center gap-2 transition-all relative ${
            activeTab === 'alerts'
              ? 'text-indigo-600 dark:text-indigo-400 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-indigo-600 dark:after:bg-indigo-400'
              : 'text-slate-500 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <AlertTriangle className="w-4 h-4 text-amber-500" />
          Replenishment Alerts
          {summary && summary.lowStockCount > 0 && (
            <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-400">
              {summary.lowStockCount}
            </span>
          )}
        </button>
      </div>

      {/* TAB 1: LEDGER VIEW */}
      {activeTab === 'ledger' && (
        <div className="space-y-4">
          {/* Filters Bar */}
          <Card className="p-3.5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="relative flex-1 max-w-md">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Filter ledger by product name, SKU, reference..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full pl-9 pr-3 py-1.5 text-xs rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex items-center gap-2.5">
                <select
                  value={selectedType}
                  onChange={(e) => {
                    setSelectedType(e.target.value);
                    setPage(0);
                  }}
                  className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="">All Movement Types</option>
                  <option value="PURCHASE">Purchases (Stock In)</option>
                  <option value="SALE">Sales (Stock Out)</option>
                  <option value="ADJUSTMENT">Manual Adjustments</option>
                  <option value="RETURN_IN">Customer Returns (In)</option>
                  <option value="RETURN_OUT">Supplier Returns (Out)</option>
                  <option value="OPENING_STOCK">Opening Stock</option>
                </select>
              </div>
            </div>
          </Card>

          {/* Ledger Table */}
          <Card className="overflow-hidden">
            {isLedgerLoading ? (
              <div className="p-12 flex items-center justify-center">
                <RefreshCw className="w-6 h-6 animate-spin text-indigo-600" />
              </div>
            ) : filteredTransactions.length === 0 ? (
              <div className="text-center py-12">
                <History className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto mb-2" />
                <h3 className="text-sm font-semibold text-slate-900 dark:text-white">
                  No stock transactions found
                </h3>
                <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                  Every product opening stock, purchase, sale, and adjustment automatically creates an
                  immutable audit ledger entry here.
                </p>
                <div className="mt-4">
                  <Button onClick={() => handleOpenAdjustModal()} className="gap-1.5">
                    <ArrowUpDown className="w-4 h-4" /> Record First Adjustment
                  </Button>
                </div>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                    <tr>
                      <th className="px-4 py-3">Date & Time</th>
                      <th className="px-4 py-3">Product & SKU</th>
                      <th className="px-4 py-3">Movement Type</th>
                      <th className="px-4 py-3">Qty Change</th>
                      <th className="px-4 py-3">Stock Transition</th>
                      <th className="px-4 py-3">Unit Valuation</th>
                      <th className="px-4 py-3">Reason / Reference</th>
                      <th className="px-4 py-3 text-right">Operator</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {filteredTransactions.map((tx) => {
                      const cfg = TYPE_CONFIG[tx.transactionType] || {
                        label: tx.transactionType,
                        badgeVariant: 'default',
                        isInflow: true,
                      };
                      const delta = Number(tx.newStock) - Number(tx.previousStock);
                      const isPositiveDelta = delta >= 0;

                      return (
                        <tr
                          key={tx.id}
                          className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                        >
                          <td className="px-4 py-3 text-slate-500 font-mono text-[11px] whitespace-nowrap">
                            <div className="flex items-center gap-1.5">
                              <Clock className="w-3.5 h-3.5 text-slate-400" />
                              {formatDate(tx.createdAt)}
                            </div>
                          </td>

                          <td className="px-4 py-3">
                            <div className="font-semibold text-slate-900 dark:text-white">
                              {tx.productName}
                            </div>
                            <div className="flex items-center gap-1.5 mt-0.5">
                              <span className="font-mono text-[11px] px-1.5 py-0.2 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">
                                {tx.productSku}
                              </span>
                              {tx.categoryName && (
                                <span className="text-[10px] text-slate-400">· {tx.categoryName}</span>
                              )}
                            </div>
                          </td>

                          <td className="px-4 py-3">
                            <Badge variant={cfg.badgeVariant}>{cfg.label}</Badge>
                          </td>

                          <td className="px-4 py-3">
                            <div
                              className={`font-semibold flex items-center gap-1 ${
                                isPositiveDelta
                                  ? 'text-emerald-600 dark:text-emerald-400'
                                  : 'text-rose-600 dark:text-rose-400'
                              }`}
                            >
                              {isPositiveDelta ? (
                                <ArrowUpRight className="w-3.5 h-3.5" />
                              ) : (
                                <ArrowDownRight className="w-3.5 h-3.5" />
                              )}
                              {isPositiveDelta ? `+${tx.quantity}` : `-${tx.quantity}`} {tx.unit}
                            </div>
                          </td>

                          <td className="px-4 py-3">
                            <div className="flex items-center gap-1.5 text-slate-700 dark:text-slate-300 font-mono">
                              <span className="text-slate-400">{tx.previousStock}</span>
                              <span className="text-slate-300 dark:text-slate-600">→</span>
                              <span className="font-bold text-slate-900 dark:text-white">
                                {tx.newStock}
                              </span>
                            </div>
                          </td>

                          <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                            <div>{formatCurrency(tx.unitCost)} / {tx.unit}</div>
                            {tx.totalCost > 0 && (
                              <div className="text-[10px] text-slate-400 font-mono">
                                Total: {formatCurrency(tx.totalCost)}
                              </div>
                            )}
                          </td>

                          <td className="px-4 py-3 text-slate-600 dark:text-slate-300 max-w-xs truncate">
                            <div className="font-medium text-slate-900 dark:text-slate-200">
                              {tx.notes || '—'}
                            </div>
                            {tx.referenceNumber && (
                              <div className="text-[10px] text-slate-400 font-mono">
                                Ref: {tx.referenceNumber}
                              </div>
                            )}
                          </td>

                          <td className="px-4 py-3 text-right text-slate-500 whitespace-nowrap">
                            <div className="inline-flex items-center gap-1 text-[11px]">
                              <ShieldCheck className="w-3 h-3 text-emerald-500" />
                              {tx.createdByName || 'System'}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>

                {/* Pagination Controls */}
                {ledgerData && ledgerData.totalPages > 1 && (
                  <div className="p-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                    <div>
                      Page {page + 1} of {ledgerData.totalPages} ({ledgerData.totalElements} records)
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page === 0}
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                      >
                        <ChevronLeft className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={page >= ledgerData.totalPages - 1}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        <ChevronRight className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </Card>
        </div>
      )}

      {/* TAB 2: REPLENISHMENT ALERTS VIEW */}
      {activeTab === 'alerts' && (
        <Card className="overflow-hidden">
          {isAlertsLoading ? (
            <div className="p-12 flex items-center justify-center">
              <RefreshCw className="w-6 h-6 animate-spin text-indigo-600" />
            </div>
          ) : alertProducts.length === 0 ? (
            <div className="text-center py-12">
              <ShieldCheck className="w-12 h-12 text-emerald-500 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-slate-900 dark:text-white">
                All stock levels are optimal!
              </h3>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                No products are currently at or below their configured minimum replenishment thresholds.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 dark:bg-slate-900/60 text-slate-500 uppercase tracking-wider font-semibold border-b border-slate-200 dark:border-slate-800">
                  <tr>
                    <th className="px-4 py-3">Product Name & SKU</th>
                    <th className="px-4 py-3">Category</th>
                    <th className="px-4 py-3">Current Stock</th>
                    <th className="px-4 py-3">Min Threshold</th>
                    <th className="px-4 py-3">Status Alert</th>
                    <th className="px-4 py-3">Restock Deficit</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {alertProducts.map((p) => {
                    const isZero = Number(p.currentStock) <= 0;
                    const deficit = Math.max(0, Number(p.minStockLevel) - Number(p.currentStock));

                    return (
                      <tr
                        key={p.id}
                        className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors"
                      >
                        <td className="px-4 py-3">
                          <div className="font-semibold text-slate-900 dark:text-white">{p.name}</div>
                          <div className="text-[11px] font-mono text-slate-500">{p.sku}</div>
                        </td>
                        <td className="px-4 py-3 text-slate-600 dark:text-slate-300">
                          {p.categoryName || 'General'}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`font-bold font-mono ${
                              isZero ? 'text-rose-600 dark:text-rose-400' : 'text-amber-600 dark:text-amber-400'
                            }`}
                          >
                            {p.currentStock} {p.unit}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-mono text-slate-500">
                          {p.minStockLevel} {p.unit}
                        </td>
                        <td className="px-4 py-3">
                          {isZero ? (
                            <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-rose-50 dark:bg-rose-950/60 text-rose-700 dark:text-rose-400 border border-rose-200 dark:border-rose-900">
                              <AlertOctagon className="w-3 h-3" /> Out of Stock
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-400 border border-amber-200 dark:border-amber-900">
                              <AlertTriangle className="w-3 h-3" /> Low Stock
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 font-mono text-xs font-semibold text-slate-700 dark:text-slate-300">
                          +{deficit} {p.unit} needed
                        </td>
                        <td className="px-4 py-3 text-right">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenAdjustModal(p)}
                            className="gap-1.5"
                          >
                            <ArrowUpDown className="w-3.5 h-3.5 text-indigo-600" />
                            Quick Adjust
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {/* Stock Adjustment Modal */}
      <StockAdjustmentModal
        isOpen={isAdjustModalOpen}
        onClose={() => {
          setIsAdjustModalOpen(false);
          setSelectedProductForAdjust(null);
        }}
        products={allProducts}
        initialProduct={selectedProductForAdjust}
      />
    </div>
  );
}
