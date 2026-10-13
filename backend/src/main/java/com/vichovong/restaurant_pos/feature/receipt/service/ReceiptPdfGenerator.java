package com.vichovong.restaurant_pos.feature.receipt.service;

import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.vichovong.restaurant_pos.common.exception.ApiException;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundLineResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundResponse;
import com.vichovong.restaurant_pos.feature.order.dto.OrderRoundSelectionResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.PaymentResponse;
import com.vichovong.restaurant_pos.feature.payment.dto.ReceiptResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an 80mm thermal-style receipt PDF (cashier spec §6 content: bill,
 * dual-currency totals, payment and change). Khmer text uses an embedded
 * Noto Sans Khmer; when the font resource is missing, Khmer runs (including
 * the riel sign) fall back to "KHR" wording so generation never fails.
 */
@Component
public class ReceiptPdfGenerator {

    private static final float WIDTH_80MM = 226.77f;
    private static final PageSize MAX_PAGE = new PageSize(WIDTH_80MM, 10000f);
    private static final float MARGIN = 12f;
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    // FontPrograms are reusable across documents; PdfFonts are not
    private final FontProgram khmerFontProgram;

    public ReceiptPdfGenerator() {
        this.khmerFontProgram = loadKhmerFont();
    }

    public byte[] generate(ReceiptResponse r) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Fonts fonts = new Fonts(khmerFontProgram);

