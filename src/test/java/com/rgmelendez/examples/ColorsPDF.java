package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

import java.awt.*;

public class ColorsPDF extends GridBoxPDF {

    private static final String PATH = "src/test/resources/documents/";

    // page header
    public void header() {
        this.setFont("Helvetica", "B", 16);
        this.setFontColor(Color.RED);
        this.setDrawColor(Color.BLUE);
        this.cell("Page with long text", 0, "C", "1");
        this.setY(70);
    }

    // page footer
    public void footer() {
        // Position at 1 cm from bottom
        this.setFont("Courier", "I", 10);
        this.setFontColor(Color.GRAY);
        this.cell("Page " + this.getPageNumber(), 0, "C");
    }

    public static void main(String[] args) {
        ColorsPDF gridBox = new ColorsPDF();
        gridBox.addPage();

        for (int i = 1; i <= 40; i++) {
            gridBox.setFontColor(Color.BLACK);
            gridBox.setFont("Times", "", 12);
            gridBox.cell("Printing line number " + i, 0, "L");
        }
        gridBox.close(PATH + "colors.pdf");
    }
}
