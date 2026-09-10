import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Product, ProductCategory, GstRate } from '../../types';
import { UOM_OPTIONS } from '../../lib/constants';
import { Plus } from 'lucide-react';
import { CategoryModal } from './CategoryModal';

interface ProductModalProps {
  isOpen: boolean;
  onClose: () => void;
  product?: Product | null;
}

export function ProductModal({ isOpen, onClose, product }: ProductModalProps) {
  const queryClient = useQueryClient();
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    sku: '',
    name: '',
    brand: '',
    categoryId: '',
    hsnSac: '',
    unit: 'PCS',
    purchasePrice: '0.00',
    sellingPrice: '0.00',
    gstRateId: '',
    openingStock: '0.00',
    minStockLevel: '5.00',
    allowNegativeStock: false,
    description: '',
  });

  const { data: categories = [] } = useQuery<ProductCategory[]>({
    queryKey: ['categories'],
    queryFn: () => api.get<ProductCategory[]>('/categories'),
    enabled: isOpen,
  });

  const { data: gstRates = [] } = useQuery<GstRate[]>({
    queryKey: ['gstRates'],
    queryFn: () => api.get<GstRate[]>('/gst/rates'),
    enabled: isOpen,
  });

  useEffect(() => {
    if (product) {
      setFormData({
        sku: product.sku,
        name: product.name,
        brand: product.brand || '',
        categoryId: product.categoryId || '',
        hsnSac: product.hsnSac,
        unit: product.unit,
        purchasePrice: product.purchasePrice.toString(),
        sellingPrice: product.sellingPrice.toString(),
        gstRateId: product.gstRateId || '',
        openingStock: product.openingStock.toString(),
        minStockLevel: product.minStockLevel.toString(),
        allowNegativeStock: product.allowNegativeStock,
        description: product.description || '',
      });
    } else {
      setFormData({
        sku: '',
        name: '',
        brand: '',
        categoryId: categories[0]?.id || '',
        hsnSac: '',
        unit: 'PCS',
        purchasePrice: '0.00',
        sellingPrice: '0.00',
        gstRateId: gstRates.find(r => r.ratePercent === 18)?.id || gstRates[0]?.id || '',
        openingStock: '0.00',
        minStockLevel: '5.00',
        allowNegativeStock: false,
        description: '',
      });
    }
    setError(null);
  }, [product, isOpen, categories.length, gstRates.length]);

  const mutation = useMutation({
    mutationFn: (payload: any) => {
      if (product) {
        return api.put<Product>(`/products/${product.id}`, payload);
      } else {
        return api.post<Product>('/products', payload);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Failed to save product');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const payload: any = {
      name: formData.name.trim(),
      brand: formData.brand.trim() || undefined,
      categoryId: formData.categoryId || undefined,
      hsnSac: formData.hsnSac.trim(),
      unit: formData.unit,
      purchasePrice: parseFloat(formData.purchasePrice) || 0,
      sellingPrice: parseFloat(formData.sellingPrice) || 0,
      gstRateId: formData.gstRateId || undefined,
      minStockLevel: parseFloat(formData.minStockLevel) || 0,
      allowNegativeStock: formData.allowNegativeStock,
      description: formData.description.trim() || undefined,
    };

    if (!product) {
      payload.sku = formData.sku.trim();
      payload.openingStock = parseFloat(formData.openingStock) || 0;
    }

    mutation.mutate(payload);
  };

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title={product ? 'Edit Product' : 'Add New Product'}
        description={
          product
            ? `Update details and inventory rules for ${product.name}`
            : 'Define item attributes, pricing, GST tax rates, and opening stock.'
        }
        className="max-w-2xl"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 text-xs rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800">
              {error}
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="SKU Code *"
              placeholder="e.g., LAP-DELL-XPS15"
              value={formData.sku}
              onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
              disabled={!!product}
              required
            />

            <Input
              label="Product Name *"
              placeholder="e.g., Dell XPS 15 Laptop"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Category
                </label>
                <button
                  type="button"
                  onClick={() => setIsCategoryModalOpen(true)}
                  className="text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-0.5"
                >
                  <Plus className="w-3 h-3" /> New
                </button>
              </div>
              <select
                value={formData.categoryId}
                onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
              >
                <option value="">Select Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <Input
              label="Brand"
              placeholder="e.g., Dell, Apple, Samsung"
              value={formData.brand}
              onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
            />

            <Input
              label="HSN / SAC Code *"
              placeholder="e.g., 8471"
              value={formData.hsnSac}
              onChange={(e) => setFormData({ ...formData, hsnSac: e.target.value })}
              required
            />

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Unit of Measurement *
              </label>
              <select
                value={formData.unit}
                onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
              >
                {UOM_OPTIONS.map((u) => (
                  <option key={u.value} value={u.value}>
                    {u.label}
                  </option>
                ))}
              </select>
            </div>

            <Input
              label="Purchase Price (₹) *"
              type="number"
              step="0.01"
              min="0"
              value={formData.purchasePrice}
              onChange={(e) => setFormData({ ...formData, purchasePrice: e.target.value })}
              required
            />

            <Input
              label="Selling Price (₹) *"
              type="number"
              step="0.01"
              min="0"
              value={formData.sellingPrice}
              onChange={(e) => setFormData({ ...formData, sellingPrice: e.target.value })}
              required
            />

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Applicable GST Rate
              </label>
              <select
                value={formData.gstRateId}
                onChange={(e) => setFormData({ ...formData, gstRateId: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
              >
                <option value="">No GST / Exempt</option>
                {gstRates.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.ratePercent}% - {r.description}
                  </option>
                ))}
              </select>
            </div>

            <Input
              label="Low Stock Alert Threshold"
              type="number"
              step="1"
              min="0"
              value={formData.minStockLevel}
              onChange={(e) => setFormData({ ...formData, minStockLevel: e.target.value })}
            />

            {!product && (
              <Input
                label="Opening Stock"
                type="number"
                step="0.01"
                min="0"
                value={formData.openingStock}
                onChange={(e) => setFormData({ ...formData, openingStock: e.target.value })}
              />
            )}

            <div className="flex items-center gap-3 pt-6">
              <input
                type="checkbox"
                id="allowNegativeStock"
                checked={formData.allowNegativeStock}
                onChange={(e) => setFormData({ ...formData, allowNegativeStock: e.target.checked })}
                className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300 dark:border-slate-700"
              />
              <label htmlFor="allowNegativeStock" className="text-xs text-slate-700 dark:text-slate-300 cursor-pointer">
                Allow Negative Stock on Sales
              </label>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              Description / Notes
            </label>
            <textarea
              rows={2}
              placeholder="Additional product specifications..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" isLoading={mutation.isPending}>
              {product ? 'Save Changes' : 'Create Product'}
            </Button>
          </div>
        </form>
      </Modal>

      <CategoryModal
        isOpen={isCategoryModalOpen}
        onClose={() => setIsCategoryModalOpen(false)}
        onCreated={(cat) => setFormData((prev) => ({ ...prev, categoryId: cat.id }))}
      />
    </>
  );
}
