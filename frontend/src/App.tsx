import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { SettingsPage } from './features/settings/SettingsPage';
import { ProductsPage } from './features/products/ProductsPage';
import { CustomersPage } from './features/customers/CustomersPage';
import { SuppliersPage } from './features/suppliers/SuppliersPage';
import { InventoryPage } from './features/inventory/InventoryPage';
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
        
        {/* Upcoming Module Placeholders */}
        <Route path="/sales" element={<PlaceholderPage title="Sales Management" description="Record customer sales transactions and link them to GST invoices." phase="Phase 4" />} />
        <Route path="/purchases" element={<PlaceholderPage title="Purchase Management" description="Record vendor purchases, supplier bills, and input tax credits." phase="Phase 4" />} />
        <Route path="/invoices" element={<PlaceholderPage title="Invoice & PDF Generator" description="Generate immutable GST-compliant tax invoices and server-side PDFs." phase="Phase 6" />} />
        <Route path="/payments" element={<PlaceholderPage title="Payment Records" description="Customer receipts, vendor payouts, UPI, and bank transfers." phase="Phase 7" />} />
        <Route path="/expenses" element={<PlaceholderPage title="Expense Tracking" description="Categorized operational expenses and GST input eligibility." phase="Phase 7" />} />
        <Route path="/gst" element={<PlaceholderPage title="GST Reporting Hub" description="GSTR-1 and GSTR-3B tax summaries and HSN breakups." phase="Phase 8" />} />
        <Route path="/reports" element={<PlaceholderPage title="Business Reports" description="Sales trends, profit & loss estimates, and inventory valuation." phase="Phase 8" />} />
        <Route path="/documents" element={<PlaceholderPage title="Document Storage" description="Secure attachment storage for receipts and vendor bills." phase="Phase 9" />} />
        <Route path="/audit" element={<PlaceholderPage title="Audit Trail" description="Immutable record of every user action and administrative modification." phase="Phase 9" />} />
        <Route path="/backups" element={<PlaceholderPage title="Backup & Restore" description="Encrypted database backups and automated integrity verification." phase="Phase 9" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
export default App;
