import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { SettingsPage } from './features/settings/SettingsPage';
import { PlaceholderPage } from './components/PlaceholderPage';

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
      <Route element={<AppLayout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        
        {/* Upcoming Module Placeholders */}
        <Route path="/sales" element={<PlaceholderPage title="Sales Management" description="Record customer sales transactions and link them to GST invoices." phase="Phase 4" />} />
        <Route path="/purchases" element={<PlaceholderPage title="Purchase Management" description="Record vendor purchases, supplier bills, and input tax credits." phase="Phase 4" />} />
        <Route path="/invoices" element={<PlaceholderPage title="Invoice & PDF Generator" description="Generate immutable GST-compliant tax invoices and server-side PDFs." phase="Phase 6" />} />
        <Route path="/products" element={<PlaceholderPage title="Product Catalog" description="Manage items, SKU codes, HSN/SAC codes, and GST rates." phase="Phase 2" />} />
        <Route path="/inventory" element={<PlaceholderPage title="Inventory Ledger" description="Continuous stock ledger tracking purchases, sales, and adjustments." phase="Phase 3" />} />
        <Route path="/customers" element={<PlaceholderPage title="Customer Management" description="B2B and B2C customer master with GSTIN verification." phase="Phase 2" />} />
        <Route path="/suppliers" element={<PlaceholderPage title="Supplier Management" description="Vendor directories, payable tracking, and purchase history." phase="Phase 2" />} />
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
