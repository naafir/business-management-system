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
  X
} from 'lucide-react';
import { cn } from '../../lib/utils';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const navItems = [
  { name: 'Dashboard', path: '/', icon: LayoutDashboard },
  { name: 'Sales', path: '/sales', icon: ShoppingCart },
  { name: 'Purchases', path: '/purchases', icon: ShoppingBag },
  { name: 'Invoices', path: '/invoices', icon: FileText },
  { name: 'Products', path: '/products', icon: Package },
  { name: 'Inventory', path: '/inventory', icon: Boxes },
  { name: 'Customers', path: '/customers', icon: Users },
  { name: 'Suppliers', path: '/suppliers', icon: Truck },
  { name: 'Payments', path: '/payments', icon: CreditCard },
  { name: 'Expenses', path: '/expenses', icon: Receipt },
  { name: 'GST Center', path: '/gst', icon: Percent },
  { name: 'Reports', path: '/reports', icon: FileSpreadsheet },
  { name: 'Documents', path: '/documents', icon: FolderOpen },
  { name: 'Audit Logs', path: '/audit', icon: History },
  { name: 'Backups', path: '/backups', icon: HardDriveDownload },
  { name: 'Settings', path: '/settings', icon: Settings },
];

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-sm lg:hidden"
        />
      )}

      <aside
        className={cn(
          'fixed top-0 bottom-0 left-0 z-50 w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 transition-transform duration-200 ease-in-out flex flex-col',
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
      >
        <div className="flex items-center justify-between h-16 px-6 border-b border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold text-sm shadow-md shadow-indigo-600/30">
              AG
            </div>
            <div>
              <h1 className="text-sm font-bold tracking-tight text-slate-900 dark:text-white">
                ApexBilling
              </h1>
              <p className="text-[10px] text-slate-500 font-medium">GST & Enterprise Suite</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 lg:hidden"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(item => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.path}
                onClick={() => onClose()}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2 text-xs font-medium rounded-lg transition-colors',
                    isActive
                      ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/50 dark:text-indigo-300 font-semibold'
                      : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-slate-100'
                  )
                }
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-100 dark:border-slate-800">
          <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl p-3 text-xs border border-slate-200/60 dark:border-slate-700/60">
            <p className="font-semibold text-slate-800 dark:text-slate-200">GST System Active</p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">Place of Supply Rules: Ready</p>
          </div>
        </div>
      </aside>
    </>
  );
}
