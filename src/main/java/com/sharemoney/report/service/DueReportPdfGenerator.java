package com.sharemoney.report.service;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.report.dto.DueReportResponse;
import com.sharemoney.report.dto.ReportLine;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Locale;

@Component
public class DueReportPdfGenerator {

    private static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    private static final String LOGO_PATH = "images/logo.png";

    private static final Color BRAND_COLOR = new Color(37, 99, 235);
    private static final Color ROW_ALT_COLOR = new Color(245, 247, 250);
    private static final Color PAID_ROW_COLOR = new Color(232, 245, 233);
    private static final Color DUE_AMOUNT_COLOR = new Color(197, 48, 48);
    private static final Color BORDER_COLOR = new Color(224, 224, 224);
    private static final Color MUTED_TEXT_COLOR = new Color(107, 114, 128);

    public byte[] generate(DueReportResponse report) {
        try {
            BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 22, Font.BOLD, Color.WHITE);
            Font subTitleFont = new Font(baseFont, 12, Font.NORMAL, Color.WHITE);
            Font headerFont = new Font(baseFont, 13, Font.BOLD, Color.WHITE);
            Font bodyFont = new Font(baseFont, 12, Font.NORMAL);
            Font mutedFont = new Font(baseFont, 10, Font.NORMAL, MUTED_TEXT_COLOR);
            Font dueAmountFont = new Font(baseFont, 12, Font.BOLD, DUE_AMOUNT_COLOR);
            Font paidAmountFont = new Font(baseFont, 12, Font.NORMAL, MUTED_TEXT_COLOR);
            Font totalLabelFont = new Font(baseFont, 15, Font.BOLD, BRAND_COLOR);
            Font footerFont = new Font(baseFont, 9, Font.NORMAL, MUTED_TEXT_COLOR);

            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(buildHeader(report, titleFont, subTitleFont));
            document.add(new Paragraph(" "));
            document.add(buildTable(report, headerFont, bodyFont, mutedFont, dueAmountFont, paidAmountFont));
            document.add(buildSummary(report, totalLabelFont));
            document.add(buildFooter(footerFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BusinessException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private PdfPTable buildHeader(DueReportResponse report, Font titleFont, Font subTitleFont) throws IOException, BadElementException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 3.4f});

        Image logo = loadLogo();
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(BRAND_COLOR);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(14);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (logo != null) {
            logo.scaleToFit(64, 64);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBackgroundColor(BRAND_COLOR);
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPadding(14);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.addElement(new Paragraph("Share Money", titleFont));
        textCell.addElement(new Paragraph("รายงานยอดที่ต้องชำระ", subTitleFont));
        Paragraph meta = new Paragraph("วันที่ออกรายงาน: " + report.issuedAt() + "   |   ผู้ออกรายงาน: " + report.issuedBy(), subTitleFont);
        meta.setSpacingBefore(4);
        textCell.addElement(meta);
        header.addCell(textCell);

        return header;
    }

    private Image loadLogo() throws IOException, BadElementException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(LOGO_PATH)) {
            if (in == null) {
                return null;
            }
            return Image.getInstance(in.readAllBytes());
        }
    }

    private PdfPTable buildTable(DueReportResponse report, Font headerFont, Font bodyFont, Font mutedFont,
                                  Font dueAmountFont, Font paidAmountFont) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 2.8f, 1.6f});
        table.setSpacingBefore(6);

        table.addCell(headerCell("รายการ", headerFont, Element.ALIGN_LEFT));
        table.addCell(headerCell("รายละเอียด", headerFont, Element.ALIGN_LEFT));
        table.addCell(headerCell("ยอดที่ต้องชำระ", headerFont, Element.ALIGN_RIGHT));

        int rowIndex = 0;
        for (ReportLine line : report.lines()) {
            Color rowColor = line.paid() ? PAID_ROW_COLOR : (rowIndex % 2 == 0 ? Color.WHITE : ROW_ALT_COLOR);
            Font detailFont = line.paid() ? mutedFont : bodyFont;
            String detail = line.what() + (line.paid() ? "  (ชำระแล้ว)" : "");

            table.addCell(bodyCell(line.title(), detailFont, Element.ALIGN_LEFT, rowColor));
            table.addCell(bodyCell(detail, detailFont, Element.ALIGN_LEFT, rowColor));
            table.addCell(bodyCell(formatAmount(line.due()), line.paid() ? paidAmountFont : dueAmountFont,
                    Element.ALIGN_RIGHT, rowColor));
            rowIndex++;
        }
        return table;
    }

    private PdfPCell headerCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(BRAND_COLOR);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private PdfPTable buildSummary(DueReportResponse report, Font totalLabelFont) {
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(100);
        summary.setWidths(new float[]{3f, 1.6f});
        summary.setSpacingBefore(10);

        PdfPCell label = new PdfPCell(new Phrase("ยอดรวมที่ต้องชำระ (" + report.dueCount() + " รายการ)", totalLabelFont));
        label.setBorder(Rectangle.TOP);
        label.setBorderColor(BRAND_COLOR);
        label.setBorderWidth(1.5f);
        label.setPadding(8);
        label.setHorizontalAlignment(Element.ALIGN_LEFT);
        summary.addCell(label);

        PdfPCell amount = new PdfPCell(new Phrase(formatAmount(report.total()) + " บาท", totalLabelFont));
        amount.setBorder(Rectangle.TOP);
        amount.setBorderColor(BRAND_COLOR);
        amount.setBorderWidth(1.5f);
        amount.setPadding(8);
        amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.addCell(amount);

        return summary;
    }

    private Paragraph buildFooter(Font footerFont) {
        Paragraph footer = new Paragraph("สร้างโดยระบบ Share Money", footerFont);
        footer.setSpacingBefore(24);
        footer.setAlignment(Element.ALIGN_CENTER);
        return footer;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
