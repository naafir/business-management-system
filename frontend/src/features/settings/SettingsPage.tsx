import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { BusinessSettings } from '../../types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { UserManagementTab } from './UserManagementTab';
import { Building2, Landmark, FileText, CheckCircle2, AlertCircle, Users } from 'lucide-react';

export function SettingsPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'profile' | 'bank' | 'invoice' | 'users'>('profile');
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: settings, isLoading } = useQuery<BusinessSettings>({
    queryKey: ['settings'],
    queryFn: () => api.get<BusinessSettings>('/settings/profile'),
  });

  const [formData, setFormData] = useState<Partial<BusinessSettings>>({});

  useEffect(() => {
    if (settings) {
      setFormData(settings);
    }
  }, [settings]);

  const mutation = useMutation({
    mutationFn: (updated: Partial<BusinessSettings>) =>
      api.put<BusinessSettings>('/settings/profile', updated),
    onSuccess: (data) => {
      queryClient.setQueryData(['settings'], data);
      setSuccessMessage('Business settings saved successfully!');
      setErrorMessage(null);
      setTimeout(() => setSuccessMessage(null), 4000);
    },
    onError: (err: any) => {
      setErrorMessage(err.message || 'Failed to update settings');
      setSuccessMessage(null);
    },
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          Business Settings
        </h1>
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
          Manage your organization profile, GSTIN, bank details, and invoice preferences.
        </p>
      </div>

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
          <p className="text-xs font-semibold text-emerald-800 dark:text-emerald-200">{successMessage}</p>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-rose-600 flex-shrink-0" />
          <p className="text-xs font-semibold text-rose-800 dark:text-rose-200">{errorMessage}</p>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-800 space-x-4">
        <button
          onClick={() => setActiveTab('profile')}
          className={`pb-3 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'profile'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <Building2 className="w-4 h-4" />
          Profile & GST Details
        </button>

        <button
          onClick={() => setActiveTab('bank')}
          className={`pb-3 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'bank'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <Landmark className="w-4 h-4" />
          Bank & UPI Details
        </button>

        <button
          onClick={() => setActiveTab('invoice')}
          className={`pb-3 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'invoice'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
        <FileText className="w-4 h-4" />
          Invoice & Tax Defaults
        </button>

        <button
          onClick={() => setActiveTab('users')}
          className={`pb-3 text-xs font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'users'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <Users className="w-4 h-4" />
          User Management
        </button>
      </div>

      {activeTab === 'users' ? (
        <UserManagementTab />
      ) : (
      <form onSubmit={handleSubmit}>
        {activeTab === 'profile' && (
          <Card>
            <CardHeader>
              <CardTitle>Business Entity & Location</CardTitle>
              <CardDescription>
                Legal details used for GST place-of-supply determination and invoice headers.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Legal Business Name *"
                  name="legalName"
                  required
                  value={formData.legalName || ''}
                  onChange={handleChange}
                  placeholder="e.g. Apex Tech Solutions Pvt Ltd"
                />
                <Input
                  label="Trade / Display Name"
                  name="tradeName"
                  value={formData.tradeName || ''}
                  onChange={handleChange}
                  placeholder="e.g. Apex Tech"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="GSTIN (15 Alphanumeric Characters)"
                  name="gstin"
                  value={formData.gstin || ''}
                  onChange={handleChange}
                  placeholder="e.g. 27AAAAA0000A1Z5"
                />
                <Input
                  label="Permanent Account Number (PAN)"
                  name="pan"
                  value={formData.pan || ''}
                  onChange={handleChange}
                  placeholder="e.g. AAAAA0000A"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Address Line 1"
                  name="addressLine1"
                  value={formData.addressLine1 || ''}
                  onChange={handleChange}
                  placeholder="Street / Building"
                />
                <Input
                  label="Address Line 2"
                  name="addressLine2"
                  value={formData.addressLine2 || ''}
                  onChange={handleChange}
                  placeholder="Area / Landmark"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Input
                  label="City"
                  name="city"
                  value={formData.city || ''}
                  onChange={handleChange}
                  placeholder="e.g. Mumbai"
                />
                <Input
                  label="State Name *"
                  name="stateName"
                  required
                  value={formData.stateName || ''}
                  onChange={handleChange}
                  placeholder="e.g. Maharashtra"
                />
                <Input
                  label="State Code (2 Digits) *"
                  name="stateCode"
                  required
                  maxLength={2}
                  value={formData.stateCode || ''}
                  onChange={handleChange}
                  placeholder="e.g. 27"
                  helperText="Crucial for Intra vs Inter state GST calculation"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Input
                  label="PIN Code"
                  name="pinCode"
                  value={formData.pinCode || ''}
                  onChange={handleChange}
                  placeholder="e.g. 400001"
                />
                <Input
                  label="Phone Number"
                  name="phone"
                  value={formData.phone || ''}
                  onChange={handleChange}
                  placeholder="+91 98765 43210"
                />
                <Input
                  label="Official Email"
                  name="email"
                  type="email"
                  value={formData.email || ''}
                  onChange={handleChange}
                  placeholder="billing@apextech.com"
                />
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'bank' && (
          <Card>
            <CardHeader>
              <CardTitle>Bank Account & UPI Details</CardTitle>
              <CardDescription>
                Details printed on customer invoices to facilitate direct bank transfers and UPI payments.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Bank Name"
                  name="bankName"
                  value={formData.bankName || ''}
                  onChange={handleChange}
                  placeholder="e.g. HDFC Bank"
                />
                <Input
                  label="Account Number"
                  name="bankAccountNumber"
                  value={formData.bankAccountNumber || ''}
                  onChange={handleChange}
                  placeholder="e.g. 50200012345678"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Input
                  label="IFSC Code"
                  name="bankIfsc"
                  value={formData.bankIfsc || ''}
                  onChange={handleChange}
                  placeholder="e.g. HDFC0001234"
                />
                <Input
                  label="Branch"
                  name="bankBranch"
                  value={formData.bankBranch || ''}
                  onChange={handleChange}
                  placeholder="e.g. Nariman Point"
                />
                <Input
                  label="UPI ID (VPA)"
                  name="bankUpiId"
                  value={formData.bankUpiId || ''}
                  onChange={handleChange}
                  placeholder="e.g. business@okaxis"
                />
              </div>
            </CardContent>
          </Card>
        )}

        {activeTab === 'invoice' && (
          <Card>
            <CardHeader>
              <CardTitle>Invoicing & Numbering Rules</CardTitle>
              <CardDescription>
                Configure continuous sequential invoice numbering and standard invoice terms.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Invoice Number Prefix *"
                  name="invoicePrefix"
                  required
                  value={formData.invoicePrefix || ''}
                  onChange={handleChange}
                  placeholder="e.g. INV-2026-"
                  helperText="Example output: INV-2026-000001"
                />
                <Input
                  label="Default Currency (ISO-3) *"
                  name="defaultCurrency"
                  required
                  maxLength={3}
                  value={formData.defaultCurrency || ''}
                  onChange={handleChange}
                  placeholder="INR"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Invoice Terms and Conditions
                </label>
                <textarea
                  name="invoiceTerms"
                  rows={4}
                  value={formData.invoiceTerms || ''}
                  onChange={handleChange}
                  placeholder="1. Goods once sold will not be taken back.&#10;2. Interest @ 18% p.a. will be charged for delayed payments."
                  className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
            </CardContent>
          </Card>
        )}

        <div className="mt-6 flex justify-end">
          <Button type="submit" size="md" isLoading={mutation.isPending}>
            Save All Settings
          </Button>
        </div>
      </form>
      )}
    </div>
  );
}
