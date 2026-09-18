import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../services/api';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { toast } from '../../hooks/useToast';
import { formatDate } from '../../lib/utils';
import {
  Users, Plus, Shield, ToggleLeft, ToggleRight, KeyRound, Eye, EyeOff
} from 'lucide-react';

interface UserDetail {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

interface CreateUserForm {
  username: string;
  email: string;
  fullName: string;
  password: string;
  role: string;
}

const ROLES = [
  { value: 'ROLE_OWNER', label: 'Owner', desc: 'Full access — all modules + user management' },
  { value: 'ROLE_ADMIN', label: 'Admin', desc: 'Full access except user management' },
  { value: 'ROLE_MANAGER', label: 'Manager', desc: 'Sales, purchases, inventory, reports' },
  { value: 'ROLE_STAFF', label: 'Staff', desc: 'Sales entry and basic inventory only' },
];

const roleBadge = (role: string) => {
  if (role === 'ROLE_OWNER') return <Badge variant="danger">Owner</Badge>;
  if (role === 'ROLE_ADMIN') return <Badge variant="warning">Admin</Badge>;
  if (role === 'ROLE_MANAGER') return <Badge variant="info">Manager</Badge>;
  return <Badge variant="default">Staff</Badge>;
};

function AddUserModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CreateUserForm>({
    username: '', email: '', fullName: '', password: '', role: 'ROLE_STAFF'
  });
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (data: CreateUserForm) => api.post<UserDetail>('/users', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('User created successfully!');
      onClose();
    },
    onError: (err: any) => setError(err.message || 'Failed to create user'),
  });

  const f = (field: keyof CreateUserForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm(prev => ({ ...prev, [field]: e.target.value }));

  return (
    <Modal isOpen onClose={onClose} title="Add New User" description="Create a staff account with specific role permissions" className="max-w-md">
      <div className="space-y-4">
        {error && (
          <div className="p-3 text-xs rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800">
            {error}
          </div>
        )}

        <Input label="Full Name *" value={form.fullName} onChange={f('fullName')} placeholder="e.g. Rahul Sharma" required />
        <Input label="Username *" value={form.username} onChange={f('username')} placeholder="e.g. rahul.sharma" required />
        <Input label="Email *" type="email" value={form.email} onChange={f('email')} placeholder="rahul@company.com" required />

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">Role *</label>
          <select
            value={form.role}
            onChange={f('role')}
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-all"
          >
            {ROLES.map(r => <option key={r.value} value={r.value}>{r.label} — {r.desc}</option>)}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">Password *</label>
          <div className="relative">
            <input
              type={showPass ? 'text' : 'password'}
              value={form.password}
              onChange={f('password')}
              placeholder="Min 8 characters"
              className="w-full px-3.5 py-2.5 pr-10 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-xs transition-all"
            />
            <button type="button" onClick={() => setShowPass(!showPass)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
              {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        <div className="flex gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
          <Button variant="outline" className="flex-1" onClick={onClose}>Cancel</Button>
          <Button
            className="flex-1"
            isLoading={mutation.isPending}
            disabled={!form.fullName || !form.username || !form.email || form.password.length < 8}
            onClick={() => { setError(null); mutation.mutate(form); }}
          >
            Create User
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function ChangePasswordModal({ onClose }: { onClose: () => void }) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => api.post('/auth/change-password', { currentPassword, newPassword, confirmPassword }),
    onSuccess: () => {
      toast.success('Password updated successfully!');
      onClose();
    },
    onError: (err: any) => setError(err.message || 'Failed to update password'),
  });

  return (
    <Modal isOpen onClose={onClose} title="Change My Password" description="Update your account login password" className="max-w-md">
      <div className="space-y-4">
        {error && (
          <div className="p-3 text-xs rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800">
            {error}
          </div>
        )}
        <Input label="Current Password *" type="password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required />
        <Input label="New Password *" type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} placeholder="Min 8 characters" required />
        <Input label="Confirm New Password *" type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="Re-enter new password" required />
        <div className="flex gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
          <Button variant="outline" className="flex-1" onClick={onClose}>Cancel</Button>
          <Button
            className="flex-1"
            isLoading={mutation.isPending}
            disabled={!currentPassword || newPassword.length < 8 || newPassword !== confirmPassword}
            onClick={() => { setError(null); mutation.mutate(); }}
          >
            Update Password
          </Button>
        </div>
      </div>
    </Modal>
  );
}

