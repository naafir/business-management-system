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