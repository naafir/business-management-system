import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { MonthlyTrend, ProfitLoss } from '../../types';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency } from '../../lib/utils';
import { toast } from '../../hooks/useToast';
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer
} from 'recharts';
import {
  BarChart3, TrendingUp, TrendingDown, IndianRupee,
  ArrowUpRight, ArrowDownRight, Minus, Download, FileSpreadsheet,
  CalendarDays,
} from 'lucide-react';

function ProfitCard({ title, value, sub, positive, icon: Icon, iconBg, iconColor }: {
  title: string; value: string; sub?: string;
  positive?: boolean | null; icon: React.ElementType; iconBg: string; iconColor: string;
}) {
  const TrendIcon = positive === true ? ArrowUpRight : positive === false ? ArrowDownRight : Minus;
  const trendColor = positive === true ? 'text-emerald-600 dark:text-emerald-400' : positive === false ? 'text-rose-600 dark:text-rose-400' : 'text-slate-500';
  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">{title}</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1 tabular-nums">{value}</h3>
            {sub && <p className={`text-[11px] mt-1 flex items-center gap-0.5 ${trendColor}`}><TrendIcon className="w-3 h-3" />{sub}</p>}
          </div>
          <div className={`w-10 h-10 rounded-xl ${iconBg} ${iconColor} flex items-center justify-center shrink-0`}>
            <Icon className="w-5 h-5" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl p-3 shadow-xl">
      <p className="text-xs font-bold text-slate-700 dark:text-slate-300 mb-2">{label}</p>
      {payload.map((p: any) => (
        <div key={p.name} className="flex items-center gap-2 text-xs">
          <div className="w-2 h-2 rounded-full" style={{ background: p.color }} />
          <span className="text-slate-600 dark:text-slate-400">{p.name}:</span>
          <span className="font-semibold text-slate-900 dark:text-slate-100">
            {typeof p.value === 'number' && p.value > 1000 ? formatCurrency(p.value) : p.value}
          </span>
        </div>
      ))}
    </div>
  );
};

/** Converts an array of objects to a CSV string and triggers browser download */
function exportToCsv(filename: string, rows: Record<string, any>[]) {
  if (!rows.length) {
    toast.warning('No data to export.');
    return;
  }
  const headers = Object.keys(rows[0]);
  const csvContent = [
    headers.join(','),
    ...rows.map(row =>
      headers.map(h => {
        const val = row[h] ?? '';
        // Quote strings that contain commas or quotes
        const str = String(val);
        return str.includes(',') || str.includes('"') ? `"${str.replace(/"/g, '""')}"` : str;
      }).join(',')
    ),
  ].join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(link.href);
  toast.success(`${filename} exported successfully!`);
}

