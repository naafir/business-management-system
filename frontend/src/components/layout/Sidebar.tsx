import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ShoppingCart,
  ShoppingBag,
  FileText,
  Package,
  Boxes,
  Users,
  Truck,
  CreditCard,
  Receipt,
  FileSpreadsheet,
  Percent,
  FolderOpen,
  History,
  HardDriveDownload,
  Settings,
  X,
  Building2,
  ChevronRight,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { useAuth } from '../../features/auth/useAuth';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const navGroups = [
  {
    label: 'Overview',
    items: [
      { name: 'Dashboard', path: '/', icon: LayoutDashboard },
    ],
  },
  {
    label: 'Operations',
    items: [
      { name: 'Sales', path: '/sales', icon: ShoppingCart },
      { name: 'Purchases', path: '/purchases', icon: ShoppingBag },
      { name: 'Invoices', path: '/invoices', icon: FileText },
      { name: 'Payments', path: '/payments', icon: CreditCard },
      { name: 'Expenses', path: '/expenses', icon: Receipt },
    ],
  },
  {
    label: 'Inventory & Catalog',
    items: [
      { name: 'Products', path: '/products', icon: Package },
      { name: 'Inventory', path: '/inventory', icon: Boxes },
    ],
  },
  {
    label: 'Parties',
    items: [
      { name: 'Customers', path: '/customers', icon: Users },
      { name: 'Suppliers', path: '/suppliers', icon: Truck },
    ],
  },
  {
    label: 'Finance & Tax',
    items: [
      { name: 'GST Center', path: '/gst', icon: Percent },
      { name: 'Reports', path: '/reports', icon: FileSpreadsheet },
    ],
  },
  {
    label: 'Administration',
    items: [
      { name: 'Audit Logs', path: '/audit', icon: History },
      { name: 'Documents', path: '/documents', icon: FolderOpen },
      { name: 'Backups', path: '/backups', icon: HardDriveDownload },
      { name: 'Settings', path: '/settings', icon: Settings },
    ],
  },
];

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { user } = useAuth();

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 z-40 bg-slate-900/60 backdrop-blur-sm lg:hidden"
        />
      )}

      <aside
        className={cn(
          'fixed top-0 bottom-0 left-0 z-50 w-64 flex flex-col transition-transform duration-200 ease-in-out',
          'bg-white dark:bg-slate-950 border-r border-slate-200 dark:border-slate-800/80',
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
      >
        {/* Logo Header */}
        <div className="flex items-center justify-between h-16 px-5 border-b border-slate-100 dark:border-slate-800/80 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 flex items-center justify-center shadow-lg shadow-indigo-600/30">
              <Building2 className="w-4.5 h-4.5 text-white" />
            </div>
            <div>
              <h1 className="text-sm font-bold tracking-tight text-slate-900 dark:text-white leading-tight">
                ApexBilling
              </h1>
              <p className="text-[10px] text-slate-400 font-medium leading-tight">GST & Enterprise Suite</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 lg:hidden transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-5">
          {navGroups.map(group => (
            <div key={group.label}>
              <p className="text-[10px] font-bold text-slate-400 dark:text-slate-600 uppercase tracking-widest px-3 mb-1.5">
                {group.label}
              </p>
              <div className="space-y-0.5">
                {group.items.map(item => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.name}
                      to={item.path}
                      end={item.path === '/'}
                      onClick={() => onClose()}
                      className={({ isActive }) =>
                        cn(
                          'group flex items-center justify-between gap-2.5 px-3 py-2 text-xs font-medium rounded-lg transition-all duration-150',
                          isActive
                            ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 font-semibold shadow-sm'
                            : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/70 hover:text-slate-900 dark:hover:text-slate-100'
                        )
                      }
                    >
                      {({ isActive }) => (
                        <>
                          <span className="flex items-center gap-2.5">
                            <Icon className={cn('w-4 h-4 flex-shrink-0 transition-colors', isActive ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 group-hover:text-slate-600 dark:group-hover:text-slate-300')} />
                            {item.name}
                          </span>
                          {isActive && <ChevronRight className="w-3 h-3 text-indigo-400 dark:text-indigo-500 shrink-0" />}
                        </>
                      )}
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* User footer */}
        <div className="p-3 border-t border-slate-100 dark:border-slate-800/80 shrink-0">
          <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200/60 dark:border-slate-800">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
              {user?.fullName?.charAt(0).toUpperCase() || 'A'}
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                {user?.fullName || 'Business Owner'}
              </p>
              <p className="text-[10px] text-slate-400 truncate">
                {user?.role?.replace('ROLE_', '') || 'OWNER'}
              </p>
            </div>
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0" title="Online" />
          </div>
        </div>
      </aside>
    </>
  );
}
