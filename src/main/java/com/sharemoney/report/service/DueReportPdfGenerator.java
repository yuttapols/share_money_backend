package com.sharemoney.report.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
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
import java.math.BigDecimal;
import java.util.Locale;

@Component
public class DueReportPdfGenerator {

    private static final String FONT_PATH = "fonts/THSarabunNew.ttf";
    private static final Color HEADER_BACKGROUND = new Color(230, 230, 230);

    public byte[] generate(DueReportResponse report) {
        try {
            BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 20, Font.BOLD);
            Font headerFont = new Font(baseFont, 12, Font.BOLD);
            Font bodyFont = new Font(baseFont, 12, Font.NORMAL);

            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Share Money - รายงานยอดที่ต้องชำระ", titleFont));
            document.add(new Paragraph("วันที่ออกรายงาน: " + report.issuedAt(), bodyFont));
            document.add(new Paragraph("ผู้ออกรายงาน: " + report.issuedBy(), bodyFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(headerCell("รายการ", headerFont));
            table.addCell(headerCell("รายละเอียด", headerFont));
            table.addCell(headerCell("ยอดที่ต้องชำระ", headerFont));

            for (ReportLine line : report.lines()) {
                table.addCell(new Phrase(line.title(), bodyFont));
                table.addCell(new Phrase(line.what(), bodyFont));
                table.addCell(new Phrase(formatAmount(line.due()), bodyFont));
            }
            document.add(table);

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("ยอดรวมที่ต้องชำระ: " + formatAmount(report.total()) + " บาท", headerFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BusinessException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setPadding(6);
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
