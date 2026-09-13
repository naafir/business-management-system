import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Expense, ExpenseSummary, PageResponse } from '../../types';
import { Card, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { formatCurrency, formatDate } from '../../lib/utils';
import { ExpenseModal } from './ExpenseModal';
import {
  PlusCircle, Receipt, Search, Trash2, CheckCircle2,
  IndianRupee, TrendingDown, Filter
} from 'lucide-react';

function SummaryCard({ title, value, sub, icon: Icon, iconBg, iconColor }: {
  title: string; value: string; sub?: string;
  icon: React.ElementType; iconBg: string; iconColor: string;
}) {
  return (
    <Card>
      <CardContent className="p-5 flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">{title}</p>
          <h3 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1 tabular-nums">{value}</h3>
          {sub && <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">{sub}</p>}
        </div>
        <div className={`w-12 h-12 rounded-xl ${iconBg} ${iconColor} flex items-center justify-center shrink-0`}>
          <Icon className="w-6 h-6" />
        </div>
      </CardContent>
    </Card>
  );
}

export function ExpensesPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const { data: summary } = useQuery<ExpenseSummary>({
    queryKey: ['expense-summary'],
    queryFn: () => api.get<ExpenseSummary>('/expenses/summary'),
    staleTime: 30_000,
  });

  const { data: expensesPage, isLoading } = useQuery<PageResponse<Expense>>({
    queryKey: ['expenses', search, page],
    queryFn: () => api.get<PageResponse<Expense>>(`/expenses?search=${encodeURIComponent(search)}&page=${page}&size=20`),
    staleTime: 30_000,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/expenses/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['expense-summary'] });
    },
  });

  const expenses = expensesPage?.content ?? [];
  const totalPages = expensesPage?.totalPages ?? 1;

  const handleDelete = (id: string, desc: string) => {
    if (confirm(`Delete expense "${desc}"? This cannot be undone.`)) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
            <Receipt className="w-6 h-6 text-amber-600" />
            Expenses
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
            Track operational expenses and GST input tax credit eligibility
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <PlusCircle className="w-4 h-4 mr-1.5" />
          Record Expense
        </Button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <SummaryCard
          title="Total Expenses"
          value={formatCurrency(summary?.totalExpensesAmount ?? 0)}
          sub={`${summary?.totalExpensesCount ?? 0} records`}
          icon={TrendingDown}
          iconBg="bg-amber-50 dark:bg-amber-950/60"
          iconColor="text-amber-600 dark:text-amber-400"
        />
        <SummaryCard
          title="ITC Eligible GST"
          value={formatCurrency(summary?.eligibleGstInputAmount ?? 0)}
          sub="This financial year"
          icon={CheckCircle2}
          iconBg="bg-emerald-50 dark:bg-emerald-950/60"
          iconColor="text-emerald-600 dark:text-emerald-400"
        />
        <SummaryCard
          title="Total Records"
          value={`${summary?.totalExpensesCount ?? 0}`}
          icon={IndianRupee}
          iconBg="bg-indigo-50 dark:bg-indigo-950/60"
          iconColor="text-indigo-600 dark:text-indigo-400"
        />
      </div>

      {/* Search & Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search by description or vendor..."
                value={search}
                onChange={e => { setSearch(e.target.value); setPage(0); }}
                className="w-full pl-9 pr-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <Button variant="outline" size="sm" className="flex items-center gap-1.5">
              <Filter className="w-3.5 h-3.5" />
              Filters
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700">
                {['Date', 'Description', 'Category', 'Vendor', 'Amount', 'GST', 'Total', 'ITC', ''].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 9 }).map((_, j) => (
                      <td key={j} className="px-4 py-3">
                        <div className="h-4 bg-slate-100 dark:bg-slate-800 rounded animate-pulse" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : expenses.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-16 text-center">
                    <Receipt className="w-10 h-10 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
                    <p className="text-slate-500 dark:text-slate-400 font-medium">No expenses recorded yet</p>
                    <p className="text-xs text-slate-400 mt-1">Click "Record Expense" to add your first entry</p>
                  </td>
                </tr>
              ) : expenses.map(exp => (
                <tr key={exp.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                  <td className="px-4 py-3 whitespace-nowrap text-slate-700 dark:text-slate-300">{formatDate(exp.expenseDate)}</td>
                  <td className="px-4 py-3 max-w-[200px] truncate font-medium text-slate-900 dark:text-slate-100">{exp.description}</td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
                      {exp.categoryName ?? 'Uncategorized'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{exp.vendorName ?? '—'}</td>
                  <td className="px-4 py-3 tabular-nums text-slate-700 dark:text-slate-300">{formatCurrency(exp.amount)}</td>
                  <td className="px-4 py-3 tabular-nums text-slate-500 dark:text-slate-400">{formatCurrency(exp.gstAmount)}</td>
                  <td className="px-4 py-3 tabular-nums font-semibold text-slate-900 dark:text-slate-100">{formatCurrency(exp.totalAmount)}</td>
                  <td className="px-4 py-3">
                    {exp.gstEligible ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700 dark:text-emerald-400">
                        <CheckCircle2 className="w-3 h-3" /> ITC
                      </span>
                    ) : (
                      <span className="text-[11px] text-slate-400">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleDelete(exp.id, exp.description)}
                      className="p-1.5 rounded-md text-slate-400 hover:text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 dark:border-slate-700">
            <span className="text-xs text-slate-500">Page {page + 1} of {totalPages}</span>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</Button>
              <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</Button>
            </div>
          </div>
        )}
      </Card>

      <ExpenseModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </div>
  );
}
