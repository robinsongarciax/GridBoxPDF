package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

public class Minimal {

    private static final String PATH = "src/test/resources/documents/";

    public static void main(String[] args) {
        GridBoxPDF gridBox = new GridBoxPDF();

        gridBox.addPage();
        gridBox.cell("Hello GridBox !");
        gridBox.close(PATH + "minimal.pdf");
    }
}
