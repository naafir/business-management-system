package com.antigravity.billing.service;

import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.dto.invoice.InvoiceItemDto;
import com.antigravity.billing.entity.BusinessSettings;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final BusinessSettingsRepository businessSettingsRepository;

    public byte[] generateInvoicePdf(InvoiceResponseDto invoice) {
        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font Styling
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(79, 70, 229));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(51, 65, 85));
            Font bodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(15, 23, 42));
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(100, 116, 139));

            // Color Palette
            Color primaryIndigo = new Color(79, 70, 229);
            Color lightBg = new Color(248, 250, 252);
            Color borderGray = new Color(226, 232, 240);

            // 1. Header Table (Business Details vs Invoice Meta)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            // Business Cell (Left)
            PdfPCell busCell = new PdfPCell();
            busCell.setBorder(Rectangle.NO_BORDER);
            String busName = (settings != null && settings.getLegalName() != null) ? settings.getLegalName() : "BUSINESS MANAGEMENT SYSTEM";
            busCell.addElement(new Paragraph(busName, titleFont));
            if (settings != null && settings.getTradeName() != null && !settings.getTradeName().isBlank()) {
                busCell.addElement(new Paragraph("Trade Name: " + settings.getTradeName(), smallFont));
            }
            if (settings != null && settings.getGstin() != null && !settings.getGstin().isBlank()) {
                busCell.addElement(new Paragraph("GSTIN: " + settings.getGstin(), bodyBold));
            }
            if (settings != null && settings.getAddressLine1() != null) {
                busCell.addElement(new Paragraph(settings.getAddressLine1() + (settings.getCity() != null ? ", " + settings.getCity() : ""), smallFont));
            }
            if (settings != null && settings.getPhone() != null) {
                busCell.addElement(new Paragraph("Phone: " + settings.getPhone() + " | Email: " + (settings.getEmail() != null ? settings.getEmail() : ""), smallFont));
            }
            headerTable.addCell(busCell);

            // Invoice Header Cell (Right)
            PdfPCell invMetaCell = new PdfPCell();
            invMetaCell.setBorder(Rectangle.NO_BORDER);
            invMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph taxTitle = new Paragraph("TAX INVOICE", subTitleFont);
            taxTitle.setAlignment(Element.ALIGN_RIGHT);
            invMetaCell.addElement(taxTitle);

            Paragraph invNumP = new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), bodyBold);
            invNumP.setAlignment(Element.ALIGN_RIGHT);
            invMetaCell.addElement(invNumP);

            Paragraph invDateP = new Paragraph("Invoice Date: " + invoice.getInvoiceDate(), bodyFont);
            invDateP.setAlignment(Element.ALIGN_RIGHT);
            invMetaCell.addElement(invDateP);

            if (invoice.getDueDate() != null) {
                Paragraph invDueP = new Paragraph("Due Date: " + invoice.getDueDate(), bodyFont);
                invDueP.setAlignment(Element.ALIGN_RIGHT);
                invMetaCell.addElement(invDueP);
            }

            Paragraph statusP = new Paragraph("Status: " + invoice.getStatus().name() + " (" + invoice.getPaymentStatus().name() + ")", bodyBold);
            statusP.setAlignment(Element.ALIGN_RIGHT);
            invMetaCell.addElement(statusP);

            headerTable.addCell(invMetaCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));

            // 2. Customer & Bank Two-Column Section
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            // Customer Cell
            PdfPCell custCell = new PdfPCell();
            custCell.setBackgroundColor(lightBg);
            custCell.setBorderColor(borderGray);
            custCell.setPadding(8);
            custCell.addElement(new Paragraph("BILLED TO (CUSTOMER):", bodyBold));
            custCell.addElement(new Paragraph(invoice.getCustomerName(), bodyBold));
            if (invoice.getCustomerGstin() != null && !invoice.getCustomerGstin().isBlank()) {
                custCell.addElement(new Paragraph("GSTIN: " + invoice.getCustomerGstin(), bodyFont));
            } else {
                custCell.addElement(new Paragraph("Unregistered Customer", smallFont));
            }
            custCell.addElement(new Paragraph("Place of Supply: " + invoice.getPlaceOfSupplyState() + " (" + invoice.getPlaceOfSupplyCode() + ")", bodyFont));
            infoTable.addCell(custCell);

            // Bank Details Cell
            PdfPCell bankCell = new PdfPCell();
            bankCell.setBackgroundColor(lightBg);
            bankCell.setBorderColor(borderGray);
            bankCell.setPadding(8);
            bankCell.addElement(new Paragraph("PAYMENT & BANK DETAILS:", bodyBold));
            if (settings != null && settings.getBankName() != null) {
                bankCell.addElement(new Paragraph("Bank: " + settings.getBankName(), bodyFont));
                bankCell.addElement(new Paragraph("A/C No: " + settings.getBankAccountNumber(), bodyFont));
                bankCell.addElement(new Paragraph("IFSC: " + settings.getBankIfsc() + " | Branch: " + (settings.getBankBranch() != null ? settings.getBankBranch() : ""), bodyFont));
                if (settings.getBankUpiId() != null) {
                    bankCell.addElement(new Paragraph("UPI ID: " + settings.getBankUpiId(), bodyFont));
                }
            } else {
                bankCell.addElement(new Paragraph("Payment Method: " + (invoice.getPaymentMethod() != null ? invoice.getPaymentMethod() : "N/A"), bodyFont));
            }
            infoTable.addCell(bankCell);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 3. Line Items Table
            PdfPTable itemTable = new PdfPTable(8);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{30, 10, 10, 12, 10, 12, 10, 16});

            String[] headers = {"Item / Product", "HSN/SAC", "Qty", "Unit Price", "Disc %", "Taxable", "GST %", "Total (₹)"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(primaryIndigo);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                itemTable.addCell(cell);
            }

            boolean isEven = false;
            if (invoice.getItems() != null) {
                for (InvoiceItemDto item : invoice.getItems()) {
                    Color rowBg = isEven ? lightBg : Color.WHITE;
                    isEven = !isEven;

                    PdfPCell pNameCell = new PdfPCell(new Phrase(item.getProductName(), bodyFont));
                    pNameCell.setBackgroundColor(rowBg);
                    pNameCell.setPadding(5);
                    itemTable.addCell(pNameCell);

                    PdfPCell hsnCell = new PdfPCell(new Phrase(item.getHsnSac() != null ? item.getHsnSac() : "-", bodyFont));
                    hsnCell.setBackgroundColor(rowBg);
                    hsnCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    hsnCell.setPadding(5);
                    itemTable.addCell(hsnCell);

                    PdfPCell qtyCell = new PdfPCell(new Phrase(item.getQuantity() + " " + item.getUnit(), bodyFont));
                    qtyCell.setBackgroundColor(rowBg);
                    qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    qtyCell.setPadding(5);
                    itemTable.addCell(qtyCell);

                    PdfPCell priceCell = new PdfPCell(new Phrase(formatAmt(item.getUnitPrice()), bodyFont));
                    priceCell.setBackgroundColor(rowBg);
                    priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    priceCell.setPadding(5);
                    itemTable.addCell(priceCell);

                    PdfPCell discCell = new PdfPCell(new Phrase(item.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0 ? item.getDiscountPercent() + "%" : "-", bodyFont));
                    discCell.setBackgroundColor(rowBg);
                    discCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    discCell.setPadding(5);
                    itemTable.addCell(discCell);

                    PdfPCell taxValCell = new PdfPCell(new Phrase(formatAmt(item.getTaxableAmount()), bodyFont));
                    taxValCell.setBackgroundColor(rowBg);
                    taxValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    taxValCell.setPadding(5);
                    itemTable.addCell(taxValCell);

                    PdfPCell gstCell = new PdfPCell(new Phrase(item.getGstRatePercent() + "%", bodyFont));
                    gstCell.setBackgroundColor(rowBg);
                    gstCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    gstCell.setPadding(5);
                    itemTable.addCell(gstCell);

                    PdfPCell totCell = new PdfPCell(new Phrase(formatAmt(item.getTotalAmount()), bodyBold));
                    totCell.setBackgroundColor(rowBg);
                    totCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totCell.setPadding(5);
                    itemTable.addCell(totCell);
                }
            }

            document.add(itemTable);
            document.add(new Paragraph(" "));

            // 4. Summary Totals & Signatures Section
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{55, 45});

            // Terms & Notes (Left)
            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(Rectangle.NO_BORDER);
            termsCell.addElement(new Paragraph("Terms & Conditions:", bodyBold));
            String termsText = (settings != null && settings.getInvoiceTerms() != null) ? settings.getInvoiceTerms() : "1. Goods once sold will not be taken back.\n2. Subject to local jurisdiction.";
            termsCell.addElement(new Paragraph(termsText, smallFont));

            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                termsCell.addElement(new Paragraph("\nNotes: " + invoice.getNotes(), smallFont));
            }
            footerTable.addCell(termsCell);

            // Financial Summary (Right)
            PdfPTable summaryBox = new PdfPTable(2);
            summaryBox.setWidthPercentage(100);

            addSummaryRow(summaryBox, "Subtotal:", formatAmt(invoice.getSubtotal()), bodyFont);
            if (invoice.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(summaryBox, "Discount:", "-" + formatAmt(invoice.getTotalDiscount()), bodyFont);
            }
            addSummaryRow(summaryBox, "Taxable Amount:", formatAmt(invoice.getTaxableAmount()), bodyFont);

            if (invoice.getCgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(summaryBox, "CGST:", formatAmt(invoice.getCgstAmount()), smallFont);
                addSummaryRow(summaryBox, "SGST:", formatAmt(invoice.getSgstAmount()), smallFont);
            }
            if (invoice.getIgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(summaryBox, "IGST:", formatAmt(invoice.getIgstAmount()), smallFont);
            }

            addSummaryRow(summaryBox, "Grand Total:", "INR " + formatAmt(invoice.getGrandTotal()), bodyBold);
            addSummaryRow(summaryBox, "Amount Paid:", formatAmt(invoice.getAmountPaid()), bodyFont);
            addSummaryRow(summaryBox, "Balance Due:", formatAmt(invoice.getBalanceDue()), bodyBold);

            PdfPCell sumWrapper = new PdfPCell(summaryBox);
            sumWrapper.setBackgroundColor(lightBg);
            sumWrapper.setBorderColor(borderGray);
            sumWrapper.setPadding(6);
            footerTable.addCell(sumWrapper);

            document.add(footerTable);

            // 5. Signature Line
            document.add(new Paragraph(" "));
            Paragraph sigP = new Paragraph("For " + busName + "\n\n\nAuthorized Signatory", smallFont);
            sigP.setAlignment(Element.ALIGN_RIGHT);
            document.add(sigP);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF invoice", e);
        }

        return baos.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, font));
        lCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, font));
        vCell.setBorder(Rectangle.NO_BORDER);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vCell);
    }

    private String formatAmt(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
