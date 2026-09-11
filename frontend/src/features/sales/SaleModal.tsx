import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Customer, Product, CreateSaleRequest, SaleItemRequest, PaymentMethod, PageResponse, BusinessSettings } from '../../types';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { formatCurrency } from '../../lib/utils';
import { Plus, Trash2, ShoppingCart, AlertCircle } from 'lucide-react';

interface SaleModalProps {
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
  currentStock: number;
}

const INDIAN_STATES = [
  { code: '37', name: 'Andhra Pradesh' },
  { code: '12', name: 'Arunachal Pradesh' },
  { code: '18', name: 'Assam' },
  { code: '10', name: 'Bihar' },
  { code: '22', name: 'Chhattisgarh' },
  { code: '30', name: 'Goa' },
  { code: '24', name: 'Gujarat' },
  { code: '06', name: 'Haryana' },
  { code: '02', name: 'Himachal Pradesh' },
  { code: '20', name: 'Jharkhand' },
  { code: '29', name: 'Karnataka' },
  { code: '32', name: 'Kerala' },
  { code: '23', name: 'Madhya Pradesh' },
  { code: '27', name: 'Maharashtra' },
  { code: '14', name: 'Manipur' },
  { code: '17', name: 'Meghalaya' },
  { code: '15', name: 'Mizoram' },
  { code: '13', name: 'Nagaland' },
  { code: '21', name: 'Odisha' },
  { code: '03', name: 'Punjab' },
  { code: '08', name: 'Rajasthan' },
  { code: '11', name: 'Sikkim' },
  { code: '33', name: 'Tamil Nadu' },
  { code: '36', name: 'Telangana' },
  { code: '16', name: 'Tripura' },
  { code: '09', name: 'Uttar Pradesh' },
  { code: '05', name: 'Uttarakhand' },
  { code: '19', name: 'West Bengal' },
  { code: '07', name: 'Delhi' },
  { code: '01', name: 'Jammu and Kashmir' },
  { code: '31', name: 'Lakshadweep' },
  { code: '34', name: 'Puducherry' }
];

