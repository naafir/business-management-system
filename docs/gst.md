# GST Calculation Engine Specification

## 1. Place of Supply Rules
- **Intra-State**: `Business State Code == Place of Supply State Code`
  - Applicable Taxes: **CGST** (Rate / 2) + **SGST** (Rate / 2)
  - IGST: 0.00
- **Inter-State**: `Business State Code != Place of Supply State Code`
  - Applicable Taxes: **IGST** (Full Rate)
  - CGST: 0.00, SGST: 0.00

## 2. Calculation Order
1. Line Item Gross = `Quantity * Unit Price`
2. Line Item Discount = `Line Item Discount (Amount or %)`
3. Taxable Value = `Gross - Discount`
4. Applicable Tax Rate = Looked up from active rates (0%, 5%, 12%, 18%, 28%)
5. Split Tax Calculation:
   - For Intra-State: `CGST = Taxable Value * (Rate / 200)`, `SGST = Taxable Value * (Rate / 200)`
   - For Inter-State: `IGST = Taxable Value * (Rate / 100)`
6. Line Total = `Taxable Value + CGST + SGST + IGST`
7. Invoice Grand Total = `Sum(Line Totals)` rounded to 2 decimal places (`HALF_UP`).
