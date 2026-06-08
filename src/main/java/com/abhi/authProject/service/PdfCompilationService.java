package com.abhi.authProject.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Font;
import com.itextpdf.text.BaseColor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfCompilationService {

    public byte[] compileImagesToPdf(List<MultipartFile> images, String uploaderName) throws Exception {
        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 52, Font.BOLD, new BaseColor(200, 200, 200, 100)); // Light grey, semi-transparent
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);

        for (MultipartFile file : images) {
            document.newPage();

            // Load image
            Image img = Image.getInstance(file.getBytes());
            
            // Scale image to fit A4 page
            float scaler = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin() - 0) / img.getWidth()) * 100;
            img.scalePercent(scaler);
            img.setAlignment(Element.ALIGN_CENTER | Element.ALIGN_TOP);
            
            document.add(img);

            // Add Watermark
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.3f); // 30% opacity
            canvas.setGState(gs);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase("Downloaded from Hack-2-Hired", watermarkFont), 297.5f, 421, 45); // Center of A4
            
            // Reset opacity for footer
            gs.setFillOpacity(1f);
            canvas.setGState(gs);

            // Add Accountability Footer
            String footerText = "Uploaded by: " + uploaderName;
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(footerText, footerFont), 297.5f, 20, 0); // Bottom center
        }

        document.close();
        return baos.toByteArray();
    }
}
