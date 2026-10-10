package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

import java.awt.*;

public class TablesPDF extends GridBoxPDF {

    private static final String PATH = "src/test/resources/documents/";

    void printBasic(String header, String data) {
        String[] headers = header.split("\\|");
        for (String columnHeader : headers) {
            this.cell(columnHeader, 40, "L", "1");
        }
        this.lineBreak();

        String[] rows = data.split("\n");
        for (String row : rows) {
            String[] columns = row.split("\\|");
            for (String column : columns) {
                this.cell(column, 40, "L", "1");
            }
            this.lineBreak();
        }
    }

    void printImproved(String header, String data) {
        String[] headers = header.split("\\|");
        for (String columnHeader : headers) {
            this.cell(columnHeader, 40, "C", "1");
        }
        this.lineBreak();

        String[] rows = data.split("\n");
        String borders = "LR";
        for (int i = 0; i < rows.length; i++) {
            if (i + 1 == rows.length) borders += "B";

            String[] columns = rows[i].split("\\|");
            this.cell(columns[0], 40, "L", borders);
            this.cell(columns[1], 40, "L", borders);
            this.cell(String.format("%,.0f", Double.parseDouble(columns[2])), 40, "R", borders);
            this.cell(String.format("%,.0f", Double.parseDouble(columns[3])), 40, "R", borders);
            this.lineBreak();
        }
    }

    void printColored(String header, String data) {
        String[] headers = header.split("\\|");
        this.setFillColor(0, 123, 255);
        this.setDrawColor(108, 117, 125);
        this.setFontColor(Color.WHITE);
        this.setFont("", "B");
        this.setRenderFill(true);
        for (String columnHeader : headers) {
            this.cell(columnHeader, 40, "C", "1");
        }
        this.lineBreak();

        this.setFont("", "");
        this.setFillColor(248, 249, 250);
        this.setFontColor(Color.BLACK);
        String[] rows = data.split("\n");
        String borders = "LR";
        boolean isFill = false;
        for (int i = 0; i < rows.length; i++) {
            if (i + 1 == rows.length) borders += "B";
            this.setRenderFill(isFill);
            String[] columns = rows[i].split("\\|");
            this.cell(columns[0], 40, "L", borders);
            this.cell(columns[1], 40, "L", borders);
            this.cell(String.format("%,.0f", Double.parseDouble(columns[2])), 40, "R", borders);
            this.cell(String.format("%,.0f", Double.parseDouble(columns[3])), 40, "R", borders);
            this.lineBreak();
            isFill = !isFill;
        }
    }

    public static void main(String[] args) {
        String header = "Country|Capital|Area (sq km)|Pop. (thousands)";
        String data = "Austria|Vienna|83859|8075\n" +
                "Belgium|Brussels|30518|10192\n" +
                "Denmark|Copenhagen|43094|5295\n" +
                "Finland|Helsinki|304529|5147\n" +
                "France|Paris|543965|58728\n" +
                "Germany|Berlin|357022|82057\n" +
                "Greece|Athens|131625|10511\n" +
                "Ireland|Dublin|70723|3694\n" +
                "Italy|Roma|301316|57563\n" +
                "Luxembourg|Luxembourg|2586|424\n" +
                "Netherlands|Amsterdam|41526|15654\n" +
                "Portugal|Lisbon|91906|9957\n" +
                "Spain|Madrid|504790|39348\n" +
                "Sweden|Stockholm|410934|8839\n" +
                "United Kingdom|London|243820|58862";

        TablesPDF gridBox = new TablesPDF();
        gridBox.addPage();
        gridBox.printBasic(header, data);

        gridBox.addPage();
        gridBox.printImproved(header, data);

        gridBox.addPage();
        gridBox.printColored(header, data);

        gridBox.close(PATH + "tables.pdf");

    }
}
