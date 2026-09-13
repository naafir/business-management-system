import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { BusinessSettings, SaleSummary, PurchaseSummary, InventorySummary, InvoiceSummary } from '../../types';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency } from '../../lib/utils';
import {
  TrendingUp,
  TrendingDown,
  Receipt,
  AlertTriangle,
  PlusCircle,
  FileText,
  ShieldCheck,
  Building,
  CheckCircle2,
  ArrowUpRight,
  Package,
  Users,
  Truck,
  BarChart3,
  IndianRupee,
  Clock,
  Activity,
} from 'lucide-react';

function StatCard({
  title,
  value,
  sub,
  subColor,
  icon: Icon,
  iconBg,
  iconColor,
  to,
}: {
  title: string;
  value: string;
  sub: string;
  subColor?: string;
  icon: React.ElementType;
  iconBg: string;
  iconColor: string;
  to?: string;
}) {
  const content = (
    <Card className="hover:border-indigo-500/50 hover:shadow-md transition-all duration-200 group cursor-pointer">
      <CardContent className="p-5 flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">{title}</p>
          <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1 tabular-nums">{value}</h3>
          <p className={`text-[11px] mt-1 flex items-center gap-0.5 ${subColor || 'text-slate-500 dark:text-slate-400'}`}>
            {sub}
          </p>
        </div>
        <div className={`w-12 h-12 rounded-xl ${iconBg} ${iconColor} flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform duration-200`}>
          <Icon className="w-6 h-6" />
        </div>
      </CardContent>
    </Card>
  );
  return to ? <Link to={to}>{content}</Link> : content;
}

function SkeletonCard() {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="animate-pulse space-y-3">
          <div className="h-3 bg-slate-200 dark:bg-slate-700 rounded w-24" />
          <div className="h-7 bg-slate-200 dark:bg-slate-700 rounded w-32" />
          <div className="h-2.5 bg-slate-200 dark:bg-slate-700 rounded w-20" />
        </div>
      </CardContent>
    </Card>
  );
}

