import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { Supplier } from '../../types';
import { INDIAN_STATES, GSTIN_REGEX, getStateFromGstin } from '../../lib/constants';

interface SupplierModalProps {
  isOpen: boolean;
  onClose: () => void;
  supplier?: Supplier | null;
}

export function SupplierModal({ isOpen, onClose, supplier }: SupplierModalProps) {
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    name: '',
    businessName: '',
    phone: '',
    email: '',
    address: '',
    city: '',
    stateName: 'Maharashtra',
    stateCode: '27',
    pinCode: '',
    gstin: '',
    notes: '',
  });

  useEffect(() => {
    if (supplier) {
      setFormData({
        name: supplier.name,
        businessName: supplier.businessName || '',
        phone: supplier.phone || '',
        email: supplier.email || '',
        address: supplier.address || '',
        city: supplier.city || '',
        stateName: supplier.stateName,
        stateCode: supplier.stateCode,
        pinCode: supplier.pinCode || '',
        gstin: supplier.gstin || '',
        notes: supplier.notes || '',
      });
    } else {
      setFormData({
        name: '',
        businessName: '',
        phone: '',
        email: '',
        address: '',
        city: '',
        stateName: 'Maharashtra',
        stateCode: '27',
        pinCode: '',
        gstin: '',
        notes: '',
      });
    }
    setError(null);
  }, [supplier, isOpen]);

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
      if (supplier) {
        return api.put<Supplier>(`/suppliers/${supplier.id}`, payload);
      } else {
        return api.post<Supplier>('/suppliers', payload);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      onClose();
    },
    onError: (err: any) => {
      setError(err.message || 'Failed to save supplier');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (formData.gstin.trim() && !GSTIN_REGEX.test(formData.gstin.trim())) {
      setError('Invalid GSTIN format. Example: 27AABCA1234A1Z5');
      return;
    }

    const payload = {
      name: formData.name.trim(),
      businessName: formData.businessName.trim() || undefined,
      phone: formData.phone.trim() || undefined,
      email: formData.email.trim() || undefined,
      address: formData.address.trim() || undefined,
      city: formData.city.trim() || undefined,
      stateName: formData.stateName.trim(),
      stateCode: formData.stateCode.trim(),
      pinCode: formData.pinCode.trim() || undefined,
      gstin: formData.gstin.trim() ? formData.gstin.trim().toUpperCase() : undefined,
      notes: formData.notes.trim() || undefined,
    };

    mutation.mutate(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={supplier ? 'Edit Supplier' : 'Add New Supplier'}
      description={
        supplier
          ? `Update vendor contact details and tax info for ${supplier.name}`
          : 'Register a new vendor or wholesale supplier for purchase orders and expense tracking.'
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
            label="Supplier / Contact Name *"
            placeholder="e.g., Rajesh Mehta"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />

          <Input
            label="Company / Business Name"
            placeholder="e.g., Mehta Trading Co"
            value={formData.businessName}
            onChange={(e) => setFormData({ ...formData, businessName: e.target.value })}
          />

          <Input
            label="GSTIN (15 Digits)"
            placeholder="e.g., 27AABCA1234A1Z5"
            value={formData.gstin}
            onChange={(e) => handleGstinChange(e.target.value)}
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
            placeholder="e.g., supplier@mehtatrading.com"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
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

          <Input
            label="City"
            placeholder="e.g., Surat"
            value={formData.city}
            onChange={(e) => setFormData({ ...formData, city: e.target.value })}
          />

          <Input
            label="PIN Code"
            placeholder="e.g., 395003"
            maxLength={6}
            value={formData.pinCode}
            onChange={(e) => setFormData({ ...formData, pinCode: e.target.value })}
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Office / Warehouse Address
          </label>
          <textarea
            rows={2}
            placeholder="Vendor location or depot address..."
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all resize-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            Internal Notes
          </label>
          <input
            type="text"
            placeholder="Payment terms, bank details, credit period..."
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
            {supplier ? 'Save Changes' : 'Create Supplier'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
