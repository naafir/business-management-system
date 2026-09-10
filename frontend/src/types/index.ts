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
