# Business Management & GST Billing System

A professional, production-grade personal business management, inventory ledger, and GST billing web application. Designed around strict data preservation, financial record accuracy, immutable audit trails, and clean architectural boundaries.

---

## 1. Project Overview & Features

- **Authentication & Authorization**: Role-based access control (OWNER, ADMIN), BCrypt password hashing, secure JWT-based stateless authentication, protected routes.
- **Business Profile Settings**: Comprehensive configuration for legal entity name, trade name, GSTIN, PAN, state code, bank details, invoice prefix, terms & conditions, and default tax rates.
- **Data Safety & Ledger Architecture**: No hard deletion of financial records. Full audit logging for every administrative and financial action.
- **Strict Money & Tax Calculation**: 100% BigDecimal precision. Deterministic intra-state (CGST + SGST) vs. inter-state (IGST) split based on business location vs. place of supply.
- **Inventory Ledger**: Real-time stock tracking with transaction types (OPENING_STOCK, PURCHASE, SALE, ADJUSTMENT, RETURN).
- **Invoices & Server-side PDF**: Unique invoice numbering, immutable finalized invoices, credit/debit notes for adjustments.
- **Modern UI**: React 18, TypeScript, Tailwind CSS, Lucide icons, Dark/Light mode theme persistence, and responsive desktop-first layout.

---

## 2. Technology Stack

### Backend
- Framework: Spring Boot 3.2.x (Java 17)
- Security: Spring Security + JJWT + BCrypt
- Persistence: Spring Data JPA + Hibernate
- Database Migrations: Flyway
- Database: PostgreSQL 16 (with H2 embedded PostgreSQL-compatibility mode for dev/test)
- Build Tool: Maven / Maven Wrapper (mvnw)
- Testing: JUnit 5, Mockito, AssertJ, Spring Boot Test

### Frontend
- Framework: React 18 with TypeScript & Vite
- Styling: Tailwind CSS (with full Dark Mode support)
- Routing: React Router DOM v6
- Server State: TanStack Query (React Query v5)
- Forms & Validation: React Hook Form + Zod
- Charts: Recharts
- Icons: Lucide React

---

## 3. Directory Structure

```
business-management-system/
├── backend/            # Spring Boot REST API & Business Logic
├── frontend/           # React + TypeScript + Vite Single Page Application
├── database/           # Standalone SQL schema & seed files
├── docs/               # Architecture, Database, GST, API, and Backup documentation
├── scripts/            # Database backup and restore automated scripts
├── backups/            # Local backup storage (git-ignored)
├── docker-compose.yml  # Multi-container orchestration (Postgres, Backend, Frontend)
├── .env.example        # Environment variable template
└── README.md
```

---

## 4. Quick Start & Installation

### Prerequisites
- Java 17+
- Node.js 18+ and npm 9+
- Docker & Docker Compose (optional for local database container)

### 1. Database Setup (Docker or Local PostgreSQL)
Using Docker Compose:
```bash
docker compose up -d postgres
```
Or start your local PostgreSQL instance and create a database named business_billing_db.

### 2. Running the Backend
Navigate to the backend/ directory:
```bash
cd backend
# Windows:
.\mvnw.cmd spring-boot:run
# macOS / Linux:
./mvnw spring-boot:run
```
The backend starts on http://localhost:8080.
Flyway automatically applies all versioned migrations on startup.
Default initial credentials:
- Username: admin
- Password: Admin@SecurePass2026!

### 3. Running the Frontend
Navigate to the frontend/ directory:
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173 in your browser.

---

## 5. Running Tests

### Backend Tests:
```bash
cd backend
.\mvnw.cmd test
```

### Frontend Typecheck & Build:
```bash
cd frontend
npm run build
```

---

## 6. Backup and Restore Strategy

Detailed instructions are available in docs/backup-and-restore.md.
Automated scripts are provided in scripts/:
- Backup: Run scripts/backup.ps1 (or scripts/backup.sh) to produce a timestamped, gzip-compressed PostgreSQL dump with a SHA-256 verification checksum.
- Restore: Run scripts/restore.ps1 -BackupFile <file> to verify and restore database state.

---

## 7. Important GST Notice

This application is built as GST-ready accounting and billing software. However, tax laws, GST rate schedules, and notification rules change periodically. For legal compliance and formal GSTR filing, invoice data should always be reconciled by a certified Chartered Accountant or tax professional.
