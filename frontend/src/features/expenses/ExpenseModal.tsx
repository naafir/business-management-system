import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { CreateExpenseRequest, ExpenseCategory } from '../../types';
import { Button } from '../../components/ui/Button';
import { X, IndianRupee, Receipt } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

const PAYMENT_METHODS = ['CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'CREDIT_CARD', 'DEBIT_CARD'] as const;

export function ExpenseModal({ isOpen, onClose }: Props) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CreateExpenseRequest>({
    expenseDate: new Date().toISOString().split('T')[0],
    description: '',
    amount: 0,
    gstAmount: 0,
    gstEligible: false,
  });

  const { data: categories } = useQuery<ExpenseCategory[]>({
    queryKey: ['expense-categories'],
    queryFn: () => api.get<ExpenseCategory[]>('/expenses/categories'),
    enabled: isOpen,
  });


  const createMutation = useMutation({
    mutationFn: (data: CreateExpenseRequest) => api.post('/expenses', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['expense-summary'] });
      onClose();
    },
  });

  useEffect(() => {
    if (isOpen) {
      setForm({
        expenseDate: new Date().toISOString().split('T')[0],
        description: '',
        amount: 0,
        gstAmount: 0,
        gstEligible: false,
      });
    }
  }, [isOpen]);

  const totalAmount = (Number(form.amount) || 0) + (Number(form.gstAmount) || 0);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-2xl w-full max-w-lg border border-slate-200 dark:border-slate-700">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-700">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-amber-100 dark:bg-amber-950/60 rounded-xl flex items-center justify-center">
              <Receipt className="w-4.5 h-4.5 text-amber-600" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-slate-100">Record Expense</h2>
              <p className="text-xs text-slate-500">Log an operational expense</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
            <X className="w-4 h-4" />
          </button>
        </div>

        <form
          onSubmit={e => {
            e.preventDefault();
            createMutation.mutate(form);
          }}
          className="p-6 space-y-4"
        >
          {/* Category + Date */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Category</label>
              <select
                value={form.categoryId ?? ''}
                onChange={e => setForm(f => ({ ...f, categoryId: e.target.value || undefined }))}
                className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Uncategorized</option>
                {categories?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Date *</label>
              <input
                type="date"
                required
                value={form.expenseDate}
                onChange={e => setForm(f => ({ ...f, expenseDate: e.target.value }))}
                className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Description *</label>
            <input
              required
              value={form.description}
              onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
              placeholder="e.g. Monthly office rent payment"
              className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Vendor */}
          <div>
            <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Vendor / Payee</label>
            <input
              value={form.vendorName ?? ''}
              onChange={e => setForm(f => ({ ...f, vendorName: e.target.value || undefined }))}
              placeholder="Name of vendor or payee"
              className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {/* Amount + GST */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Amount (excl. GST) *</label>
              <div className="relative mt-1">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">₹</span>
                <input
                  type="number" step="0.01" min="0.01" required
                  value={form.amount || ''}
                  onChange={e => setForm(f => ({ ...f, amount: parseFloat(e.target.value) || 0 }))}
                  className="w-full pl-7 pr-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">GST Amount</label>
              <div className="relative mt-1">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">₹</span>
                <input
                  type="number" step="0.01" min="0"
                  value={form.gstAmount || ''}
                  onChange={e => setForm(f => ({ ...f, gstAmount: parseFloat(e.target.value) || 0 }))}
                  className="w-full pl-7 pr-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
          </div>

          {/* GST Eligible Toggle */}
          <div className="flex items-center gap-3 p-3 rounded-lg bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800">
            <input
              type="checkbox"
              id="gstEligible"
              checked={form.gstEligible ?? false}
              onChange={e => setForm(f => ({ ...f, gstEligible: e.target.checked }))}
              className="w-4 h-4 rounded border-slate-300 text-indigo-600"
            />
            <label htmlFor="gstEligible" className="text-sm font-medium text-amber-900 dark:text-amber-200 cursor-pointer">
              Eligible for GST Input Tax Credit (ITC)
            </label>
          </div>

          {/* Payment Method + Reference */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Payment Method</label>
              <select
                value={form.paymentMethod ?? ''}
                onChange={e => setForm(f => ({ ...f, paymentMethod: (e.target.value as any) || undefined }))}
                className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="">Select...</option>
                {PAYMENT_METHODS.map(m => <option key={m} value={m}>{m.replace(/_/g, ' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase tracking-wide">Reference #</label>
              <input
                value={form.referenceNumber ?? ''}
                onChange={e => setForm(f => ({ ...f, referenceNumber: e.target.value || undefined }))}
                placeholder="Bill / receipt number"
                className="mt-1 w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Total Preview */}
          <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700">
            <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
              <IndianRupee className="w-4 h-4" />
              Total Amount
            </div>
            <span className="text-lg font-bold text-slate-900 dark:text-slate-100">
              ₹{totalAmount.toFixed(2)}
            </span>
          </div>

          {createMutation.isError && (
            <div className="text-xs text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 px-3 py-2 rounded-lg border border-rose-200 dark:border-rose-800">
              Failed to save expense. Please try again.
            </div>
          )}

          <div className="flex gap-2 pt-1">
            <Button type="button" variant="outline" className="flex-1" onClick={onClose}>Cancel</Button>
            <Button type="submit" className="flex-1" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Saving…' : 'Record Expense'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
