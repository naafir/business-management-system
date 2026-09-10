import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Customer } from '../../types';
import { INDIAN_STATES, GSTIN_REGEX, getStateFromGstin } from '../../lib/constants';
import { Building2, User } from 'lucide-react';

interface CustomerModalProps {
  isOpen: boolean;
  onClose: () => void;
  customer?: Customer | null;
}

export function CustomerModal({ isOpen, onClose, customer }: CustomerModalProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [sameAsBilling, setSameAsBilling] = useState(true);

  const [formData, setFormData] = useState({
    name: '',
    businessName: '',
    phone: '',
    email: '',
    billingAddress: '',
    shippingAddress: '',
    city: '',
    stateName: 'Maharashtra',
    stateCode: '27',
    pinCode: '',
    gstin: '',
    customerType: 'B2C' as 'B2B' | 'B2C',
    creditLimit: '0.00',
    notes: '',
  });

  useEffect(() => {
    if (customer) {
      setFormData({
        name: customer.name,
        businessName: customer.businessName || '',
        phone: customer.phone || '',
        email: customer.email || '',
        billingAddress: customer.billingAddress || '',
        shippingAddress: customer.shippingAddress || '',
        city: customer.city || '',
        stateName: customer.stateName,
        stateCode: customer.stateCode,
        pinCode: customer.pinCode || '',
        gstin: customer.gstin || '',
        customerType: customer.customerType,
        creditLimit: customer.creditLimit.toString(),
        notes: customer.notes || '',
      });
      setSameAsBilling(
        !customer.shippingAddress || customer.shippingAddress === customer.billingAddress
      );
    } else {
      setFormData({
        name: '',
        businessName: '',
        phone: '',
        email: '',
        billingAddress: '',
        shippingAddress: '',
        city: '',
        stateName: 'Maharashtra',
        stateCode: '27',
        pinCode: '',
        gstin: '',
        customerType: 'B2C',
        creditLimit: '0.00',
        notes: '',
      });
      setSameAsBilling(true);
    }
    setError(null);
  }, [customer, isOpen]);

  // Handle GSTIN change with auto-detection of state
  const handleGstinChange = (val: string) => {
    const uppercaseGstin = val.toUpperCase().trim();
    setFormData((prev) => {
      const updated = { ...prev, gstin: uppercaseGstin };
      const matchedState = getStateFromGstin(uppercaseGstin);
      if (matchedState) {
        updated.stateName = matchedState.name;
        updated.stateCode = matchedState.code;
      }
      return updated;
    });
  };

  // Handle state dropdown change
  const handleStateChange = (stateName: string) => {
    const matchedState = INDIAN_STATES.find((s) => s.name === stateName);
    setFormData((prev) => ({
      ...prev,
      stateName: stateName,
      stateCode: matchedState ? matchedState.code : prev.stateCode,
    }));
  };

  const mutation = useMutation({
    mutationFn: (payload: any) => {
      if (customer) {
        return api.put<Customer>(`/customers/${customer.id}`, payload);
      } else {
        return api.post<Customer>('/customers', payload);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Failed to save customer');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Client-side validations
    if (formData.customerType === 'B2B' && !formData.gstin.trim()) {
      setError('GSTIN is required for B2B registered customers.');
      return;
    }

    if (formData.gstin.trim() && !GSTIN_REGEX.test(formData.gstin.trim())) {
      setError('Invalid GSTIN format. Example: 27AABCA1234A1Z5');
      return;
    }

    const payload = {
      name: formData.name.trim(),
      businessName: formData.businessName.trim() || undefined,
      phone: formData.phone.trim() || undefined,
      email: formData.email.trim() || undefined,
      billingAddress: formData.billingAddress.trim() || undefined,
      shippingAddress: sameAsBilling
        ? formData.billingAddress.trim() || undefined
        : formData.shippingAddress.trim() || undefined,
      city: formData.city.trim() || undefined,
      stateName: formData.stateName.trim(),
      stateCode: formData.stateCode.trim(),
      pinCode: formData.pinCode.trim() || undefined,
      gstin: formData.gstin.trim() ? formData.gstin.trim().toUpperCase() : undefined,
      customerType: formData.customerType,
      creditLimit: parseFloat(formData.creditLimit) || 0,
      notes: formData.notes.trim() || undefined,
    };

    mutation.mutate(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={customer ? 'Edit Customer' : 'Add New Customer'}
      description={
        customer
          ? `Update account information and tax profile for ${customer.name}`
          : 'Create a B2B business client or B2C consumer with GSTIN validation.'
      }
      className="max-w-2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800">
            {error}
          </div>
        )}

        {/* Customer Type Selector */}
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Customer Segmentation *
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setFormData({ ...formData, customerType: 'B2C' })}
              className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                formData.customerType === 'B2C'
                  ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'
              }`}
            >
              <User className="w-4 h-4" />
              B2C (Individual / Consumer)
            </button>
            <button
              type="button"
              onClick={() => setFormData({ ...formData, customerType: 'B2B' })}
              className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all ${
                formData.customerType === 'B2B'
                  ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'
              }`}
            >
              <Building2 className="w-4 h-4" />
              B2B (Business / GST Registered)
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="Customer / Contact Name *"
            placeholder="e.g., Rajesh Kumar"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />

          <Input
            label={formData.customerType === 'B2B' ? 'Legal Business Name *' : 'Business / Trade Name'}
            placeholder="e.g., Zenith Enterprises Pvt Ltd"
            value={formData.businessName}
            onChange={(e) => setFormData({ ...formData, businessName: e.target.value })}
            required={formData.customerType === 'B2B'}
          />

          <Input
            label={formData.customerType === 'B2B' ? 'GSTIN (15 Digits) *' : 'GSTIN (Optional)'}
            placeholder="e.g., 27AABCA1234A1Z5"
            value={formData.gstin}
            onChange={(e) => handleGstinChange(e.target.value)}
            required={formData.customerType === 'B2B'}
          />

          <Input
            label="Phone Number"
            type="tel"
            placeholder="e.g., 9876543210"
            value={formData.phone}
            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          />

          <Input
            label="Email Address"
            type="email"
            placeholder="e.g., contact@zenith.com"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          />

          <Input
            label="Credit Limit (₹)"
            type="number"
            step="1000"
            min="0"
            value={formData.creditLimit}
            onChange={(e) => setFormData({ ...formData, creditLimit: e.target.value })}
          />

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              State *
            </label>
            <select
              value={formData.stateName}
              onChange={(e) => handleStateChange(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
            >
              {INDIAN_STATES.map((s) => (
                <option key={s.code} value={s.name}>
                  {s.code} - {s.name}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="City"
              placeholder="e.g., Mumbai"
              value={formData.city}
              onChange={(e) => setFormData({ ...formData, city: e.target.value })}
            />

            <Input
              label="PIN Code"
              placeholder="e.g., 400001"
              maxLength={6}
              value={formData.pinCode}
              onChange={(e) => setFormData({ ...formData, pinCode: e.target.value })}
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Billing Address
          </label>
          <textarea
            rows={2}
            placeholder="Premises, Street, Landmark..."
            value={formData.billingAddress}
            onChange={(e) => setFormData({ ...formData, billingAddress: e.target.value })}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all resize-none"
          />
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="sameAsBilling"
              checked={sameAsBilling}
              onChange={(e) => setSameAsBilling(e.target.checked)}
              className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 border-slate-300 dark:border-slate-700"
            />
            <label htmlFor="sameAsBilling" className="text-xs text-slate-700 dark:text-slate-300 cursor-pointer">
              Shipping address is the same as billing address
            </label>
          </div>

          {!sameAsBilling && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Shipping Address
              </label>
              <textarea
                rows={2}
                placeholder="Warehouse or delivery destination..."
                value={formData.shippingAddress}
                onChange={(e) => setFormData({ ...formData, shippingAddress: e.target.value })}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all resize-none"
              />
            </div>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Internal Notes
          </label>
          <input
            type="text"
            placeholder="Special commercial terms, contact persons, or delivery instructions..."
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
          />
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" isLoading={mutation.isPending}>
            {customer ? 'Save Changes' : 'Create Customer'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
