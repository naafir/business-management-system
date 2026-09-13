import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../services/api';
import { GstOutputSummary, GstInputSummary, HsnSummaryLine } from '../../types';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency } from '../../lib/utils';
import {
  FileText, TrendingUp, TrendingDown,
  ShieldCheck, Download, AlertTriangle, CheckCircle2
} from 'lucide-react';

function StatRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-slate-100 dark:border-slate-800 last:border-0">
      <span className="text-sm text-slate-600 dark:text-slate-400">{label}</span>
      <span className={`text-sm font-semibold tabular-nums ${highlight ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-900 dark:text-slate-100'}`}>
        {value}
      </span>
    </div>
  );
}

function getDefaultDateRange() {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  return {
    startDate: start.toISOString().split('T')[0],
    endDate: now.toISOString().split('T')[0],
  };
}

export function GstReportPage() {
  const [dateRange, setDateRange] = useState(getDefaultDateRange);

  const params = `startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;

  const { data: outputData, isLoading: outputLoading } = useQuery<GstOutputSummary>({
    queryKey: ['gst-output', dateRange],
    queryFn: () => api.get<GstOutputSummary>(`/reports/gst-output?${params}`),
    staleTime: 60_000,
  });

  const { data: inputData, isLoading: inputLoading } = useQuery<GstInputSummary>({
    queryKey: ['gst-input', dateRange],
    queryFn: () => api.get<GstInputSummary>(`/reports/gst-input?${params}`),
    staleTime: 60_000,
  });

  const { data: hsnData, isLoading: hsnLoading } = useQuery<HsnSummaryLine[]>({
    queryKey: ['hsn-summary', dateRange],
    queryFn: () => api.get<HsnSummaryLine[]>(`/reports/hsn-summary?${params}`),
    staleTime: 60_000,
  });

  const netTaxLiability = ((outputData?.totalTaxCollected ?? 0) - (inputData?.totalInputTaxCredit ?? 0));

  const exportHsnCsv = () => {
    if (!hsnData?.length) return;
    const header = ['HSN/SAC', 'Taxable Value', 'CGST', 'SGST', 'IGST', 'Total Tax', 'Total Amount'];
    const rows = hsnData.map(r => [
      r.hsnSac, r.totalTaxableValue, r.totalCgst, r.totalSgst, r.totalIgst, r.totalTax, r.totalAmount
    ].join(','));
    const csv = [header.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `hsn-summary-${dateRange.startDate}-to-${dateRange.endDate}.csv`; a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-indigo-600" />
            GST Report
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
            Output tax collected, input tax credit, and HSN-wise breakdown
          </p>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={dateRange.startDate}
            onChange={e => setDateRange(d => ({ ...d, startDate: e.target.value }))}
            className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <span className="text-slate-400 text-sm">to</span>
          <input
            type="date"
            value={dateRange.endDate}
            onChange={e => setDateRange(d => ({ ...d, endDate: e.target.value }))}
            className="px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Quick presets */}
      <div className="flex flex-wrap gap-2">
        {[
          { label: 'This Month', fn: () => { const n = new Date(); setDateRange({ startDate: new Date(n.getFullYear(), n.getMonth(), 1).toISOString().split('T')[0], endDate: n.toISOString().split('T')[0] }); } },
          { label: 'Last Month', fn: () => { const n = new Date(); const s = new Date(n.getFullYear(), n.getMonth() - 1, 1); const e = new Date(n.getFullYear(), n.getMonth(), 0); setDateRange({ startDate: s.toISOString().split('T')[0], endDate: e.toISOString().split('T')[0] }); } },
          { label: 'This Quarter', fn: () => { const n = new Date(); const q = Math.floor(n.getMonth() / 3); const s = new Date(n.getFullYear(), q * 3, 1); setDateRange({ startDate: s.toISOString().split('T')[0], endDate: n.toISOString().split('T')[0] }); } },
          { label: 'This Year', fn: () => { const n = new Date(); setDateRange({ startDate: `${n.getFullYear()}-01-01`, endDate: n.toISOString().split('T')[0] }); } },
        ].map(({ label, fn }) => (
          <button key={label} onClick={fn}
            className="px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 hover:border-indigo-300 dark:hover:border-indigo-700 hover:text-indigo-600 dark:hover:text-indigo-400 transition-all">
            {label}
          </button>
        ))}
      </div>

      {/* Net Tax Liability Banner */}
      <div className={`p-5 rounded-2xl border ${netTaxLiability >= 0 ? 'bg-gradient-to-r from-rose-900 via-rose-800 to-slate-900 border-rose-700' : 'bg-gradient-to-r from-emerald-900 via-emerald-800 to-slate-900 border-emerald-700'} text-white`}>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-widest text-white/70 mb-1">Net GST Liability (Output − ITC)</p>
            <h2 className="text-3xl font-bold tabular-nums">{formatCurrency(Math.abs(netTaxLiability))}</h2>
            <p className="text-xs text-white/70 mt-1">
              {netTaxLiability >= 0 ? 'Tax payable to government' : 'ITC surplus (carry forward)'}
            </p>
          </div>
          {netTaxLiability >= 0
            ? <AlertTriangle className="w-10 h-10 text-rose-300" />
            : <CheckCircle2 className="w-10 h-10 text-emerald-300" />}
        </div>
      </div>

      {/* Output + Input side by side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Output Tax */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-emerald-600" />
              Output Tax (Collected from Sales)
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 pt-0">
            {outputLoading ? (
              <div className="space-y-2 animate-pulse">{Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-8 bg-slate-100 dark:bg-slate-800 rounded" />)}</div>
            ) : (
              <>
                <StatRow label="Total Revenue (incl. GST)" value={formatCurrency(outputData?.totalRevenue ?? 0)} />
                <StatRow label="Taxable Value" value={formatCurrency(outputData?.totalTaxableValue ?? 0)} />
                <StatRow label="B2B Taxable Value" value={formatCurrency(outputData?.b2bTaxableValue ?? 0)} />
                <StatRow label="B2C Taxable Value" value={formatCurrency(outputData?.b2cTaxableValue ?? 0)} />
                <StatRow label="CGST Collected" value={formatCurrency(outputData?.totalCgstCollected ?? 0)} />
                <StatRow label="SGST Collected" value={formatCurrency(outputData?.totalSgstCollected ?? 0)} />
                <StatRow label="IGST Collected" value={formatCurrency(outputData?.totalIgstCollected ?? 0)} />
                <div className="mt-2 pt-2 border-t border-slate-200 dark:border-slate-700 flex justify-between">
                  <span className="text-sm font-bold text-slate-900 dark:text-slate-100">Total Output Tax</span>
                  <span className="text-sm font-bold text-emerald-600 dark:text-emerald-400 tabular-nums">{formatCurrency(outputData?.totalTaxCollected ?? 0)}</span>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Input Tax Credit */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingDown className="w-4 h-4 text-blue-600" />
              Input Tax Credit (Paid on Purchases)
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 pt-0">
            {inputLoading ? (
              <div className="space-y-2 animate-pulse">{Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-8 bg-slate-100 dark:bg-slate-800 rounded" />)}</div>
            ) : (
              <>
                <StatRow label="Total Purchase Value (incl. GST)" value={formatCurrency(inputData?.totalPurchaseValue ?? 0)} />
                <StatRow label="Taxable Value" value={formatCurrency(inputData?.totalTaxableValue ?? 0)} />
                <StatRow label="CGST Paid" value={formatCurrency(inputData?.totalCgstPaid ?? 0)} />
                <StatRow label="SGST Paid" value={formatCurrency(inputData?.totalSgstPaid ?? 0)} />
                <StatRow label="IGST Paid" value={formatCurrency(inputData?.totalIgstPaid ?? 0)} />
                <StatRow label="Purchases Count" value={`${inputData?.totalPurchasesCount ?? 0}`} />
                <div className="mt-2 pt-2 border-t border-slate-200 dark:border-slate-700 flex justify-between">
                  <span className="text-sm font-bold text-slate-900 dark:text-slate-100">Total Input Tax Credit</span>
                  <span className="text-sm font-bold text-blue-600 dark:text-blue-400 tabular-nums">{formatCurrency(inputData?.totalInputTaxCredit ?? 0)}</span>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* HSN Summary Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-600" />
              HSN / SAC Summary (GSTR-1)
            </CardTitle>
            <Button variant="outline" size="sm" onClick={exportHsnCsv} disabled={!hsnData?.length}>
              <Download className="w-3.5 h-3.5 mr-1.5" />
              Export CSV
            </Button>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700">
                {['HSN/SAC', 'Qty', 'Taxable Value', 'CGST', 'SGST', 'IGST', 'Total Tax', 'Total Amount'].map(h => (
                  <th key={h} className="px-4 py-3 text-right first:text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {hsnLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 8 }).map((_, j) => (
                      <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-100 dark:bg-slate-800 rounded animate-pulse" /></td>
                    ))}
                  </tr>
                ))
              ) : !hsnData?.length ? (
                <tr>
                  <td colSpan={8} className="px-4 py-12 text-center text-slate-500 dark:text-slate-400">
                    No sales data for the selected period
                  </td>
                </tr>
              ) : hsnData.map((row, i) => (
                <tr key={i} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                  <td className="px-4 py-3 font-mono text-indigo-600 dark:text-indigo-400">{row.hsnSac}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{row.totalQuantity.toFixed(2)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(row.totalTaxableValue)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(row.totalCgst)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(row.totalSgst)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatCurrency(row.totalIgst)}</td>
                  <td className="px-4 py-3 text-right tabular-nums font-semibold">{formatCurrency(row.totalTax)}</td>
                  <td className="px-4 py-3 text-right tabular-nums font-bold text-slate-900 dark:text-slate-100">{formatCurrency(row.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
