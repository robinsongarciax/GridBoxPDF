package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

public class HeaderFooter extends GridBoxPDF {

    private static final String PATH = "src/test/resources/documents/";

    // page header
    public void header() {
        // Image insertion example.
        // Download an image to your local machine and replace the path below
        //    with the actual image path.
        // Supported image formats depend on the underlying PDFBox implementation.
        // this.drawImage("/path/to/your/image.png", 10, 10, 50);

        this.setX(80);
        this.setFont("Times", "B", 16);
        this.cell("Title", 30, "C", "1");
        this.setY(70);
    }

    // page footer
    public void footer() {
        this.setFont("Courier", "I", 10);
        this.cell("Page " + this.getPageNumber(), 0, "C");
    }

    public static void main(String[] args) {
        HeaderFooter gridBox = new HeaderFooter();
        gridBox.addPage();

        // Print enough lines to demonstrate automatic page breaks.
        // By default, the page break threshold is 10 mm from the bottom.
        // Use setPageBreak() to change this threshold.
        gridBox.setFont("Times", "", 12);

        for (int i = 1; i <= 40; i++) {
            gridBox.cell("Printing line number " + i, 0, "L");
        }
        gridBox.close(PATH + "header-footer.pdf");
    }
}
