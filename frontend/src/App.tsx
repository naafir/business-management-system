import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { SettingsPage } from './features/settings/SettingsPage';
import { ProductsPage } from './features/products/ProductsPage';
import { CustomersPage } from './features/customers/CustomersPage';
import { SuppliersPage } from './features/suppliers/SuppliersPage';
import { InventoryPage } from './features/inventory/InventoryPage';
import { PurchasesPage } from './features/purchases/PurchasesPage';
import { SalesPage } from './features/sales/SalesPage';
import { InvoicesPage } from './features/invoices/InvoicesPage';
import { ExpensesPage } from './features/expenses/ExpensesPage';
import { GstReportPage } from './features/gst/GstReportPage';
import { ReportsPage } from './features/reports/ReportsPage';
import { AuditPage } from './features/audit/AuditPage';
import { PlaceholderPage } from './components/PlaceholderPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<AppLayout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/suppliers" element={<SuppliersPage />} />
        <Route path="/purchases" element={<PurchasesPage />} />
        <Route path="/sales" element={<SalesPage />} />
        <Route path="/invoices" element={<InvoicesPage />} />
        {/* Phase 7 */}
        <Route path="/expenses" element={<ExpensesPage />} />
        <Route path="/payments" element={<PlaceholderPage title="Payment Records" description="Customer receipts, vendor payouts, UPI, and bank transfers." phase="Phase 7" />} />
        {/* Phase 8 */}
        <Route path="/gst" element={<GstReportPage />} />
        <Route path="/reports" element={<ReportsPage />} />
        {/* Phase 9 */}
        <Route path="/audit" element={<AuditPage />} />
        <Route path="/documents" element={<PlaceholderPage title="Document Storage" description="Secure attachment storage for receipts and vendor bills." phase="Phase 9" />} />
        <Route path="/backups" element={<PlaceholderPage title="Backup & Restore" description="Encrypted database backups and automated integrity verification." phase="Phase 9" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