export function DashboardPage() {
  const { data: settings } = useQuery<BusinessSettings>({
    queryKey: ['settings'],
    queryFn: () => api.get<BusinessSettings>('/settings/profile'),
  });

  const { data: salesSummary, isLoading: salesLoading } = useQuery<SaleSummary>({
    queryKey: ['sales-summary'],
    queryFn: () => api.get<SaleSummary>('/sales/summary'),
    staleTime: 30_000,
  });

  const { data: purchaseSummary, isLoading: purchasesLoading } = useQuery<PurchaseSummary>({
    queryKey: ['purchases-summary'],
    queryFn: () => api.get<PurchaseSummary>('/purchases/summary'),
    staleTime: 30_000,
  });

  const { data: inventorySummary, isLoading: inventoryLoading } = useQuery<InventorySummary>({
    queryKey: ['inventory-summary'],
    queryFn: () => api.get<InventorySummary>('/inventory/summary'),
    staleTime: 30_000,
  });

  const { data: invoiceSummary, isLoading: invoicesLoading } = useQuery<InvoiceSummary>({
    queryKey: ['invoices-summary'],
    queryFn: () => api.get<InvoiceSummary>('/invoices/summary'),
    staleTime: 30_000,
  });

  const loading = salesLoading || purchasesLoading || inventoryLoading || invoicesLoading;

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white p-6 rounded-2xl shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/10 rounded-full -translate-y-1/2 translate-x-1/4 pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-40 h-40 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/4 pointer-events-none" />
        <div className="relative">
          <div className="flex items-center gap-2 mb-1">
            <Building className="w-5 h-5 text-indigo-400" />
            <h1 className="text-xl font-bold tracking-tight">
              {settings?.tradeName || settings?.legalName || 'My Business Dashboard'}
            </h1>
          </div>
          <p className="text-xs text-slate-300">
            GSTIN: {settings?.gstin || 'Not configured'} &bull; State: {settings?.stateName} ({settings?.stateCode})
          </p>
          <div className="flex items-center gap-2 mt-2.5">
            <span className="inline-flex items-center gap-1.5 text-[11px] font-medium text-emerald-300 bg-emerald-900/50 border border-emerald-700/60 px-2.5 py-1 rounded-full">
              <Activity className="w-3 h-3" />
              Live Data
            </span>
            <span className="text-[11px] text-slate-400">
              {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2 relative">
          <Link to="/settings">
            <Button variant="secondary" size="sm">Configure Settings</Button>
          </Link>
          <Link to="/invoices">
            <Button size="sm" className="bg-indigo-500 hover:bg-indigo-600">
              <PlusCircle className="w-4 h-4 mr-1.5" />
              New Invoice
            </Button>
          </Link>
        </div>
      </div>

      {/* Financial Overview */}
      <div>
        <h2 className="text-xs font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-3 flex items-center gap-2">
          <BarChart3 className="w-3.5 h-3.5" />
          Financial Overview
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {loading ? (
            Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
          ) : (
            <>
              <StatCard
                title="Total Sales"
                value={formatCurrency(salesSummary?.totalSalesAmount ?? 0)}
                sub={`${salesSummary?.totalSalesCount ?? 0} sale records`}
                subColor="text-emerald-600 dark:text-emerald-400"
                icon={TrendingUp}
                iconBg="bg-emerald-50 dark:bg-emerald-950/60"
                iconColor="text-emerald-600 dark:text-emerald-400"
                to="/sales"
              />
              <StatCard
                title="Total Purchases"
                value={formatCurrency(purchaseSummary?.totalPurchasesAmount ?? 0)}
                sub={`${purchaseSummary?.totalPurchasesCount ?? 0} purchase records`}
                icon={TrendingDown}
                iconBg="bg-blue-50 dark:bg-blue-950/60"
                iconColor="text-blue-600 dark:text-blue-400"
                to="/purchases"
              />
              <StatCard
                title="Receivable"
                value={formatCurrency(salesSummary?.totalOutstandingReceivable ?? 0)}
                sub={`${salesSummary?.pendingSalesCount ?? 0} pending sales`}
                subColor={(salesSummary?.totalOutstandingReceivable ?? 0) > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}
                icon={IndianRupee}
                iconBg="bg-amber-50 dark:bg-amber-950/60"
                iconColor="text-amber-600 dark:text-amber-400"
                to="/sales"
              />
              <StatCard
                title="Payable"
                value={formatCurrency(purchaseSummary?.totalOutstandingPayable ?? 0)}
                sub={`${purchaseSummary?.pendingPurchasesCount ?? 0} pending payments`}
                subColor={(purchaseSummary?.totalOutstandingPayable ?? 0) > 0 ? 'text-rose-600 dark:text-rose-400' : 'text-emerald-600 dark:text-emerald-400'}
                icon={Receipt}
                iconBg="bg-rose-50 dark:bg-rose-950/60"
                iconColor="text-rose-600 dark:text-rose-400"
                to="/purchases"
              />
            </>
          )}
        </div>
      </div>

      {/* Inventory & Invoices */}
      <div>
        <h2 className="text-xs font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-3 flex items-center gap-2">
          <Package className="w-3.5 h-3.5" />
          Inventory &amp; Invoices
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {loading ? (
            Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
          ) : (
            <>
              <StatCard
                title="Stock Valuation"
                value={formatCurrency(inventorySummary?.totalValuation ?? 0)}
                sub={`${inventorySummary?.totalProductsCount ?? 0} products tracked`}
                icon={Package}
                iconBg="bg-violet-50 dark:bg-violet-950/60"
                iconColor="text-violet-600 dark:text-violet-400"
                to="/inventory"
              />
              <StatCard
                title="Low Stock Alerts"
                value={`${inventorySummary?.lowStockCount ?? 0} Items`}
                sub={(inventorySummary?.lowStockCount ?? 0) === 0 ? 'Inventory healthy' : 'Action required!'}
                subColor={(inventorySummary?.lowStockCount ?? 0) > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}
                icon={(inventorySummary?.lowStockCount ?? 0) > 0 ? AlertTriangle : CheckCircle2}
                iconBg={(inventorySummary?.lowStockCount ?? 0) > 0 ? 'bg-amber-50 dark:bg-amber-950/60' : 'bg-emerald-50 dark:bg-emerald-950/60'}
                iconColor={(inventorySummary?.lowStockCount ?? 0) > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}
                to="/inventory"
              />
              <StatCard
                title="Invoices Issued"
                value={`${invoiceSummary?.totalInvoicesCount ?? 0}`}
                sub={`${invoiceSummary?.finalizedInvoicesCount ?? 0} finalized, ${invoiceSummary?.draftInvoicesCount ?? 0} draft`}
                icon={FileText}
                iconBg="bg-indigo-50 dark:bg-indigo-950/60"
                iconColor="text-indigo-600 dark:text-indigo-400"
                to="/invoices"
              />
              <StatCard
                title="Invoice Receivable"
                value={formatCurrency(invoiceSummary?.totalOutstandingReceivable ?? 0)}
                sub="Outstanding from invoices"
                subColor={(invoiceSummary?.totalOutstandingReceivable ?? 0) > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}
                icon={Clock}
                iconBg="bg-sky-50 dark:bg-sky-950/60"
                iconColor="text-sky-600 dark:text-sky-400"
                to="/invoices"
              />
            </>
          )}
        </div>
      </div>

      {/* Quick Actions + System Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Quick Actions */}
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm">
              <PlusCircle className="w-4 h-4 text-indigo-500" />
              Quick Actions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 p-4 pt-0">
            {[
              { to: '/sales', label: 'Record New Sale', icon: TrendingUp, color: 'text-emerald-600', bg: 'bg-emerald-50 dark:bg-emerald-950/40' },
              { to: '/purchases', label: 'Log New Purchase', icon: TrendingDown, color: 'text-blue-600', bg: 'bg-blue-50 dark:bg-blue-950/40' },
              { to: '/invoices', label: 'Create Invoice', icon: FileText, color: 'text-indigo-600', bg: 'bg-indigo-50 dark:bg-indigo-950/40' },
              { to: '/products', label: 'Manage Products', icon: Package, color: 'text-violet-600', bg: 'bg-violet-50 dark:bg-violet-950/40' },
              { to: '/customers', label: 'View Customers', icon: Users, color: 'text-sky-600', bg: 'bg-sky-50 dark:bg-sky-950/40' },
              { to: '/suppliers', label: 'View Suppliers', icon: Truck, color: 'text-amber-600', bg: 'bg-amber-50 dark:bg-amber-950/40' },
            ].map(({ to, label, icon: Icon, color, bg }) => (
              <Link
                key={to}
                to={to}
                className="flex items-center gap-3 p-3 rounded-lg border border-slate-200/80 dark:border-slate-800 hover:border-indigo-300 dark:hover:border-indigo-700 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-all duration-150 group"
              >
                <div className={`w-7 h-7 rounded-lg ${bg} flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform`}>
                  <Icon className={`w-3.5 h-3.5 ${color}`} />
                </div>
                <span className="text-sm font-medium text-slate-700 dark:text-slate-300">{label}</span>
                <ArrowUpRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-600 ml-auto group-hover:text-indigo-500 transition-colors" />
              </Link>
            ))}
          </CardContent>
        </Card>

        {/* System Foundation Status */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  Production Foundation
                </CardTitle>
                <p className="text-xs text-slate-500 mt-0.5">
                  Core infrastructure, database ledger, and financial security modules active.
                </p>
              </div>
              <span className="text-xs font-semibold text-emerald-600 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
                Active &amp; Verified
              </span>
            </div>
          </CardHeader>
          <CardContent className="space-y-3 p-4 pt-0">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-3.5 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
                <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Stateless Auth</h4>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">BCrypt + JWT</p>
                <p className="text-xs text-slate-500 mt-1">Role-based OWNER / ADMIN access. All API requests verified.</p>
              </div>
              <div className="p-3.5 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
                <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Financial Precision</h4>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">100% BigDecimal</p>
                <p className="text-xs text-slate-500 mt-1">No floating-point errors. Deterministic CGST/SGST/IGST split.</p>
              </div>
              <div className="p-3.5 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
                <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Database</h4>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">PostgreSQL + Flyway</p>
                <p className="text-xs text-slate-500 mt-1">V5 migrations applied. Immutable audit trail and backup scripts ready.</p>
              </div>
            </div>
            <div className="p-4 rounded-xl bg-indigo-50/50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 flex items-center justify-between">
              <div className="text-xs text-indigo-900 dark:text-indigo-200">
                <span className="font-semibold">Next Step:</span> Configure your legal entity name, GSTIN, and bank details in Business Settings.
              </div>
              <Link to="/settings">
                <Button size="sm" variant="outline" className="text-xs border-indigo-300 dark:border-indigo-700 text-indigo-600 dark:text-indigo-300 shrink-0 ml-3">
                  Open Settings <ArrowUpRight className="w-3.5 h-3.5 ml-1" />
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
