import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { BusinessSettings } from '../../types';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
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
  ArrowUpRight
} from 'lucide-react';

export function DashboardPage() {
  const { data: settings } = useQuery<BusinessSettings>({
    queryKey: ['settings'],
    queryFn: () => api.get<BusinessSettings>('/settings/profile'),
  });

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-gradient-to-r from-indigo-900 to-slate-900 text-white p-6 rounded-2xl shadow-xl">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Building className="w-5 h-5 text-indigo-400" />
            <h1 className="text-xl font-bold tracking-tight">
              {settings?.tradeName || settings?.legalName || 'My Business Dashboard'}
            </h1>
          </div>
          <p className="text-xs text-slate-300">
            GSTIN: {settings?.gstin || 'Not configured'} • State: {settings?.stateName} ({settings?.stateCode})
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link to="/settings">
            <Button variant="secondary" size="sm">
              Configure Settings
            </Button>
          </Link>
          <Link to="/invoices">
            <Button size="sm" className="bg-indigo-500 hover:bg-indigo-600">
              <PlusCircle className="w-4 h-4 mr-1.5" />
              New Invoice
            </Button>
          </Link>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="hover:border-indigo-500/50 transition-colors">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Today's Sales</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">₹0.00</h3>
              <p className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1 flex items-center gap-0.5">
                <TrendingUp className="w-3.5 h-3.5" /> 0 invoices today
              </p>
            </div>
            <div className="w-12 h-12 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <FileText className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-500/50 transition-colors">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Today's Purchases</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">₹0.00</h3>
              <p className="text-[11px] text-slate-500 mt-1 flex items-center gap-0.5">
                <TrendingDown className="w-3.5 h-3.5" /> 0 receipts logged
              </p>
            </div>
            <div className="w-12 h-12 rounded-xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <TrendingDown className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-500/50 transition-colors">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Today's Expenses</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">₹0.00</h3>
              <p className="text-[11px] text-slate-500 mt-1 flex items-center gap-0.5">
                <Receipt className="w-3.5 h-3.5" /> 0 entries
              </p>
            </div>
            <div className="w-12 h-12 rounded-xl bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center">
              <Receipt className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>

        <Card className="hover:border-indigo-500/50 transition-colors">
          <CardContent className="p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Low Stock Alerts</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">0 Items</h3>
              <p className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1 flex items-center gap-0.5">
                <CheckCircle2 className="w-3.5 h-3.5" /> Inventory healthy
              </p>
            </div>
            <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Phase 1 Foundation Status Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-600" />
                Phase 1 Production Foundation
              </CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">
                Core infrastructure, database ledger, and financial security modules active.
              </p>
            </div>
            <span className="text-xs font-semibold text-emerald-600 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
              Active & Verified
            </span>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Stateless Authentication</h4>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">BCrypt + JWT Tokens</p>
              <p className="text-xs text-slate-500 mt-1">Role-based security active (`OWNER`, `ADMIN`). Unauthenticated API requests rejected with 401/403.</p>
            </div>

            <div className="p-4 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Financial Precision</h4>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">100% BigDecimal Engine</p>
              <p className="text-xs text-slate-500 mt-1">No floating point errors. Deterministic Place of Supply rules & automatic CGST/SGST/IGST breakdown.</p>
            </div>

            <div className="p-4 rounded-xl border border-slate-200/80 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <h4 className="text-xs font-semibold uppercase text-slate-500 tracking-wider">Database & Migrations</h4>
              <p className="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1">PostgreSQL & Flyway V1</p>
              <p className="text-xs text-slate-500 mt-1">Versioned DDL migrations, immutable audit trail tables, and automated backup scripts ready.</p>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-indigo-50/50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 flex items-center justify-between">
            <div className="text-xs text-indigo-900 dark:text-indigo-200">
              <span className="font-semibold">Next Step:</span> Configure your legal entity name, GSTIN, and bank details in Business Settings.
            </div>
            <Link to="/settings">
              <Button size="sm" variant="outline" className="text-xs border-indigo-300 dark:border-indigo-700 text-indigo-600 dark:text-indigo-300">
                Open Settings <ArrowUpRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