export function ReportsPage() {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [mode, setMode] = useState<'year' | 'custom'>('year');
  const [customStart, setCustomStart] = useState(`${currentYear}-04-01`);
  const [customEnd, setCustomEnd] = useState(new Date().toISOString().split('T')[0]);

  const startDate = mode === 'year' ? `${year}-01-01` : customStart;
  const endDate = mode === 'year'
    ? (year === currentYear ? new Date().toISOString().split('T')[0] : `${year}-12-31`)
    : customEnd;

  const { data: trend, isLoading: trendLoading } = useQuery<MonthlyTrend[]>({
    queryKey: ['monthly-trend', year],
    queryFn: () => api.get<MonthlyTrend[]>(`/reports/monthly-trend?year=${year}`),
    staleTime: 60_000,
  });

  const { data: pl, isLoading: plLoading } = useQuery<ProfitLoss>({
    queryKey: ['profit-loss', startDate, endDate],
    queryFn: () => api.get<ProfitLoss>(`/reports/profit-loss?startDate=${startDate}&endDate=${endDate}`),
    staleTime: 60_000,
  });

  const chartData = (trend ?? []).map(t => ({
    name: t.monthName,
    Sales: Number(t.salesAmount),
    Purchases: Number(t.purchasesAmount),
    Expenses: Number(t.expensesAmount),
  }));

  const netProfit = pl?.netProfit ?? 0;

  const handleExportMonthly = () => {
    if (!trend?.length) { toast.warning('No monthly data to export.'); return; }
    const rows = trend.map(t => ({
      Month: t.monthName,
      'Sales (₹)': Number(t.salesAmount).toFixed(2),
      'Purchases (₹)': Number(t.purchasesAmount).toFixed(2),
      'Expenses (₹)': Number(t.expensesAmount).toFixed(2),
      'Sales Count': t.salesCount,
      'Purchase Count': t.purchasesCount,
    }));
    exportToCsv(`monthly-trend-${year}.csv`, rows);
  };

  const handleExportPL = () => {
    if (!pl) { toast.warning('No P&L data to export.'); return; }
    const rows = [{
      'Period': `${startDate} to ${endDate}`,
      'Total Revenue (₹)': Number(pl.totalRevenue).toFixed(2),
      'Total Purchases/COGS (₹)': Number(pl.totalCogs).toFixed(2),
      'Gross Profit (₹)': Number(pl.grossProfit).toFixed(2),
      'Gross Margin %': pl.grossMarginPercent,
      'Total Expenses (₹)': Number(pl.totalExpenses).toFixed(2),
      'Net Profit (₹)': Number(pl.netProfit).toFixed(2),
      'Tax Collected (₹)': Number(pl.totalTaxCollected).toFixed(2),
      'Input Tax Credit (₹)': Number(pl.totalInputTaxCredit).toFixed(2),
      'Net Tax Liability (₹)': Number(pl.netTaxLiability).toFixed(2),
    }];
    exportToCsv(`profit-loss-${startDate}-to-${endDate}.csv`, rows);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2.5">
            <BarChart3 className="w-7 h-7 text-indigo-600 dark:text-indigo-400" />
            Business Reports
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Sales trends, profitability, and cash flow overview. Export any report to CSV.
          </p>
        </div>

        {/* Period Controls */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 shrink-0">
          <div className="flex rounded-xl bg-slate-100 dark:bg-slate-800 p-1 gap-1">
            <button
              onClick={() => setMode('year')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${mode === 'year' ? 'bg-white dark:bg-slate-700 text-indigo-700 dark:text-indigo-300 shadow-sm' : 'text-slate-500'}`}
            >
              By Year
            </button>
            <button
              onClick={() => setMode('custom')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${mode === 'custom' ? 'bg-white dark:bg-slate-700 text-indigo-700 dark:text-indigo-300 shadow-sm' : 'text-slate-500'}`}
            >
              <CalendarDays className="w-3.5 h-3.5" /> Custom Range
            </button>
          </div>

          {mode === 'year' ? (
            <select
              value={year}
              onChange={e => setYear(Number(e.target.value))}
              className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {[currentYear, currentYear - 1, currentYear - 2, currentYear - 3].map(y => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          ) : (
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={customStart}
                onChange={e => setCustomStart(e.target.value)}
                className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <span className="text-slate-400 text-xs">to</span>
              <input
                type="date"
                value={customEnd}
                onChange={e => setCustomEnd(e.target.value)}
                className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          )}
        </div>
      </div>

      {/* P&L Summary Cards */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-xs font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500 flex items-center gap-2">
            <IndianRupee className="w-3.5 h-3.5" />
            Profit & Loss — {mode === 'year' ? year : `${startDate} to ${endDate}`}
          </h2>
          <Button
            size="sm"
            variant="outline"
            onClick={handleExportPL}
            disabled={plLoading}
            className="text-xs flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5" /> Export CSV
          </Button>
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {plLoading ? (
            Array.from({ length: 4 }).map((_, i) => (
              <Card key={i}><CardContent className="p-5"><div className="animate-pulse space-y-2"><div className="h-3 bg-slate-200 dark:bg-slate-700 rounded w-20" /><div className="h-7 bg-slate-200 dark:bg-slate-700 rounded w-28" /></div></CardContent></Card>
            ))
          ) : (
            <>
              <ProfitCard
                title="Total Revenue"
                value={formatCurrency(pl?.totalRevenue ?? 0)}
                sub="Incl. GST"
                positive={null}
                icon={TrendingUp}
                iconBg="bg-emerald-50 dark:bg-emerald-950/60"
                iconColor="text-emerald-600 dark:text-emerald-400"
              />
              <ProfitCard
                title="Total Purchases"
                value={formatCurrency(pl?.totalCogs ?? 0)}
                sub="COGS"
                positive={null}
                icon={TrendingDown}
                iconBg="bg-blue-50 dark:bg-blue-950/60"
                iconColor="text-blue-600 dark:text-blue-400"
              />
              <ProfitCard
                title="Gross Profit"
                value={formatCurrency(pl?.grossProfit ?? 0)}
                sub={`${pl?.grossMarginPercent ?? 0}% margin`}
                positive={(pl?.grossProfit ?? 0) > 0}
                icon={IndianRupee}
                iconBg={(pl?.grossProfit ?? 0) > 0 ? 'bg-emerald-50 dark:bg-emerald-950/60' : 'bg-rose-50 dark:bg-rose-950/60'}
                iconColor={(pl?.grossProfit ?? 0) > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}
              />
              <ProfitCard
                title="Net Profit"
                value={formatCurrency(netProfit)}
                sub={`After ${formatCurrency(pl?.totalExpenses ?? 0)} expenses`}
                positive={netProfit > 0}
                icon={netProfit > 0 ? TrendingUp : TrendingDown}
                iconBg={netProfit > 0 ? 'bg-emerald-50 dark:bg-emerald-950/60' : 'bg-rose-50 dark:bg-rose-950/60'}
                iconColor={netProfit > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}
              />
            </>
          )}
        </div>
      </div>

      {/* Monthly Bar Chart */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-indigo-600" />
              Monthly Sales vs Purchases vs Expenses — {year}
            </CardTitle>
            <Button size="sm" variant="outline" onClick={handleExportMonthly} disabled={trendLoading} className="text-xs flex items-center gap-1.5">
              <FileSpreadsheet className="w-3.5 h-3.5" /> Export CSV
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-4 pt-0">
          {trendLoading ? (
            <div className="h-64 flex items-center justify-center">
              <div className="animate-spin w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full" />
            </div>
          ) : chartData.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center gap-2 text-slate-400">
              <BarChart3 className="w-10 h-10 opacity-30" />
              <p className="text-sm">No data for {year}</p>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,116,139,0.15)" />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} />
                <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} tickFormatter={v => v >= 1000 ? `₹${(v/1000).toFixed(0)}k` : `₹${v}`} />
                <Tooltip content={<CustomTooltip />} />
                <Legend wrapperStyle={{ fontSize: '12px' }} />
                <Bar dataKey="Sales" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Purchases" fill="#0ea5e9" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Expenses" fill="#f59e0b" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      {/* Revenue Trend Line Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="w-4 h-4 text-emerald-600" />
            Revenue Trend — {year}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 pt-0">
          {trendLoading ? (
            <div className="h-64 flex items-center justify-center">
              <div className="animate-spin w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full" />
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(100,116,139,0.15)" />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} />
                <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} tickFormatter={v => v >= 1000 ? `₹${(v/1000).toFixed(0)}k` : `₹${v}`} />
                <Tooltip content={<CustomTooltip />} />
                <Legend wrapperStyle={{ fontSize: '12px' }} />
                <Line type="monotone" dataKey="Sales" stroke="#4f46e5" strokeWidth={2.5} dot={{ r: 4, fill: '#4f46e5' }} activeDot={{ r: 6 }} />
                <Line type="monotone" dataKey="Expenses" stroke="#f59e0b" strokeWidth={2} dot={{ r: 3, fill: '#f59e0b' }} strokeDasharray="5 5" />
              </LineChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      {/* Tax Summary */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <IndianRupee className="w-4 h-4 text-amber-600" />
              Tax Summary
            </CardTitle>
            <Button size="sm" variant="outline" onClick={handleExportPL} disabled={plLoading} className="text-xs flex items-center gap-1.5">
              <Download className="w-3.5 h-3.5" /> Export
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-4 pt-0">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-100 dark:border-emerald-900/50">
              <p className="text-xs font-semibold uppercase text-emerald-600 dark:text-emerald-400 tracking-wide">Output Tax Collected</p>
              <p className="text-xl font-bold text-emerald-800 dark:text-emerald-200 mt-1 tabular-nums">{formatCurrency(pl?.totalTaxCollected ?? 0)}</p>
              <p className="text-[11px] text-emerald-700/60 dark:text-emerald-400/60 mt-1">From sales invoices</p>
            </div>
            <div className="p-4 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-100 dark:border-blue-900/50">
              <p className="text-xs font-semibold uppercase text-blue-600 dark:text-blue-400 tracking-wide">Input Tax Credit</p>
              <p className="text-xl font-bold text-blue-800 dark:text-blue-200 mt-1 tabular-nums">{formatCurrency(pl?.totalInputTaxCredit ?? 0)}</p>
              <p className="text-[11px] text-blue-700/60 dark:text-blue-400/60 mt-1">From purchase invoices</p>
            </div>
            <div className={`p-4 rounded-xl border ${(pl?.netTaxLiability ?? 0) >= 0 ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-100 dark:border-amber-900/50' : 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-100 dark:border-emerald-900/50'}`}>
              <p className={`text-xs font-semibold uppercase tracking-wide ${(pl?.netTaxLiability ?? 0) >= 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
                Net Tax Liability
              </p>
              <p className={`text-xl font-bold mt-1 tabular-nums ${(pl?.netTaxLiability ?? 0) >= 0 ? 'text-amber-800 dark:text-amber-200' : 'text-emerald-800 dark:text-emerald-200'}`}>
                {formatCurrency(pl?.netTaxLiability ?? 0)}
              </p>
              <p className="text-[11px] text-slate-500 mt-1">
                {(pl?.netTaxLiability ?? 0) >= 0 ? 'Payable to Govt.' : 'GST Refund Eligible'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