export function UserManagementTab() {
  const queryClient = useQueryClient();
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isChangePassOpen, setIsChangePassOpen] = useState(false);
  const [confirmToggle, setConfirmToggle] = useState<UserDetail | null>(null);

  const { data: users = [], isLoading } = useQuery<UserDetail[]>({
    queryKey: ['users'],
    queryFn: () => api.get<UserDetail[]>('/users'),
  });

  const toggleMutation = useMutation({
    mutationFn: (user: UserDetail) =>
      api.patch<UserDetail>(`/users/${user.id}/status?active=${!user.active}`, {}),
    onSuccess: (_, user) => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success(`User ${user.active ? 'disabled' : 'enabled'} successfully.`);
      setConfirmToggle(null);
    },
    onError: (err: any) => toast.error(err.message || 'Failed to update user status'),
  });

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">User Accounts</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">Manage staff access and roles</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => setIsChangePassOpen(true)} className="flex items-center gap-1.5 text-xs" size="sm">
            <KeyRound className="w-3.5 h-3.5" /> Change Password
          </Button>
          <Button onClick={() => setIsAddOpen(true)} className="flex items-center gap-1.5 text-xs" size="sm">
            <Plus className="w-3.5 h-3.5" /> Add User
          </Button>
        </div>
      </div>

      {/* Role Legend */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
        {ROLES.map(r => (
          <div key={r.value} className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
            <div className="flex items-center gap-1.5 mb-1">
              <Shield className="w-3 h-3 text-indigo-500" />
              <span className="text-xs font-bold text-slate-800 dark:text-slate-200">{r.label}</span>
            </div>
            <p className="text-[10px] text-slate-500 dark:text-slate-400 leading-relaxed">{r.desc}</p>
          </div>
        ))}
      </div>

      {/* Users Table */}
      <div className="rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center">
            <div className="animate-spin w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full mx-auto mb-2" />
            <p className="text-xs text-slate-500">Loading users...</p>
          </div>
        ) : (
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 font-semibold border-b border-slate-200 dark:border-slate-800 uppercase tracking-wider">
              <tr>
                <th className="py-3 px-4">User</th>
                <th className="py-3 px-4">Username</th>
                <th className="py-3 px-4">Role</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Joined</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {users.map(u => (
                <tr key={u.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
                        {u.fullName.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-white">{u.fullName}</p>
                        <p className="text-[10px] text-slate-500">{u.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-4 font-mono text-slate-600 dark:text-slate-400">{u.username}</td>
                  <td className="py-3 px-4">{roleBadge(u.role)}</td>
                  <td className="py-3 px-4">
                    {u.active
                      ? <span className="inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400 font-medium"><span className="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block" /> Active</span>
                      : <span className="inline-flex items-center gap-1 text-xs text-slate-400 font-medium"><span className="w-1.5 h-1.5 rounded-full bg-slate-400 inline-block" /> Disabled</span>
                    }
                  </td>
                  <td className="py-3 px-4 text-slate-500 dark:text-slate-400">{formatDate(u.createdAt)}</td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => setConfirmToggle(u)}
                        title={u.active ? 'Disable user' : 'Enable user'}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 transition-colors"
                      >
                        {u.active
                          ? <ToggleRight className="w-4 h-4 text-emerald-500" />
                          : <ToggleLeft className="w-4 h-4 text-slate-400" />}
                      </button>
                      <button
                        title="Reset password"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/40 transition-colors"
                      >
                        <KeyRound className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Add User Modal */}
      {isAddOpen && <AddUserModal onClose={() => setIsAddOpen(false)} />}

      {/* Change Password Modal */}
      {isChangePassOpen && <ChangePasswordModal onClose={() => setIsChangePassOpen(false)} />}

      {/* Confirm Toggle Dialog */}
      <ConfirmDialog
        isOpen={!!confirmToggle}
        onClose={() => setConfirmToggle(null)}
        onConfirm={() => confirmToggle && toggleMutation.mutate(confirmToggle)}
        title={confirmToggle?.active ? 'Disable User Account' : 'Enable User Account'}
        message={`Are you sure you want to ${confirmToggle?.active ? 'disable' : 'enable'} the account for ${confirmToggle?.fullName}? ${confirmToggle?.active ? 'They will immediately lose access to the system.' : 'They will regain access with their existing credentials.'}`}
        confirmLabel={confirmToggle?.active ? 'Disable Account' : 'Enable Account'}
        variant={confirmToggle?.active ? 'danger' : 'primary'}
        isLoading={toggleMutation.isPending}
      />
    </div>
  );
}
