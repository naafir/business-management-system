export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface UserSummary {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: UserSummary;
}

export interface BusinessSettings {
  id: string;
  legalName: string;
  tradeName?: string;
  gstin?: string;
  pan?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  stateName: string;
  stateCode: string;
  pinCode?: string;
  phone?: string;
  email?: string;
  website?: string;
  logoUrl?: string;
  bankName?: string;
  bankAccountNumber?: string;
  bankIfsc?: string;
  bankBranch?: string;
  bankUpiId?: string;
  invoicePrefix: string;
  invoiceNextSeq: number;
  invoiceTerms?: string;
  defaultCurrency: string;
  updatedAt?: string;
}

export interface GstRate {
  id: string;
  ratePercent: number;
  description: string;
  active: boolean;
}

export interface ProductCategory {
  id: string;
  name: string;
  description?: string;
  active: boolean;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  description?: string;
  categoryId?: string;
  categoryName?: string;
  brand?: string;
  hsnSac: string;
  unit: string;
  purchasePrice: number;
  sellingPrice: number;
  gstRateId?: string;
  gstRatePercent?: number;
  openingStock: number;
  currentStock: number;
  minStockLevel: number;
  isLowStock?: boolean;
  lowStock?: boolean;
  allowNegativeStock: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Customer {
  id: string;
  name: string;
  businessName?: string;
  phone?: string;
  email?: string;
  billingAddress?: string;
  shippingAddress?: string;
  city?: string;
  stateName: string;
  stateCode: string;
  pinCode?: string;
  gstin?: string;
  customerType: 'B2B' | 'B2C';
  creditLimit: number;
  outstandingBalance: number;
  notes?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Supplier {
  id: string;
  name: string;
  businessName?: string;
  phone?: string;
  email?: string;
  address?: string;
  city?: string;
  stateName: string;
  stateCode: string;
  pinCode?: string;
  gstin?: string;
  outstandingBalance: number;
  notes?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type InventoryTransactionType =
  | 'PURCHASE'
  | 'SALE'
  | 'RETURN_IN'
  | 'RETURN_OUT'
  | 'ADJUSTMENT'
  | 'OPENING_STOCK';

export interface InventoryTransaction {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  categoryName?: string;
  unit: string;
  transactionType: InventoryTransactionType;
  quantity: number;
  previousStock: number;
  newStock: number;
  unitCost: number;
  totalCost: number;
  referenceType?: string;
  referenceId?: string;
  referenceNumber?: string;
  notes?: string;
  createdBy?: string;
  createdByName?: string;
  createdAt: string;
}

export interface InventorySummary {
  totalProductsCount: number;
  lowStockCount: number;
  outOfStockCount: number;
  totalValuation: number;
  totalUnitsInStock: number;
}

export type StockAdjustmentMode = 'INCREASE' | 'DECREASE' | 'SET_EXACT';

export interface StockAdjustmentRequest {
  productId: string;
  adjustmentMode: StockAdjustmentMode;
  quantity: number;
  reason: string;
  notes?: string;
  unitCost?: number;
}

export type PaymentStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID';
export type PaymentMethod = 'CASH' | 'UPI' | 'BANK_TRANSFER' | 'CARD' | 'CHEQUE' | 'CREDIT';

export interface PurchaseItem {
  id: string;
  productId: string;
  productName: string;
  productSku?: string;
  hsnSac?: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  taxableAmount: number;
  gstRatePercent: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
}

export interface Purchase {
  id: string;
  purchaseNumber: string;
  supplierId: string;
  supplierName: string;
  supplierGstin?: string;
  supplierInvoiceNumber?: string;
  purchaseDate: string;
  subtotal: number;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalTax: number;
  totalDiscount: number;
  grandTotal: number;
  amountPaid: number;
  balanceDue: number;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  notes?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
  items: PurchaseItem[];
}

export interface PurchaseItemRequest {
  productId: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
}

export interface CreatePurchaseRequest {
  supplierId: string;
  supplierInvoiceNumber?: string;
  purchaseDate: string;
  items: PurchaseItemRequest[];
  amountPaid?: number;
  paymentMethod?: PaymentMethod;
  notes?: string;
}

export interface PurchaseSummary {
  totalPurchasesAmount: number;
  totalOutstandingPayable: number;
  totalPurchasesCount: number;
  pendingPurchasesCount: number;
}

export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  productSku?: string;
  hsnSac?: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  taxableAmount: number;
  gstRatePercent: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
}

export interface Sale {
  id: string;
  saleNumber: string;
  customerId?: string;
  customerName: string;
  customerGstin?: string;
  placeOfSupplyState: string;
  placeOfSupplyCode: string;
  saleDate: string;
  subtotal: number;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalTax: number;
  totalDiscount: number;
  grandTotal: number;
  amountPaid: number;
  balanceDue: number;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  notes?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
  items: SaleItem[];
}

export interface SaleItemRequest {
  productId: string;
  quantity: number;
  unitPrice?: number;
  discountPercent?: number;
}

export interface CreateSaleRequest {
  customerId?: string;
  customerName?: string;
  customerGstin?: string;
  placeOfSupplyState?: string;
  placeOfSupplyCode?: string;
  saleDate: string;
  items: SaleItemRequest[];
  amountPaid?: number;
  paymentMethod?: PaymentMethod;
  notes?: string;
}

export interface SaleSummary {
  totalSalesAmount: number;
  totalOutstandingReceivable: number;
  totalSalesCount: number;
  pendingSalesCount: number;
}

export type InvoiceStatus = 'DRAFT' | 'FINALIZED' | 'CANCELLED';

export interface InvoiceItem {
  id: string;
  productId?: string;
  productName: string;
  productSku?: string;
  hsnSac?: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  taxableAmount: number;
  gstRatePercent: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalAmount: number;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  saleId?: string;
  customerId?: string;
  customerName: string;
  customerGstin?: string;
  placeOfSupplyState: string;
  placeOfSupplyCode: string;
  invoiceDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalTax: number;
  totalDiscount: number;
  grandTotal: number;
  amountPaid: number;
  balanceDue: number;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  notes?: string;
  termsAndConditions?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
  items: InvoiceItem[];
}

export interface InvoiceItemRequest {
  productId: string;
  quantity: number;
  unitPrice?: number;
  discountPercent?: number;
}

export interface CreateInvoiceRequest {
  saleId?: string;
  customerId?: string;
  customerName?: string;
  customerGstin?: string;
  placeOfSupplyState?: string;
  placeOfSupplyCode?: string;
  invoiceDate: string;
  dueDate?: string;
  items: InvoiceItemRequest[];
  amountPaid?: number;
  paymentMethod?: PaymentMethod;
  notes?: string;
  termsAndConditions?: string;
}


export interface InvoiceSummary {
  totalInvoicedAmount: number;
  totalOutstandingReceivable: number;
  totalInvoicesCount: number;
  finalizedInvoicesCount: number;
  draftInvoicesCount: number;
}

// ─── Phase 7: Expenses ────────────────────────────────────────────────────────

export interface ExpenseCategory {
  id: string;
  name: string;
  description?: string;
  active: boolean;
}

export interface Expense {
  id: string;
  categoryId?: string;
  categoryName?: string;
  expenseDate: string;
  vendorName?: string;
  description: string;
  amount: number;
  gstAmount: number;
  totalAmount: number;
  gstEligible: boolean;
  paymentMethod?: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpenseRequest {
  categoryId?: string;
  expenseDate: string;
  vendorName?: string;
  description: string;
  amount: number;
  gstAmount?: number;
  gstEligible?: boolean;
  paymentMethod?: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
}

export interface ExpenseSummary {
  totalExpensesAmount: number;
  eligibleGstInputAmount: number;
  totalExpensesCount: number;
}

// ─── Phase 8: Reports ─────────────────────────────────────────────────────────

export interface GstOutputSummary {
  totalRevenue: number;
  totalTaxableValue: number;
  totalCgstCollected: number;
  totalSgstCollected: number;
  totalIgstCollected: number;
  totalTaxCollected: number;
  b2bTaxableValue: number;
  b2cTaxableValue: number;
  totalInvoicesCount: number;
}

export interface GstInputSummary {
  totalPurchaseValue: number;
  totalTaxableValue: number;
  totalCgstPaid: number;
  totalSgstPaid: number;
  totalIgstPaid: number;
  totalInputTaxCredit: number;
  totalPurchasesCount: number;
}

export interface HsnSummaryLine {
  hsnSac: string;
  totalQuantity: number;
  totalTaxableValue: number;
  totalCgst: number;
  totalSgst: number;
  totalIgst: number;
  totalTax: number;
  totalAmount: number;
}

export interface MonthlyTrend {
  year: number;
  month: number;
  monthName: string;
  salesAmount: number;
  purchasesAmount: number;
  expensesAmount: number;
  salesCount: number;
  purchasesCount: number;
}

export interface ProfitLoss {
  totalRevenue: number;
  totalCogs: number;
  grossProfit: number;
  grossMarginPercent: number;
  totalExpenses: number;
  netProfit: number;
  totalTaxCollected: number;
  totalInputTaxCredit: number;
  netTaxLiability: number;
}

// ─── Phase 9: Audit Trail ─────────────────────────────────────────────────────

export interface AuditLogEntry {
  id: string;
  userId?: string;
  username?: string;
  action: string;
  entityType: string;
  entityId?: string;
  details?: string;
  createdAt: string;
}

// ─── Documents & Backup Types ────────────────────────────────────────────────

export interface DocumentItem {
  id: string;
  fileName: string;
  fileType: string;
  fileSizeBytes: number;
  filePath: string;
  entityType: string;
  entityId?: string;
  notes?: string;
  uploadedBy?: string;
  uploadedByName?: string;
  createdAt: string;
}

export interface BackupItem {
  fileName: string;
  fileSizeBytes: number;
  formattedSize: string;
  createdAt: string;
  checksumSha256?: string;
  status: 'VERIFIED' | 'PENDING';
}