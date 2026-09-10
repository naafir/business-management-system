import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Product, StockAdjustmentMode, StockAdjustmentRequest, InventoryTransaction } from '../../types';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { AlertTriangle, ArrowDown, ArrowUp, Equal, Package, ShieldAlert } from 'lucide-react';

interface StockAdjustmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  products: Product[];
  initialProduct?: Product | null;
}

const COMMON_REASONS = [
  'Physical inventory audit count discrepancy',
  'Damaged packaging / broken bottles or packs',
  'Expired / spoiled stock write-off',
  'Customer return (manual intake)',
  'Internal sample / testing consumption',
  'Supplier consignment adjustment',
  'Other operational adjustment',
];

export function StockAdjustmentModal({
  isOpen,
  onClose,
  products,
  initialProduct,
}: StockAdjustmentModalProps) {
  const queryClient = useQueryClient();

  const [selectedProductId, setSelectedProductId] = useState<string>('');
  const [adjustmentMode, setAdjustmentMode] = useState<StockAdjustmentMode>('INCREASE');
  const [quantity, setQuantity] = useState<string>('');
  const [reason, setReason] = useState<string>(COMMON_REASONS[0]);
  const [notes, setNotes] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (initialProduct) {
      setSelectedProductId(initialProduct.id);
    } else if (products.length > 0 && !selectedProductId) {
      setSelectedProductId(products[0].id);
    }
  }, [initialProduct, products, isOpen]);

  const selectedProduct = products.find((p) => p.id === selectedProductId);
  const currentStock = selectedProduct ? Number(selectedProduct.currentStock) : 0;
  const numQty = parseFloat(quantity) || 0;

  let resultingStock = currentStock;
  if (adjustmentMode === 'INCREASE') {
    resultingStock = currentStock + numQty;
  } else if (adjustmentMode === 'DECREASE') {
    resultingStock = currentStock - numQty;
  } else if (adjustmentMode === 'SET_EXACT') {
    resultingStock = numQty;
  }

  const willBeNegative = resultingStock < 0 && !(selectedProduct?.allowNegativeStock ?? false);

  const mutation = useMutation({
    mutationFn: (data: StockAdjustmentRequest) =>
      api.post<InventoryTransaction>('/inventory/adjust', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      onClose();
      resetForm();
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to adjust stock';
      setError(msg);
    },
  });

  const resetForm = () => {
    setQuantity('');
    setNotes('');
    setError(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!selectedProductId) {
      setError('Please select a product');
      return;
    }

    if (numQty <= 0) {
      setError('Adjustment quantity must be greater than zero');
      return;
    }

    if (willBeNegative) {
      setError(
        `Insufficient stock. Deducting this quantity would result in negative stock (${resultingStock} ${selectedProduct?.unit}), which is disabled for this product.`
      );
      return;
    }

    mutation.mutate({
      productId: selectedProductId,
      adjustmentMode,
      quantity: numQty,
      reason,
      notes: notes.trim() || undefined,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Record Stock Adjustment"
      description="Update stock levels with full transaction ledger tracking and audit accountability."
      className="max-w-xl"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-800 dark:text-rose-300 text-xs flex items-start gap-2.5">
            <ShieldAlert className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <div className="flex-1 font-medium">{error}</div>
          </div>
        )}

        {/* Product Selector */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Select Product *
          </label>
          <select
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value)}
            disabled={!!initialProduct}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          >
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku}) — Stock: {p.currentStock} {p.unit}
              </option>
            ))}
          </select>
        </div>

        {selectedProduct && (
          <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 text-xs flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <Package className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              <div>
                <div className="font-semibold text-slate-900 dark:text-white">
                  {selectedProduct.name}
                </div>
                <div className="text-[11px] text-slate-500 font-mono">
                  SKU: {selectedProduct.sku} · Category: {selectedProduct.categoryName || 'General'}
                </div>
              </div>
            </div>
            <div className="text-right">
              <div className="text-[11px] text-slate-500">Current Stock</div>
              <div className="font-bold text-sm text-slate-900 dark:text-white">
                {selectedProduct.currentStock} {selectedProduct.unit}
              </div>
            </div>
          </div>
        )}

        {/* Adjustment Mode Toggle */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Adjustment Action *
          </label>
          <div className="grid grid-cols-3 gap-2.5">
            <button
              type="button"
              onClick={() => setAdjustmentMode('INCREASE')}
              className={`p-3 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition-all ${
                adjustmentMode === 'INCREASE'
                  ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 ring-2 ring-emerald-500/20'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400'
              }`}
            >
              <ArrowUp className="w-4 h-4 text-emerald-600" />
              <span>Increase Stock (+)</span>
            </button>

            <button
              type="button"
              onClick={() => setAdjustmentMode('DECREASE')}
              className={`p-3 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition-all ${
                adjustmentMode === 'DECREASE'
                  ? 'border-rose-500 bg-rose-50 dark:bg-rose-950/40 text-rose-800 dark:text-rose-300 ring-2 ring-rose-500/20'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400'
              }`}
            >
              <ArrowDown className="w-4 h-4 text-rose-600" />
              <span>Deduct Stock (-)</span>
            </button>

            <button
              type="button"
              onClick={() => setAdjustmentMode('SET_EXACT')}
              className={`p-3 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition-all ${
                adjustmentMode === 'SET_EXACT'
                  ? 'border-indigo-500 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-800 dark:text-indigo-300 ring-2 ring-indigo-500/20'
                  : 'border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400'
              }`}
            >
              <Equal className="w-4 h-4 text-indigo-600" />
              <span>Set Exact Balance (=)</span>
            </button>
          </div>
        </div>

        {/* Quantity Input & Live Preview */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label={
              adjustmentMode === 'SET_EXACT'
                ? 'New Total Quantity *'
                : 'Adjustment Quantity *'
            }
            type="number"
            step="0.01"
            min="0.01"
            required
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0.00"
          />

          {/* Real-time Calculation Card */}
          <div className="p-3.5 rounded-xl border border-dashed border-slate-200 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-900/40 flex flex-col justify-center">
            <div className="text-[11px] text-slate-500 mb-1">Resulting Balance Preview</div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs text-slate-500 line-through">
                {currentStock}
              </span>
              <span className="text-slate-400">→</span>
              <span
                className={`font-mono text-base font-bold ${
                  resultingStock < 0
                    ? 'text-rose-600'
                    : resultingStock <= (selectedProduct?.minStockLevel ?? 5)
                    ? 'text-amber-600'
                    : 'text-emerald-600'
                }`}
              >
                {resultingStock} {selectedProduct?.unit || 'PCS'}
              </span>
            </div>
            {willBeNegative && (
              <span className="text-[10px] text-rose-600 font-semibold mt-1 flex items-center gap-1">
                <AlertTriangle className="w-3 h-3" /> Exceeds available stock
              </span>
            )}
          </div>
        </div>

        {/* Reason Selector */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Primary Reason *
          </label>
          <select
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          >
            {COMMON_REASONS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>

        {/* Additional Notes */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Additional Audit Notes
          </label>
          <textarea
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Provide context, batch numbers, or inspection notes for ledger review..."
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
          />
        </div>

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={mutation.isPending || willBeNegative || numQty <= 0}
            className="gap-2"
          >
            {mutation.isPending ? 'Processing...' : 'Apply Stock Adjustment'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
