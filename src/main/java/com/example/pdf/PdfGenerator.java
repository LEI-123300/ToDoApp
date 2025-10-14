package com.example.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class PdfGenerator {
    public static byte[] generateTasksPdfBytes(List<String> tasks, String title) throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(titleParagraph);

            document.add(new Paragraph(" ")); // linha em branco

            // Lista de tarefas
            Font taskFont = new Font(Font.HELVETICA, 12);
            if (tasks == null || tasks.isEmpty()) {
                document.add(new Paragraph("Sem tarefas registadas.", taskFont));
            } else {
                for (String t : tasks) {
                    Paragraph p = new Paragraph(" - " + t, taskFont);
                    p.setSpacingBefore(4);
                    document.add(p);
                }
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new DocumentException("Erro ao gerar PDF: " + e.getMessage());
        }
    }

    public static ByteArrayInputStream toInputStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }
}
