package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

import java.io.IOException;

public class PageBreak extends GridBoxPDF {

    public void footer() {
        this.setFont("COURIER", "", 8);
        this.cell("Page " + this.getPageNumber(), 0, "C", "1");
    }

    public static void main(String[] args) throws IOException {

        String longText = "em ipsum dolor sit amet, consectetur adipiscing elit. Nunc ut pellentesque leo. Sed et felis vitae elit convallis pellentesque a sed leo. Nullam congue interdum nibh, in dignissim ligula dictum in. Etiam convallis elementum ex, a tempor tellus feugiat sit amet. Proin non est ut libero vulputate eleifend. Aliquam erat volutpat. Proin et dolor mi. Mauris placerat, lorem non faucibus porta, tellus mi molestie libero, id egestas magna mi id lorem. In nec diam sodales, dapibus nisl et, interdum quam. Sed vulputate mi nec lorem gravida facilisis. Vivamus aliquet leo et massa euismod lacinia. Quisque tristique est vitae nulla efficitur, eget tristique massa mollis. In hac habitasse platea dictumst. Sed in ex sit amet leo bibendum ultrices et sed eros. Proin vel augue id est tempus suscipit in non massa.";
        String extraLongText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc ut pellentesque leo. Sed et felis vitae elit convallis pellentesque a sed leo. Nullam congue interdum nibh, in dignissim ligula dictum in. Etiam convallis elementum ex, a tempor tellus feugiat sit amet. Proin non est ut libero vulputate eleifend. Aliquam erat volutpat. Proin et dolor mi. Mauris placerat, lorem non faucibus porta, tellus mi molestie libero, id egestas magna mi id lorem. In nec diam sodales, dapibus nisl et, interdum quam. Sed vulputate mi nec lorem gravida facilisis. Vivamus aliquet leo et massa euismod lacinia. Quisque tristique est vitae nulla efficitur, eget tristique massa mollis. In hac habitasse platea dictumst. Sed in ex sit amet leo bibendum ultrices et sed eros. Proin vel augue id est tempus suscipit in non massa. \n\nPellentesque in tempus lectus. Curabitur vitae sem vitae felis aliquet tempus. Duis malesuada id erat ut venenatis. Ut in ligula scelerisque, tempor sem id, consectetur est. Suspendisse potenti. Nam gravida tincidunt ex, id convallis eros congue vel. Vivamus sit amet tellus scelerisque, posuere tortor nec, pellentesque diam. Proin eu ipsum efficitur, vehicula metus et, sodales lorem. Curabitur feugiat gravida tincidunt. In hac habitasse platea dictumst. Donec sit amet est id nulla dictum eleifend. Duis feugiat, ante at pellentesque tincidunt, ex lorem congue ex, at rhoncus erat urna sed justo. Morbi finibus metus ac facilisis malesuada.";

        PageBreak gridBox = new PageBreak();

        gridBox.addPage();

        gridBox.setFont("HELVETICA", "B", 18);
        gridBox.cell("Hello GridBoxPDF", 80, "L", "1", 0);
        gridBox.multiCell("Hello GridBoxPDF", 0, "L", "1");

        gridBox.setXY(98, 50);
        // This will perform and automatic page break
        gridBox.setFont("HELVETICA", "", 16);
        gridBox.multiCell(extraLongText, 0, "L", "1");

        gridBox.setFont("HELVETICA", "", 12);
        gridBox.multiCell("Text  with  double  spaces  between  words", 0, "L", "1");
        gridBox.multiCell("Text with single space between words", 0, "L", "1");

        gridBox.setFont("HELVETICA", "I");
        gridBox.cell("End!", 0, "L", "1", 2);
        gridBox.lineBreak();

        // Manual page break
        gridBox.addPage();
        gridBox.inlineText(longText);
        gridBox.close("src/test/resources/documents/page-breaks.pdf");
    }

}
