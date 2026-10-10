package com.rgmelendez.examples;

import com.rgmelendez.core.GridBoxPDF;

public class CustomFont {

    private static final String PATH = "src/test/resources/documents/";

    public static void main(String[] args) {
        String longText = "em ipsum dolor sit amet, consectetur adipiscing elit. Nunc ut pellentesque leo. Sed et felis vitae elit convallis pellentesque a sed leo. Nullam congue interdum nibh, in dignissim ligula dictum in. Etiam convallis elementum ex, a tempor tellus feugiat sit amet. Proin non est ut libero vulputate eleifend. Aliquam erat volutpat. Proin et dolor mi. Mauris placerat, lorem non faucibus porta, tellus mi molestie libero, id egestas magna mi id lorem. In nec diam sodales, dapibus nisl et, interdum quam. Sed vulputate mi nec lorem gravida facilisis. Vivamus aliquet leo et massa euismod lacinia. Quisque tristique est vitae nulla efficitur, eget tristique massa mollis. In hac habitasse platea dictumst. Sed in ex sit amet leo bibendum ultrices et sed eros. Proin vel augue id est tempus suscipit in non massa.";

        GridBoxPDF gridBox = new GridBoxPDF();

        gridBox.addPage();

        // Loads the font from the specified resource path using the given base name.
        // By default, GridBoxPDF searches for and registers the Regular, Bold,
        // Italic, and Bold Italic styles.
        gridBox.addFontFromSource("fonts/Roboto/static/", "Roboto");
        gridBox.setFont("Roboto", "B");
        gridBox.cell("Testing external fonts", 0, "C", "0", 2);
        gridBox.lineBreak();

        // Reuse the current font family and change its style to Italic.
        gridBox.setFont("Roboto", "B");
        gridBox.cell("Roboto Font", 0, "L", "0", 2);
        // Reuse the current font when fontName is empty.
        gridBox.setFont("", "I");
        gridBox.multiCell(longText);
        gridBox.lineBreak();

        // To test loading a font from the file system:
        // 1. Download Liberation Fonts from:
        //    https://github.com/liberationfonts/liberation-fonts/releases
        // 2. Extract the archive and replace the path below with the directory
        //    containing the required font files.

        /*
        gridBox.addFontFromFile("/path/to/liberation-sans/", "LiberationSans");
        gridBox.setFont("LiberationSans", "B");
        gridBox.cell("Liberation Sans Font", 0, "L", "0", 2);

        gridBox.setFont("", "I");
        gridBox.multiCell(longText);
        */

        gridBox.close(PATH + "custom-font.pdf");
    }
}
