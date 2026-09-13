import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { MonthlyTrend, ProfitLoss } from '../../types';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { formatCurrency } from '../../lib/utils';
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer
} from 'recharts';
import {
  BarChart3, TrendingUp, TrendingDown, IndianRupee,
  ArrowUpRight, ArrowDownRight, Minus
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

export function ReportsPage() {
  const [year, setYear] = useState(new Date().getFullYear());

  const currentYear = new Date().getFullYear();
  const startDate = `${year}-01-01`;
  const endDate = year === currentYear ? new Date().toISOString().split('T')[0] : `${year}-12-31`;

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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-indigo-600" />
            Business Reports
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
            Sales trends, profitability, and cash flow overview
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-slate-500">Year:</span>
          <select
            value={year}
            onChange={e => setYear(Number(e.target.value))}
            className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {[currentYear, currentYear - 1, currentYear - 2].map(y => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </div>
      </div>

      {/* P&L Summary Cards */}
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
              sub={`After ₹${formatCurrency(pl?.totalExpenses ?? 0)} expenses`}
              positive={netProfit > 0}
              icon={netProfit > 0 ? TrendingUp : TrendingDown}
              iconBg={netProfit > 0 ? 'bg-emerald-50 dark:bg-emerald-950/60' : 'bg-rose-50 dark:bg-rose-950/60'}
              iconColor={netProfit > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}
            />
          </>
        )}
      </div>

      {/* Monthly Bar Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-indigo-600" />
            Monthly Sales vs Purchases vs Expenses — {year}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 pt-0">
          {trendLoading ? (
            <div className="h-64 flex items-center justify-center">
              <div className="animate-spin w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full" />
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
          <CardTitle className="flex items-center gap-2">
            <IndianRupee className="w-4 h-4 text-amber-600" />
            Tax Summary
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4 pt-0">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-100 dark:border-emerald-900/50">
              <p className="text-xs font-semibold uppercase text-emerald-600 dark:text-emerald-400 tracking-wide">Output Tax Collected</p>
              <p className="text-xl font-bold text-emerald-800 dark:text-emerald-200 mt-1 tabular-nums">{formatCurrency(pl?.totalTaxCollected ?? 0)}</p>
            </div>
            <div className="p-4 rounded-xl bg-blue-50 dark:bg-blue-950/30 border border-blue-100 dark:border-blue-900/50">
              <p className="text-xs font-semibold uppercase text-blue-600 dark:text-blue-400 tracking-wide">Input Tax Credit</p>
              <p className="text-xl font-bold text-blue-800 dark:text-blue-200 mt-1 tabular-nums">{formatCurrency(pl?.totalInputTaxCredit ?? 0)}</p>
            </div>
            <div className={`p-4 rounded-xl border ${(pl?.netTaxLiability ?? 0) >= 0 ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-100 dark:border-amber-900/50' : 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-100 dark:border-emerald-900/50'}`}>
              <p className={`text-xs font-semibold uppercase tracking-wide ${(pl?.netTaxLiability ?? 0) >= 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}`}>Net Tax Liability</p>
              <p className={`text-xl font-bold mt-1 tabular-nums ${(pl?.netTaxLiability ?? 0) >= 0 ? 'text-amber-800 dark:text-amber-200' : 'text-emerald-800 dark:text-emerald-200'}`}>{formatCurrency(pl?.netTaxLiability ?? 0)}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
