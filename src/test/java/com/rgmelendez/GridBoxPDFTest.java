package com.rgmelendez;

import com.rgmelendez.core.GridBoxPDF;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GridBoxPDFTest {

    @Test
    void shouldCreateDocument() {
        GridBoxPDF pdf = new GridBoxPDF(
                GridBoxPDF.PageFormat.A4,
                GridBoxPDF.PageOrientation.PORTRAIT,
                GridBoxPDF.PageUnit.MILIMETERS
        );

        assertNotNull(pdf);
    }

    @Test
    void shouldIncrementPageNumber() {
        GridBoxPDF pdf = new GridBoxPDF();

        pdf.addPage();
        assertEquals(1, pdf.getPageNumber());

        pdf.addPage();
        assertEquals(2, pdf.getPageNumber());
    }

    @Test
    void shouldGeneratePdf() {
        GridBoxPDF pdf = new GridBoxPDF();

        pdf.addPage();
        pdf.cell("Hello GridBoxPDF", 0, "L");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        pdf.close(output);

        assertTrue(output.size() > 0);
    }
}
