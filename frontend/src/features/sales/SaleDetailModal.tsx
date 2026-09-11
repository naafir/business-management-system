import { Sale } from '../../types';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { formatCurrency } from '../../lib/utils';
import { User, Calendar, MapPin, CreditCard, Tag } from 'lucide-react';

interface SaleDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  sale: Sale | null;
}

export function SaleDetailModal({ isOpen, onClose, sale }: SaleDetailModalProps) {
  if (!sale) return null;

  const isIntraState = (sale.cgstAmount > 0 || sale.sgstAmount > 0) && sale.igstAmount === 0;

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
      title={`Sale Invoice: ${sale.saleNumber}`}
      className="max-w-4xl"
    >
      <div className="space-y-6 text-slate-800 dark:text-slate-200">
        {/* Header Metadata */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50 dark:bg-slate-800/50 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <User className="w-3.5 h-3.5 text-indigo-500" />
              Customer
            </span>
            <p className="font-medium text-slate-900 dark:text-white mt-0.5">{sale.customerName}</p>
            {sale.customerGstin ? (
              <p className="text-xs text-slate-500 dark:text-slate-400">GSTIN: {sale.customerGstin}</p>
            ) : (
              <p className="text-xs text-slate-400 italic">Unregistered / Walk-in</p>
            )}
          </div>

          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5 text-indigo-500" />
              Place of Supply & GST Type
            </span>
            <p className="font-medium text-slate-900 dark:text-white mt-0.5">
              {sale.placeOfSupplyState} ({sale.placeOfSupplyCode})
            </p>
            <div className="mt-1">
              {isIntraState ? (
                <span className="inline-flex items-center text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded border border-emerald-200 dark:border-emerald-800">
                  Intra-State (CGST + SGST)
                </span>
              ) : (
                <span className="inline-flex items-center text-[11px] font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 px-2 py-0.5 rounded border border-blue-200 dark:border-blue-800">
                  Inter-State (IGST)
                </span>
              )}
            </div>
          </div>

          <div>
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
              <CreditCard className="w-3.5 h-3.5 text-indigo-500" />
              Payment Status
            </span>
            <div className="mt-1 flex items-center gap-2">
              {getStatusBadge(sale.paymentStatus)}
              {sale.paymentMethod && (
                <span className="text-xs text-slate-500 dark:text-slate-400">
                  via {sale.paymentMethod}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1 mt-1">
              <Calendar className="w-3 h-3" /> Date: {sale.saleDate}
            </p>
          </div>
        </div>

        {/* Itemized Table */}
        <div>
          <h4 className="text-sm font-bold text-slate-900 dark:text-white mb-2 flex items-center gap-1.5">
            <Tag className="w-4 h-4 text-indigo-500" />
            Billed Products & Items
          </h4>
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-700">
                <tr>
                  <th className="py-2.5 px-3">Product</th>
                  <th className="py-2.5 px-3">HSN</th>
                  <th className="py-2.5 px-3 text-right">Qty</th>
                  <th className="py-2.5 px-3 text-right">Unit Price</th>
                  <th className="py-2.5 px-3 text-right">Disc %</th>
                  <th className="py-2.5 px-3 text-right">Taxable</th>
                  <th className="py-2.5 px-3 text-right">GST %</th>
                  <th className="py-2.5 px-3 text-right">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 dark:divide-slate-700/60">
                {sale.items?.map((item) => (
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
            {sale.notes && (
              <div className="text-xs bg-slate-50 dark:bg-slate-800 p-3 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="font-semibold text-slate-700 dark:text-slate-300 block mb-0.5">Notes / Terms:</span>
                <span className="text-slate-600 dark:text-slate-400 whitespace-pre-wrap">{sale.notes}</span>
              </div>
            )}
          </div>

          <div className="w-full md:w-72 space-y-1.5 text-xs bg-slate-50 dark:bg-slate-800/80 p-3.5 rounded-xl border border-slate-200 dark:border-slate-700">
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Subtotal:</span>
              <span className="font-medium">{formatCurrency(sale.subtotal)}</span>
            </div>
            {sale.totalDiscount > 0 && (
              <div className="flex justify-between text-emerald-600 dark:text-emerald-400">
                <span>Total Discount:</span>
                <span>-{formatCurrency(sale.totalDiscount)}</span>
              </div>
            )}
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Taxable Amount:</span>
              <span className="font-medium">{formatCurrency(sale.taxableAmount)}</span>
            </div>
            
            {/* Tax Breakdown */}
            {sale.cgstAmount > 0 && (
              <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                <span>CGST:</span>
                <span className="font-medium">{formatCurrency(sale.cgstAmount)}</span>
              </div>
            )}
            {sale.sgstAmount > 0 && (
              <div className="flex justify-between text-indigo-600 dark:text-indigo-400">
                <span>SGST:</span>
                <span className="font-medium">{formatCurrency(sale.sgstAmount)}</span>
              </div>
            )}
            {sale.igstAmount > 0 && (
              <div className="flex justify-between text-blue-600 dark:text-blue-400">
                <span>IGST:</span>
                <span className="font-medium">{formatCurrency(sale.igstAmount)}</span>
              </div>
            )}

            <div className="flex justify-between text-sm font-bold text-slate-900 dark:text-white border-t border-slate-200 dark:border-slate-700 pt-1.5 mt-1">
              <span>Grand Total:</span>
              <span className="text-indigo-600 dark:text-indigo-400">{formatCurrency(sale.grandTotal)}</span>
            </div>
            <div className="flex justify-between text-emerald-600 dark:text-emerald-400 font-medium pt-0.5">
              <span>Amount Received:</span>
              <span>{formatCurrency(sale.amountPaid)}</span>
            </div>
            <div className={`flex justify-between font-semibold pt-0.5 ${sale.balanceDue > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-slate-500'}`}>
              <span>Balance Receivable:</span>
              <span>{formatCurrency(sale.balanceDue)}</span>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
}
