import { useState, useCallback } from 'react';

export type ToastVariant = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  message: string;
  variant: ToastVariant;
  duration?: number;
}

let externalSetToasts: React.Dispatch<React.SetStateAction<Toast[]>> | null = null;

export function useToast() {
  const [toasts, setToasts] = useState<Toast[]>([]);
  externalSetToasts = setToasts;

  const toast = useCallback(
    (message: string, variant: ToastVariant = 'info', duration = 4000) => {
      const id = Math.random().toString(36).slice(2);
      const newToast: Toast = { id, message, variant, duration };
      setToasts(prev => [...prev, newToast]);
      setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== id));
      }, duration);
    },
    []
  );

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return { toasts, toast, dismiss };
}

// Imperative toast helper for use outside React components
export const toast = {
  success: (msg: string) => externalSetToasts?.(prev => addToast(prev, msg, 'success')),
  error: (msg: string) => externalSetToasts?.(prev => addToast(prev, msg, 'error')),
  warning: (msg: string) => externalSetToasts?.(prev => addToast(prev, msg, 'warning')),
  info: (msg: string) => externalSetToasts?.(prev => addToast(prev, msg, 'info')),
};

function addToast(prev: Toast[], message: string, variant: ToastVariant): Toast[] {
  const id = Math.random().toString(36).slice(2);
  setTimeout(() => {
    externalSetToasts?.(p => p.filter(t => t.id !== id));
  }, 4000);
  return [...prev, { id, message, variant }];
}
