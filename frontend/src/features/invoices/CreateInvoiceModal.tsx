import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Customer, Product, Sale, CreateInvoiceRequest, InvoiceItemRequest, PaymentMethod, PageResponse } from '../../types';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../lib/utils';
import { Plus, Trash2, ShoppingBag, AlertCircle, RefreshCw } from 'lucide-react';

interface CreateInvoiceModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface FormLineItem {
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  gstRatePercent: number;
  unit: string;
}

export function CreateInvoiceModal({ isOpen, onClose }: CreateInvoiceModalProps) {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'sale' | 'custom'>('custom');

  // Direct Invoice state
  const [selectedSaleId, setSelectedSaleId] = useState('');
  const [customerId, setCustomerId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerGstin, setCustomerGstin] = useState('');
  const [placeOfSupplyCode] = useState('27');
  const [placeOfSupplyState] = useState('Maharashtra');
  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().split('T')[0]);
  const [dueDate, setDueDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [amountPaid, setAmountPaid] = useState<number | ''>('');
  const [notes, setNotes] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const [items, setItems] = useState<FormLineItem[]>([
    { productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs' }
  ]);

  // Fetch sales without invoices
  const { data: salesPage } = useQuery<PageResponse<Sale>>({
    queryKey: ['sales-for-invoices'],
    queryFn: () => api.get<PageResponse<Sale>>('/sales?size=50'),
    enabled: isOpen && tab === 'sale',
  });

  // Fetch active customers
  const { data: customerPage } = useQuery<PageResponse<Customer>>({
    queryKey: ['customers-dropdown'],
    queryFn: () => api.get<PageResponse<Customer>>('/customers?activeOnly=true&size=100'),
    enabled: isOpen && tab === 'custom',
  });

  // Fetch active products
  const { data: productPage } = useQuery<PageResponse<Product>>({
    queryKey: ['products-dropdown'],
    queryFn: () => api.get<PageResponse<Product>>('/products?activeOnly=true&size=100'),
    enabled: isOpen && tab === 'custom',
  });

  const sales = salesPage?.content || [];
  const customers = customerPage?.content || [];
  const products = productPage?.content || [];

  useEffect(() => {
    if (isOpen) {
      setTab('custom');
      setSelectedSaleId('');
      setCustomerId('');
      setCustomerName('');
      setCustomerGstin('');
      setInvoiceDate(new Date().toISOString().split('T')[0]);
      setDueDate('');
      setPaymentMethod('CASH');
      setAmountPaid('');
      setNotes('');
      setFormError(null);
      setItems([{ productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs' }]);
    }
  }, [isOpen]);

  const handleProductSelect = (index: number, pId: string) => {
    const selectedProd = products.find(p => p.id === pId);
    if (!selectedProd) return;

    const newItems = [...items];
    newItems[index] = {
      productId: selectedProd.id,
      productName: selectedProd.name,
      quantity: newItems[index].quantity || 1,
      unitPrice: selectedProd.sellingPrice || 0,
      discountPercent: 0,
      gstRatePercent: selectedProd.gstRatePercent || 0,
      unit: selectedProd.unit || 'pcs'
    };
    setItems(newItems);
  };

  const handleItemChange = (index: number, field: keyof FormLineItem, val: any) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: val };
    setItems(newItems);
  };

  const addItemRow = () => {
    setItems(prev => [...prev, { productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs' }]);
  };

  const removeItemRow = (index: number) => {
    if (items.length <= 1) return;
    setItems(prev => prev.filter((_, i) => i !== index));
  };

  const calculateRowTotals = (item: FormLineItem) => {
    const qty = Number(item.quantity) || 0;
    const price = Number(item.unitPrice) || 0;
    const disc = Number(item.discountPercent) || 0;
    const gstRate = Number(item.gstRatePercent) || 0;

    const gross = qty * price;
    const discountAmt = (gross * disc) / 100;
    const taxable = gross - discountAmt;
    const taxAmt = (taxable * gstRate) / 100;
    const total = taxable + taxAmt;

    return { taxable, taxAmt, total };
  };

  const totals = items.reduce(
    (acc, item) => {
      const res = calculateRowTotals(item);
      acc.taxable += res.taxable;
      acc.tax += res.taxAmt;
      acc.grandTotal += res.total;
      return acc;
    },
    { taxable: 0, tax: 0, grandTotal: 0 }
  );

  const createMutation = useMutation({
    mutationFn: (req: CreateInvoiceRequest) => api.post('/invoices', req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      queryClient.invalidateQueries({ queryKey: ['invoices-summary'] });
      onClose();
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to create invoice.');
    }
  });

  const createFromSaleMutation = useMutation({
    mutationFn: (saleId: string) => api.post(`/invoices/from-sale/${saleId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      queryClient.invalidateQueries({ queryKey: ['invoices-summary'] });
      onClose();
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to generate invoice from sale.');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (tab === 'sale') {
      if (!selectedSaleId) {
        setFormError('Please select a sale transaction.');
        return;
      }
      createFromSaleMutation.mutate(selectedSaleId);
      return;
    }

    const validItems: InvoiceItemRequest[] = items
      .filter(i => i.productId !== '')
      .map(i => ({
        productId: i.productId,
        quantity: Number(i.quantity),
        unitPrice: Number(i.unitPrice),
        discountPercent: Number(i.discountPercent) || 0
      }));

    if (validItems.length === 0) {
      setFormError('Please add at least one valid product line item.');
      return;
    }

    const requestBody: CreateInvoiceRequest = {
      customerId: customerId || undefined,
      customerName: customerName.trim() || undefined,
      customerGstin: customerGstin.trim() || undefined,
      placeOfSupplyState,
      placeOfSupplyCode,
      invoiceDate,
      dueDate: dueDate || undefined,
      items: validItems,
      amountPaid: amountPaid === '' ? totals.grandTotal : Number(amountPaid),
      paymentMethod,
      notes: notes.trim() || undefined
    };

    createMutation.mutate(requestBody);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create GST Tax Invoice"
      className="max-w-4xl"
    >
      <div className="space-y-5 text-slate-800 dark:text-slate-200">
        {/* Mode Selector */}
        <div className="flex border-b border-slate-200 dark:border-slate-700">
          <button
            type="button"
            onClick={() => setTab('custom')}
            className={`pb-2.5 px-4 text-xs font-semibold border-b-2 transition-colors ${
              tab === 'custom'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            Create Direct Tax Invoice
          </button>
          <button
            type="button"
            onClick={() => setTab('sale')}
            className={`pb-2.5 px-4 text-xs font-semibold border-b-2 transition-colors ${
              tab === 'sale'
                ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
                : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            Convert Existing Sale to Invoice
          </button>
        </div>

        {formError && (
          <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-lg flex items-center gap-2 text-xs text-red-600 dark:text-red-400">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          {tab === 'sale' ? (
            <div className="space-y-4 py-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Select Sale Transaction <span className="text-red-500">*</span>
                </label>
                <select
                  value={selectedSaleId}
                  onChange={(e) => setSelectedSaleId(e.target.value)}
                  className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  <option value="">-- Choose Sale Order --</option>
                  {sales.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.saleNumber} - {s.customerName} ({s.saleDate}) - {formatCurrency(s.grandTotal)}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                  Converting a sale automatically finalizes the invoice and inherits all line items, tax split, and customer details.
                </p>
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button type="button" variant="outline" onClick={onClose}>
                  Cancel
                </Button>
                <Button type="submit" isLoading={createFromSaleMutation.isPending}>
                  <RefreshCw className="w-3.5 h-3.5 mr-1.5" /> Convert & Finalize Invoice
                </Button>
              </div>
            </div>
          ) : (
            <>
              {/* Direct Form */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Customer
                  </label>
                  <select
                    value={customerId}
                    onChange={(e) => {
                      setCustomerId(e.target.value);
                      const c = customers.find(c => c.id === e.target.value);
                      if (c) {
                        setCustomerName(c.name);
                        setCustomerGstin(c.gstin || '');
                      }
                    }}
                    className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  >
                    <option value="">-- Walk-in Customer --</option>
                    {customers.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name} {c.gstin ? `(${c.gstin})` : ''}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Invoice Date <span className="text-red-500">*</span>
                  </label>
                  <Input
                    type="date"
                    value={invoiceDate}
                    onChange={(e) => setInvoiceDate(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Due Date
                  </label>
                  <Input
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                  />
                </div>
              </div>

              {/* Items table */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                    <ShoppingBag className="w-4 h-4 text-indigo-500" />
                    Invoice Items
                  </label>
                  <Button type="button" variant="outline" size="sm" onClick={addItemRow} className="text-xs py-1">
                    <Plus className="w-3.5 h-3.5 mr-1" /> Add Product Line
                  </Button>
                </div>

                <div className="overflow-x-auto border border-slate-200 dark:border-slate-700 rounded-lg">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-700">
                      <tr>
                        <th className="py-2.5 px-3 min-w-[200px]">Product</th>
                        <th className="py-2.5 px-3 w-20 text-center">Qty</th>
                        <th className="py-2.5 px-3 w-28 text-right">Unit Price</th>
                        <th className="py-2.5 px-3 w-20 text-right">Disc %</th>
                        <th className="py-2.5 px-3 w-20 text-right">GST %</th>
                        <th className="py-2.5 px-3 w-28 text-right">Total (₹)</th>
                        <th className="py-2.5 px-2 w-10 text-center"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60">
                      {items.map((item, idx) => {
                        const rowTotals = calculateRowTotals(item);
                        return (
                          <tr key={idx} className="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                            <td className="py-2 px-3">
                              <select
                                value={item.productId}
                                onChange={(e) => handleProductSelect(idx, e.target.value)}
                                className="w-full px-2 py-1 text-xs rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                              >
                                <option value="">-- Choose Product --</option>
                                {products.map((p) => (
                                  <option key={p.id} value={p.id}>
                                    {p.name} ({p.sku})
                                  </option>
                                ))}
                              </select>
                            </td>
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                min="0.01"
                                step="any"
                                value={item.quantity}
                                onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                                className="text-center py-1 px-1 text-xs"
                              />
                            </td>
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                min="0"
                                step="any"
                                value={item.unitPrice}
                                onChange={(e) => handleItemChange(idx, 'unitPrice', e.target.value)}
                                className="text-right py-1 px-1 text-xs"
                              />
                            </td>
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                min="0"
                                max="100"
                                step="any"
                                value={item.discountPercent}
                                onChange={(e) => handleItemChange(idx, 'discountPercent', e.target.value)}
                                className="text-right py-1 px-1 text-xs"
                              />
                            </td>
                            <td className="py-2 px-3 text-right font-medium text-indigo-600 dark:text-indigo-400">
                              {item.gstRatePercent}%
                            </td>
                            <td className="py-2 px-3 text-right font-bold text-slate-900 dark:text-white">
                              {formatCurrency(rowTotals.total)}
                            </td>
                            <td className="py-2 px-2 text-center">
                              <button
                                type="button"
                                onClick={() => removeItemRow(idx)}
                                disabled={items.length <= 1}
                                className="text-slate-400 hover:text-red-500 disabled:opacity-30"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Totals & Submit */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 border-t border-slate-200 dark:border-slate-700 pt-4">
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                      Payment Method
                    </label>
                    <select
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                      className="w-full px-3 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                    >
                      <option value="CASH">Cash</option>
                      <option value="UPI">UPI / Digital Payout</option>
                      <option value="BANK_TRANSFER">Bank Transfer (NEFT/RTGS)</option>
                      <option value="CARD">Debit / Credit Card</option>
                      <option value="CREDIT">Customer Credit</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                      Notes
                    </label>
                    <textarea
                      rows={2}
                      placeholder="Optional invoice notes..."
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      className="w-full px-3 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                    />
                  </div>
                </div>

                <div className="bg-slate-50 dark:bg-slate-800/70 p-4 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2 text-xs flex flex-col justify-between">
                  <div className="space-y-2">
                    <div className="flex justify-between text-slate-600 dark:text-slate-400">
                      <span>Taxable Value:</span>
                      <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(totals.taxable)}</span>
                    </div>
                    <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                      <span>GST Amount:</span>
                      <span className="font-semibold">{formatCurrency(totals.tax)}</span>
                    </div>
                    <div className="flex justify-between text-sm font-bold text-slate-900 dark:text-white border-t border-slate-200 dark:border-slate-700 pt-2">
                      <span>Grand Total:</span>
                      <span className="text-indigo-600 dark:text-indigo-400">{formatCurrency(totals.grandTotal)}</span>
                    </div>
                  </div>

                  <div className="flex justify-end gap-2 pt-3">
                    <Button type="button" variant="outline" onClick={onClose}>
                      Cancel
                    </Button>
                    <Button type="submit" isLoading={createMutation.isPending}>
                      Create Tax Invoice
                    </Button>
                  </div>
                </div>
              </div>
            </>
          )}
        </form>
      </div>
    </Modal>
  );
}