export function SaleModal({ isOpen, onClose }: SaleModalProps) {
  const queryClient = useQueryClient();

  const [customerId, setCustomerId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerGstin, setCustomerGstin] = useState('');
  const [placeOfSupplyCode, setPlaceOfSupplyCode] = useState('27');
  const [placeOfSupplyState, setPlaceOfSupplyState] = useState('Maharashtra');
  const [saleDate, setSaleDate] = useState(new Date().toISOString().split('T')[0]);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [amountPaid, setAmountPaid] = useState<number | ''>('');
  const [notes, setNotes] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const [items, setItems] = useState<FormLineItem[]>([
    { productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs', currentStock: 0 }
  ]);

  // Fetch business profile settings
  const { data: businessSettings } = useQuery<BusinessSettings>({
    queryKey: ['settings'],
    queryFn: () => api.get<BusinessSettings>('/settings/profile'),
    enabled: isOpen,
  });

  // Fetch active customers
  const { data: customerPage } = useQuery<PageResponse<Customer>>({
    queryKey: ['customers-dropdown'],
    queryFn: () => api.get<PageResponse<Customer>>('/customers?activeOnly=true&size=100'),
    enabled: isOpen,
  });

  // Fetch active products
  const { data: productPage } = useQuery<PageResponse<Product>>({
    queryKey: ['products-dropdown'],
    queryFn: () => api.get<PageResponse<Product>>('/products?activeOnly=true&size=100'),
    enabled: isOpen,
  });

  const customers = customerPage?.content || [];
  const products = productPage?.content || [];

  const businessStateCode = businessSettings?.stateCode || '27';
  const isIntraState = placeOfSupplyCode === businessStateCode;

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setCustomerId('');
      setCustomerName('');
      setCustomerGstin('');
      const defaultCode = businessSettings?.stateCode || '27';
      const defaultState = INDIAN_STATES.find(s => s.code === defaultCode)?.name || 'Maharashtra';
      setPlaceOfSupplyCode(defaultCode);
      setPlaceOfSupplyState(defaultState);
      setSaleDate(new Date().toISOString().split('T')[0]);
      setPaymentMethod('CASH');
      setAmountPaid('');
      setNotes('');
      setFormError(null);
      setItems([{ productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs', currentStock: 0 }]);
    }
  }, [isOpen, businessSettings]);

  const handleCustomerSelect = (cId: string) => {
    setCustomerId(cId);
    if (!cId) {
      setCustomerName('');
      setCustomerGstin('');
      return;
    }
    const selectedCust = customers.find(c => c.id === cId);
    if (selectedCust) {
      setCustomerName(selectedCust.name);
      setCustomerGstin(selectedCust.gstin || '');
      if (selectedCust.stateCode) {
        setPlaceOfSupplyCode(selectedCust.stateCode);
        const stateObj = INDIAN_STATES.find(s => s.code === selectedCust.stateCode);
        if (stateObj) setPlaceOfSupplyState(stateObj.name);
      }
    }
  };

  const handleStateChange = (code: string) => {
    setPlaceOfSupplyCode(code);
    const stateObj = INDIAN_STATES.find(s => s.code === code);
    if (stateObj) setPlaceOfSupplyState(stateObj.name);
  };

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
      unit: selectedProd.unit || 'pcs',
      currentStock: selectedProd.currentStock || 0
    };
    setItems(newItems);
  };

  const handleItemChange = (index: number, field: keyof FormLineItem, val: any) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: val };
    setItems(newItems);
  };

  const addItemRow = () => {
    setItems(prev => [...prev, { productId: '', productName: '', quantity: 1, unitPrice: 0, discountPercent: 0, gstRatePercent: 0, unit: 'pcs', currentStock: 0 }]);
  };

  const removeItemRow = (index: number) => {
    if (items.length <= 1) return;
    setItems(prev => prev.filter((_, i) => i !== index));
  };

  // Calculations
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

  const cgstAmount = isIntraState ? totals.tax / 2 : 0;
  const sgstAmount = isIntraState ? totals.tax / 2 : 0;
  const igstAmount = isIntraState ? 0 : totals.tax;

  const effectiveAmountPaid = amountPaid === '' ? totals.grandTotal : Number(amountPaid);

  const createMutation = useMutation({
    mutationFn: (req: CreateSaleRequest) => api.post('/sales', req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['sales-summary'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      onClose();
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to record sale invoice.');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const validItems: SaleItemRequest[] = items
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

    const requestBody: CreateSaleRequest = {
      customerId: customerId || undefined,
      customerName: customerName.trim() || undefined,
      customerGstin: customerGstin.trim() || undefined,
      placeOfSupplyState,
      placeOfSupplyCode,
      saleDate,
      items: validItems,
      amountPaid: effectiveAmountPaid,
      paymentMethod,
      notes: notes.trim() || undefined
    };

    createMutation.mutate(requestBody);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create New Customer Sale Invoice"
      className="max-w-4xl"
    >
      <form onSubmit={handleSubmit} className="space-y-5 text-slate-800 dark:text-slate-200">
        {formError && (
          <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-lg flex items-center gap-2 text-xs text-red-600 dark:text-red-400">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {/* Customer & Place of Supply */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-slate-50 dark:bg-slate-800/50 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
          <div className="md:col-span-2">
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Customer Selection
            </label>
            <select
              value={customerId}
              onChange={(e) => handleCustomerSelect(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              <option value="">-- Walk-in / Retail Customer (Unregistered) --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {c.gstin ? `(${c.gstin})` : ''}
                </option>
              ))}
            </select>

            {!customerId && (
              <div className="grid grid-cols-2 gap-2 mt-2">
                <Input
                  type="text"
                  placeholder="Customer Name (optional)"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  className="text-xs"
                />
                <Input
                  type="text"
                  placeholder="Customer GSTIN (optional)"
                  value={customerGstin}
                  onChange={(e) => setCustomerGstin(e.target.value)}
                  className="text-xs"
                />
              </div>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Place of Supply <span className="text-red-500">*</span>
            </label>
            <select
              value={placeOfSupplyCode}
              onChange={(e) => handleStateChange(e.target.value)}
              className="w-full px-3 py-2 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            >
              {INDIAN_STATES.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.code} - {s.name}
                </option>
              ))}
            </select>
            <div className="mt-1">
              {isIntraState ? (
                <span className="text-[10px] font-semibold text-emerald-600 dark:text-emerald-400">
                  ✓ Intra-State (CGST + SGST)
                </span>
              ) : (
                <span className="text-[10px] font-semibold text-blue-600 dark:text-blue-400">
                  ⚡ Inter-State (IGST)
                </span>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Sale Date <span className="text-red-500">*</span>
            </label>
            <Input
              type="date"
              value={saleDate}
              onChange={(e) => setSaleDate(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Line Items Table */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
              <ShoppingCart className="w-4 h-4 text-indigo-500" />
              Sale Items
            </label>
            <Button type="button" variant="outline" size="sm" onClick={addItemRow} className="text-xs py-1">
              <Plus className="w-3.5 h-3.5 mr-1" /> Add Item Line
            </Button>
          </div>

          <div className="overflow-x-auto border border-slate-200 dark:border-slate-700 rounded-lg">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-700">
                <tr>
                  <th className="py-2.5 px-3 min-w-[200px]">Product</th>
                  <th className="py-2.5 px-3 w-20 text-center">Stock</th>
                  <th className="py-2.5 px-3 w-20 text-center">Qty</th>
                  <th className="py-2.5 px-3 w-28 text-right">Selling Price</th>
                  <th className="py-2.5 px-3 w-20 text-right">Disc %</th>
                  <th className="py-2.5 px-3 w-20 text-right">GST %</th>
                  <th className="py-2.5 px-3 w-28 text-right">Total (₹)</th>
                  <th className="py-2.5 px-2 w-10 text-center"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60">
                {items.map((item, idx) => {
                  const rowTotals = calculateRowTotals(item);
                  const isStockLow = item.productId && item.quantity > item.currentStock;
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
                              {p.name} ({p.sku}) - Stock: {p.currentStock} {p.unit}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="py-2 px-3 text-center text-slate-500 dark:text-slate-400">
                        {item.productId ? `${item.currentStock} ${item.unit}` : '-'}
                      </td>
                      <td className="py-2 px-3">
                        <Input
                          type="number"
                          min="0.01"
                          step="any"
                          value={item.quantity}
                          onChange={(e) => handleItemChange(idx, 'quantity', e.target.value)}
                          className={`text-center py-1 px-1 text-xs ${isStockLow ? 'border-amber-500 text-amber-600 font-bold' : ''}`}
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

        {/* Footer & Totals */}
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
                <option value="UPI">UPI / GPay / PhonePe</option>
                <option value="CARD">Debit / Credit Card</option>
                <option value="BANK_TRANSFER">Bank Transfer (NEFT/IMPS)</option>
                <option value="CHEQUE">Cheque</option>
                <option value="CREDIT">Customer Credit (On Account)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Amount Received (₹)
              </label>
              <Input
                type="number"
                min="0"
                step="any"
                placeholder={`Default: Full amount (${formatCurrency(totals.grandTotal)})`}
                value={amountPaid}
                onChange={(e) => setAmountPaid(e.target.value === '' ? '' : Number(e.target.value))}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Notes / Special Terms
              </label>
              <textarea
                rows={2}
                placeholder="Optional customer billing notes or warranty info..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full px-3 py-1.5 text-xs rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="bg-slate-50 dark:bg-slate-800/70 p-4 rounded-xl border border-slate-200 dark:border-slate-700 flex flex-col justify-between">
            <div className="space-y-2 text-xs">
              <div className="flex justify-between text-slate-600 dark:text-slate-400">
                <span>Taxable Amount:</span>
                <span className="font-semibold text-slate-900 dark:text-white">{formatCurrency(totals.taxable)}</span>
              </div>
              {isIntraState ? (
                <>
                  <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                    <span>CGST:</span>
                    <span className="font-semibold">{formatCurrency(cgstAmount)}</span>
                  </div>
                  <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                    <span>SGST:</span>
                    <span className="font-semibold">{formatCurrency(sgstAmount)}</span>
                  </div>
                </>
              ) : (
                <div className="flex justify-between text-blue-600 dark:text-blue-400">
                  <span>IGST:</span>
                  <span className="font-semibold">{formatCurrency(igstAmount)}</span>
                </div>
              )}
              <div className="flex justify-between text-sm font-bold text-slate-900 dark:text-white border-t border-slate-200 dark:border-slate-700 pt-2">
                <span>Grand Total:</span>
                <span className="text-indigo-600 dark:text-indigo-400">{formatCurrency(totals.grandTotal)}</span>
              </div>
              <div className="flex justify-between text-xs text-slate-500 pt-1">
                <span>Amount Received:</span>
                <span className="font-medium text-emerald-600 dark:text-emerald-400">{formatCurrency(effectiveAmountPaid)}</span>
              </div>
              <div className="flex justify-between text-xs font-bold text-amber-600 dark:text-amber-400 pt-0.5">
                <span>Balance Due:</span>
                <span>{formatCurrency(Math.max(0, totals.grandTotal - effectiveAmountPaid))}</span>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" isLoading={createMutation.isPending}>
                Create Sale Invoice
              </Button>
            </div>
          </div>
        </div>
      </form>
    </Modal>
  );
}