            try (Document doc = new Document(pdf, MAX_PAGE)) {
                doc.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

                doc.add(text(r.restaurantName(), fonts, 12, true).setTextAlignment(TextAlignment.CENTER));
                doc.add(text("Receipt " + r.receiptNumber(), fonts, 9, true)
                        .setTextAlignment(TextAlignment.CENTER));
                doc.add(text("Table " + r.bill().tableNumber()
                        + "   " + TS.format(r.payment().paidAt()), fonts, 8, false)
                        .setTextAlignment(TextAlignment.CENTER));
                doc.add(separator());

                for (OrderRoundResponse round : r.bill().rounds()) {
                    doc.add(text("Round " + round.roundNumber(), fonts, 8, true));
                    for (OrderRoundLineResponse line : round.lines()) {
                        addLine(doc, line, fonts);
                    }
                }

                doc.add(separator());
                doc.add(amountRow("Subtotal", usd(r.bill().subtotal()), fonts, 8, false));
                doc.add(amountRow(vatLabel(r), usd(r.bill().vatAmount()), fonts, 8, false));
                doc.add(amountRow("TOTAL", usd(r.bill().grandTotal()), fonts, 10, true));
                if (r.bill().grandTotalKhr() != null) {
                    doc.add(amountRow("", khr(r.bill().grandTotalKhr(), fonts), fonts, 10, true));
                }

                doc.add(separator());
                PaymentResponse p = r.payment();
                doc.add(amountRow("Paid by " + p.method(),
                        money(p.amountTendered(), p.tenderedCurrency(), fonts), fonts, 8, false));
                if (p.changeUsd() != null && p.changeUsd().signum() > 0) {
                    doc.add(amountRow("Change", usd(p.changeUsd()), fonts, 8, false));
                    if (p.changeKhr() != null) {
                        doc.add(amountRow("", khr(p.changeKhr(), fonts), fonts, 8, false));
                    }
                }
                if (p.referenceNote() != null && !p.referenceNote().isBlank()) {
                    doc.add(text("Ref: " + p.referenceNote(), fonts, 7, false));
                }
                if (p.paidBy() != null) {
                    doc.add(text("Cashier: " + p.paidBy(), fonts, 7, false));
                }

                doc.add(separator());
                doc.add(text(fonts.hasKhmer() ? "Thank you! សូមអរគុណ" : "Thank you!", fonts, 9, false)
                        .setTextAlignment(TextAlignment.CENTER));
                
                // Crop the page to content height
                float bottomY = doc.getRenderer().getCurrentArea().getBBox().getY();
                float cropY = bottomY - MARGIN;
                if (cropY < 0) cropY = 0;
                float height = MAX_PAGE.getHeight() - cropY;
                pdf.getPage(1).setMediaBox(new Rectangle(0, cropY, WIDTH_80MM, height));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate receipt PDF: " + e.getMessage());
        }
    }

    /**
     * Plain items print as one row. Items with modifiers get a per-unit price
     * breakdown — base, each modifier's cost — closed by their own subtotal row.
     */
    private void addLine(Document doc, OrderRoundLineResponse line, Fonts fonts) {
        String header = line.quantity() + "x " + line.nameEn();
        if (line.selections().isEmpty()) {
            doc.add(amountRow(header, usd(line.lineTotal()), fonts, 8, false, 0));
            addRemark(doc, line, fonts);
            return;
        }

        doc.add(amountRow(header, usd(line.basePrice()), fonts, 8, false, 0));
        for (OrderRoundSelectionResponse sel : line.selections()) {
            String qty = sel.quantity() > 1 ? sel.quantity() + "x " : "";
            BigDecimal cost = sel.unitPrice().multiply(BigDecimal.valueOf(sel.quantity()));
            doc.add(amountRow("+ " + qty + sel.nameEn(), usd(cost), fonts, 7, false, 10));
        }
        addRemark(doc, line, fonts);
        String label = line.quantity() > 1
                ? "= " + line.quantity() + " × " + usd(line.unitPrice())
                : "Item total";
        doc.add(amountRow(label, usd(line.lineTotal()), fonts, 8, true, 10));
    }

    private void addRemark(Document doc, OrderRoundLineResponse line, Fonts fonts) {
        if (line.remark() != null && !line.remark().isBlank()) {
            doc.add(text("Note: " + line.remark(), fonts, 7, false).setMarginLeft(10));
        }
    }

    private String vatLabel(ReceiptResponse r) {
        return r.bill().rounds().stream().findFirst()
                .map(round -> "VAT (" + round.vatRate().movePointRight(2).stripTrailingZeros()
                        .toPlainString() + "%)")
                .orElse("VAT");
    }

    private static String usd(BigDecimal amount) {
        return String.format("$%,.2f", amount);
    }

    private String khr(BigDecimal amount, Fonts fonts) {
        return fonts.hasKhmer()
                ? String.format("៛%,.0f", amount)
                : String.format("KHR %,.0f", amount);
    }

    private String money(BigDecimal amount, String currency, Fonts fonts) {
        return "KHR".equals(currency)
                ? khr(amount, fonts)
                : String.format("$%,.2f", amount);
    }

    /** Label left, amount right on one line via a right-aligned tab stop. */
    private Paragraph amountRow(String label, String amount, Fonts fonts, float size, boolean bold) {
        return amountRow(label, amount, fonts, size, bold, 0);
    }

    private Paragraph amountRow(String label, String amount, Fonts fonts, float size,
                                boolean bold, float indent) {
        Paragraph p = new Paragraph().setFontSize(size).setMultipliedLeading(1.1f)
                .setMarginLeft(indent);
        // Tab position is relative to the paragraph's content box, so subtract
        // the indent to keep every amount on the same right edge
        p.addTabStops(new TabStop(WIDTH_80MM - 2 * MARGIN - indent, TabAlignment.RIGHT));
        chunks(label, fonts, bold).forEach(p::add);
        p.add(new Tab());
        chunks(amount, fonts, bold).forEach(p::add);
        return p;
    }

    private Paragraph text(String content, Fonts fonts, float size, boolean bold) {
        Paragraph p = new Paragraph().setFontSize(size).setMultipliedLeading(1.1f);
        chunks(content, fonts, bold).forEach(p::add);
        return p;
    }

    /** Splits mixed text into runs so Khmer glyphs get the embedded font. */
    private List<Text> chunks(String content, Fonts fonts, boolean bold) {
        List<Text> chunks = new ArrayList<>();
        if (content.isEmpty()) {
            return chunks;
        }
        StringBuilder run = new StringBuilder();
        boolean runIsKhmer = isKhmer(content.codePointAt(0));
        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            boolean khmer = isKhmer(cp);
            if (khmer != runIsKhmer) {
                chunks.add(chunk(run.toString(), runIsKhmer, fonts, bold));
                run.setLength(0);
                runIsKhmer = khmer;
            }
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        chunks.add(chunk(run.toString(), runIsKhmer, fonts, bold));
        return chunks;
    }

    private Text chunk(String content, boolean khmer, Fonts fonts, boolean bold) {
        return new Text(content).setFont(khmer && fonts.hasKhmer() ? fonts.khmer() : fonts.latin(bold));
    }

    private static boolean isKhmer(int codePoint) {
        return (codePoint >= 0x1780 && codePoint <= 0x17FF)   // Khmer (includes ៛)
                || (codePoint >= 0x19E0 && codePoint <= 0x19FF); // Khmer symbols
    }

    private static LineSeparator separator() {
        return new LineSeparator(new DashedLine(0.5f)).setMarginTop(4).setMarginBottom(4);
    }

    private static FontProgram loadKhmerFont() {
        try (InputStream in = ReceiptPdfGenerator.class.getResourceAsStream(
                "/fonts/NotoSansKhmer-Regular.ttf")) {
            return in == null ? null : FontProgramFactory.createFont(in.readAllBytes());
        } catch (IOException e) {
            return null;
        }
    }

    /** Per-document font set — PdfFont instances cannot be shared between PDFs. */
    private record Fonts(PdfFont regular, PdfFont bold, PdfFont khmerOrNull) {

        Fonts(FontProgram khmerProgram) throws IOException {
            this(PdfFontFactory.createFont(StandardFonts.HELVETICA),
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD),
                    khmerProgram == null ? null
                            : PdfFontFactory.createFont(khmerProgram, PdfEncodings.IDENTITY_H));
        }

        boolean hasKhmer() {
            return khmerOrNull != null;
        }

        PdfFont khmer() {
            return khmerOrNull;
        }

        PdfFont latin(boolean isBold) {
            return isBold ? bold : regular;
        }
    }
}
