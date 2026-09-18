import { useEffect, useState } from 'react';
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-react';
import { cn } from '../../lib/utils';
import type { Toast, ToastVariant } from '../../hooks/useToast';

const VARIANTS: Record<ToastVariant, { icon: React.ElementType; bar: string; bg: string; text: string; border: string }> = {
  success: {
    icon: CheckCircle2,
    bar: 'bg-emerald-500',
    bg: 'bg-white dark:bg-slate-900',
    text: 'text-emerald-700 dark:text-emerald-300',
    border: 'border-emerald-200 dark:border-emerald-800',
  },
  error: {
    icon: XCircle,
    bar: 'bg-rose-500',
    bg: 'bg-white dark:bg-slate-900',
    text: 'text-rose-700 dark:text-rose-300',
    border: 'border-rose-200 dark:border-rose-800',
  },
  warning: {
    icon: AlertTriangle,
    bar: 'bg-amber-500',
    bg: 'bg-white dark:bg-slate-900',
    text: 'text-amber-700 dark:text-amber-300',
    border: 'border-amber-200 dark:border-amber-800',
  },
  info: {
    icon: Info,
    bar: 'bg-indigo-500',
    bg: 'bg-white dark:bg-slate-900',
    text: 'text-indigo-700 dark:text-indigo-300',
    border: 'border-indigo-200 dark:border-indigo-800',
  },
};

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: (id: string) => void }) {
  const [visible, setVisible] = useState(false);
  const v = VARIANTS[toast.variant];
  const Icon = v.icon;

  useEffect(() => {
    const t = setTimeout(() => setVisible(true), 10);
    return () => clearTimeout(t);
  }, []);

  const handleDismiss = () => {
    setVisible(false);
    setTimeout(() => onDismiss(toast.id), 300);
  };

  return (
    <div
      className={cn(
        'flex items-start gap-3 w-full max-w-sm rounded-xl border shadow-xl shadow-slate-900/10 px-4 py-3.5 relative overflow-hidden transition-all duration-300',
        v.bg, v.border,
        visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
      )}
    >
      {/* Colored left bar */}
      <div className={cn('absolute left-0 top-0 bottom-0 w-1 rounded-l-xl', v.bar)} />

      <Icon className={cn('w-4.5 h-4.5 mt-0.5 shrink-0', v.text)} />

      <p className="text-xs font-medium text-slate-800 dark:text-slate-200 flex-1 leading-relaxed pr-2">
        {toast.message}
      </p>

      <button
        onClick={handleDismiss}
        className="p-0.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors shrink-0"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}

interface ToastContainerProps {
  toasts: Toast[];
  onDismiss: (id: string) => void;
}

export function ToastContainer({ toasts, onDismiss }: ToastContainerProps) {
  if (toasts.length === 0) return null;

  return (
    <div
      aria-live="polite"
      className="fixed bottom-5 right-5 z-[9999] flex flex-col gap-2.5 pointer-events-none"
    >
      {toasts.map(t => (
        <div key={t.id} className="pointer-events-auto">
          <ToastItem toast={t} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
}
