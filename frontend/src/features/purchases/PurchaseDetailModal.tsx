import { Purchase } from '../../types';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { formatCurrency } from '../../lib/utils';
import { Building2, Calendar, FileText, User, CreditCard, Tag } from 'lucide-react';

interface PurchaseDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  purchase: Purchase | null;
}

export function PurchaseDetailModal({ isOpen, onClose, purchase }: PurchaseDetailModalProps) {
  if (!purchase) return null;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PAID':
        return <Badge variant="success">Paid</Badge>;
      case 'PARTIALLY_PAID':
        return <Badge variant="warning">Partially Paid</Badge>;
      case 'PENDING':
        return <Badge variant="danger">Pending</Badge>;
      default:
        return <Badge variant="default">{status}</Badge>;
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Purchase Record: ${purchase.purchaseNumber}`}
      className="max-w-4xl"
    >
      <div className="space-y-6 text-slate-800 dark:text-slate-200">
        {/* Header Metadata */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50 dark:bg-slate-800/50 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <Building2 className="w-3.5 h-3.5 text-indigo-500" />
              Supplier
            </span>
            <p className="font-medium text-slate-900 dark:text-white mt-0.5">{purchase.supplierName}</p>
            {purchase.supplierGstin && (
              <p className="text-xs text-slate-500 dark:text-slate-400">GSTIN: {purchase.supplierGstin}</p>
            )}
          </div>

          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-indigo-500" />
              Supplier Invoice #
            </span>
            <p className="font-medium text-slate-900 dark:text-white mt-0.5">
              {purchase.supplierInvoiceNumber || 'N/A'}
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1 mt-0.5">
              <Calendar className="w-3 h-3" /> Date: {purchase.purchaseDate}
            </p>
          </div>

          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <CreditCard className="w-3.5 h-3.5 text-indigo-500" />
              Payment Status
            </span>
            <div className="mt-1 flex items-center gap-2">
              {getStatusBadge(purchase.paymentStatus)}
              {purchase.paymentMethod && (
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  via {purchase.paymentMethod}
                </span>
              )}
            </div>
            {purchase.createdByName && (
              <p className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1 mt-1">
                <User className="w-3 h-3" /> Recorded by: {purchase.createdByName}
              </p>
            )}
          </div>
        </div>

        {/* Itemized Table */}
        <div>
          <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-1.5">
            <Tag className="w-4 h-4 text-indigo-500" />
            Purchased Line Items
          </h4>
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-700">
                <tr>
                  <th className="py-2.5 px-3">Product</th>
                  <th className="py-2.5 px-3">HSN</th>
                  <th className="py-2.5 px-3 text-right">Qty</th>
                  <th className="py-2.5 px-3 text-right">Unit Price</th>
                  <th className="py-2.5 px-3 text-right">Discount</th>
                  <th className="py-2.5 px-3 text-right">Taxable</th>
                  <th className="py-2.5 px-3 text-right">GST %</th>
                  <th className="py-2.5 px-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60">
                {purchase.items?.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30">
                    <td className="py-2 px-3">
                      <div className="font-medium text-slate-900 dark:text-white">{item.productName}</div>
                      {item.productSku && <div className="text-[10px] text-slate-400">SKU: {item.productSku}</div>}
                    </td>
                    <td className="py-2 px-3 text-slate-500 dark:text-slate-400">{item.hsnSac || '-'}</td>
                    <td className="py-2 px-3 text-right font-medium">{item.quantity} {item.unit}</td>
                    <td className="py-2 px-3 text-right">{formatCurrency(item.unitPrice)}</td>
                    <td className="py-2 px-3 text-right text-slate-500 dark:text-slate-400">
                      {item.discountPercent > 0 ? `${item.discountPercent}%` : '-'}
                    </td>
                    <td className="py-2 px-3 text-right">{formatCurrency(item.taxableAmount)}</td>
                    <td className="py-2 px-3 text-right text-indigo-600 dark:text-indigo-400">
                      {item.gstRatePercent}%
                    </td>
                    <td className="py-2 px-3 text-right font-semibold text-slate-900 dark:text-white">
                      {formatCurrency(item.totalAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Financial Totals Breakdown */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4 border-t border-slate-200 dark:border-slate-700 pt-4">
          <div className="max-w-md">
            {purchase.notes && (
              <div className="text-xs bg-slate-50 dark:bg-slate-800 p-3 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="font-semibold text-slate-700 dark:text-slate-300 block mb-0.5">Notes / References:</span>
                <span className="text-slate-600 dark:text-slate-400 whitespace-pre-wrap">{purchase.notes}</span>
              </div>
            )}
          </div>

          <div className="w-full md:w-72 space-y-1.5 text-xs bg-slate-50 dark:bg-slate-800/80 p-3.5 rounded-xl border border-slate-200 dark:border-slate-700">
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Subtotal:</span>
              <span className="font-medium">{formatCurrency(purchase.subtotal)}</span>
            </div>
            {purchase.totalDiscount > 0 && (
              <div className="flex justify-between text-emerald-600 dark:text-emerald-400">
                <span>Total Discount:</span>
                <span>-{formatCurrency(purchase.totalDiscount)}</span>
              </div>
            )}
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Taxable Amount:</span>
              <span className="font-medium">{formatCurrency(purchase.taxableAmount)}</span>
            </div>
            <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
              <span>Total Input GST:</span>
              <span className="font-medium">{formatCurrency(purchase.totalTax)}</span>
            </div>
            {purchase.cgstAmount > 0 && (
              <div className="flex justify-between text-[11px] text-slate-500 pl-2">
                <span>CGST + SGST:</span>
                <span>{formatCurrency(purchase.cgstAmount + purchase.sgstAmount)}</span>
              </div>
            )}
            {purchase.igstAmount > 0 && (
              <div className="flex justify-between text-[11px] text-slate-500 pl-2">
                <span>IGST:</span>
                <span>{formatCurrency(purchase.igstAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-sm font-bold text-slate-900 dark:text-white border-t border-slate-200 dark:border-slate-700 pt-1.5 mt-1">
              <span>Grand Total:</span>
              <span className="text-indigo-600 dark:text-indigo-400">{formatCurrency(purchase.grandTotal)}</span>
            </div>
            <div className="flex justify-between text-emerald-600 dark:text-emerald-400 font-medium pt-0.5">
              <span>Amount Paid:</span>
              <span>{formatCurrency(purchase.amountPaid)}</span>
            </div>
            <div className={`flex justify-between font-semibold pt-0.5 ${purchase.balanceDue > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-slate-500'}`}>
              <span>Balance Due:</span>
              <span>{formatCurrency(purchase.balanceDue)}</span>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}
