/*
 * Copyright 2026 Robinson Artemio Garcia Melendez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rgmelendez.core;

import com.rgmelendez.config.PDFConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Main class for creating PDF documents with GridBoxPDF.
 *
 * <p>Default configuration:</p>
 * <ul>
 *   <li>Paper: Letter</li>
 *   <li>Orientation: Portrait</li>
 *   <li>Margins: 10 mm</li>
 *   <li>Unit: millimeters</li>
 *   <li>Font: Helvetica</li>
 *   <li>Font size: 12</li>
 * </ul>
 *
 * <p>Content is positioned from top to bottom and from left to right
 * within the document margins.
 * Positions, dimensions, and margins are expressed in the
 * unit configured for the document.</p>
 */
public class GridBoxPDF {

    private PDDocument document;
    private PDPageContentStream writer;
    private PDRectangle pageFormat = PDRectangle.LETTER;
    private final GridBoxFontManager fontManager = new GridBoxFontManager();

    /**
     * Supported page formats.
     */
    public enum PageFormat {
        /** US Letter format. */
        LETTER,

        /** ISO A4 format. */
        A4,

        /** US Legal format. */
        LEGAL,

        /** ISO A5 format. */
        A5
    }

    private String pageOrientation;

    /**
     * Measurement units supported by GridBoxPDF.
     */
    public enum PageUnit {
        /** Centimeters. */
        CENTIMETERS(PDFConfig.PAGEUNIT_CM),

        /** Millimeters. */
        MILIMETERS(PDFConfig.PAGEUNIT_MM),

        /** Inches. */
        INCHES(PDFConfig.PAGEUNIT_INCH),

        /** PDF points. */
        POINTS(PDFConfig.PAGEUNIT_PTS);

        private final float value;

        private PageUnit(float value) {
            this.value = value;
        }

        /**
         * Returns the conversion factor from this unit to PDF points.
         *
         * @return conversion factor to PDF points
         */
        public float getValue() {
            return value;
        }
    }

    /**
     * Page orientation.
     */
    public enum PageOrientation {
        /** Landscape orientation. */
        LANDSCAPE,

        /** Portrait orientation. */
        PORTRAIT
    }

    private float pageUnit = PDFConfig.PAGEUNIT_MM;
    private float pageHeight;
    private float pageWidth;
    private int pageNumber = 0;
    private boolean isFooterWriting = false;
    private boolean isHeaderWriting = false;

    // font
    private float fontSize = 12;
    private String fontName;
    private String fontStyle;

    // positions
    private float xPosition;
    private float yPosition;
    private float currentHeightSize;

    // margins
    private float marginLeft = 10 * pageUnit;
    private float marginRight = 10 * pageUnit;
    private float marginTop = 10 * pageUnit;
    private float pageBreak = 10 * pageUnit;

    private boolean renderLine = true;
    private boolean renderFill = false;

    // colors
    private Color fontColor = Color.BLACK;
    private Color drawColor = Color.BLACK;
    private Color fillColor = Color.WHITE;

    // images
    private final Map<String, PDImageXObject> images = new HashMap<>();

    /**
     * Creates a new PDF document using the default configuration.
     *
     * <p>The default configuration uses Letter paper, portrait
     * orientation, 10 mm margins, Helvetica as the default font,
     * and millimeters as the default unit.</p>
     */
    public GridBoxPDF() {
        initialize();
    }

    /**
     * Creates a PDF document with the specified page format, orientation,
     * and measurement unit.
     *
     * @param pageFormat Page format to use for the document
     * @param pageOrientation Page orientation to use for the document
     * @param pageUnit Measurement unit used throughout the document
     * @see PageFormat
     * @see PageOrientation
     * @see PageUnit
     */
    public GridBoxPDF(PageFormat pageFormat, PageOrientation pageOrientation, PageUnit pageUnit) {
        switch (pageFormat) {
            case A4:
                this.pageFormat = PDRectangle.A4;
                break;
            case LEGAL:
                this.pageFormat = PDRectangle.LEGAL;
                break;
            case A5:
                this.pageFormat = PDRectangle.A5;
                break;
        }

        if (PageOrientation.LANDSCAPE.equals(pageOrientation)) {
            this.pageOrientation = pageOrientation.toString();
        }

        this.pageUnit = pageUnit.getValue();

        initialize();
    }

    /**
     * Closes the PDF document and writes it to the specified file.
     *
     * @param fileName Path and name of the output PDF file
     * @throws RuntimeException if the document cannot be saved
     */
    public void close(String fileName) {
        printFooter();
        try {
            writer.close();
            document.save(new File(fileName));
            document.close();
        } catch (IOException e) {
            throw new RuntimeException("It is not possible to save the document! " + e.getMessage());
        }
    }

    /**
     * Closes the PDF document and writes it to the specified output stream.
     *
     * @param pdfStream Output stream that receives the generated PDF
     * @throws RuntimeException if the document cannot be saved
     */
    public void close(ByteArrayOutputStream pdfStream) {
        printFooter();
        try {
            writer.close();
            document.save(pdfStream);
            document.close();
        } catch (IOException e) {
            throw new RuntimeException("It is not possible to save the document! " + e.getMessage());
        }
    }

    /**
     * Adds a new page to the document.
     *
     * <p>The current page is completed before the new page is created.
     * If a footer is defined, it is rendered before the new page.
     * If a header is defined, it is rendered at the start of the new page.</p>
     */
    public void addPage() {
        if (pageNumber > 0 && !isFooterWriting && !isHeaderWriting) {
            isFooterWriting = true;
            this.setXY(0, -pageBreak/pageUnit);
            GBFontInfo fontInfo = saveFontState();
            footer();
            restoreFontState(fontInfo);
            isFooterWriting = false;
        }
        PDPage pdPage = new PDPage(pageFormat);
        if ("LANDSCAPE".equals(pageOrientation)) {
            pdPage.setRotation(90);
        }
        try {
            if (pageNumber > 0) {
                writer.close();
            }
            pageNumber++;
            document.addPage(pdPage);
            writer = new PDPageContentStream(document, pdPage);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        setXY(0, 0);
        if (!isFooterWriting && !isHeaderWriting) {
            isHeaderWriting = true;
            GBFontInfo fontInfo = saveFontState();
            header();
            restoreFontState(fontInfo);
            this.lineBreak();
            isHeaderWriting = false;
        }
    }

    private void printFooter() {
        if (pageNumber > 0) {
            isFooterWriting = true;
            this.setXY(0, -pageBreak/pageUnit);
            GBFontInfo fontInfo = saveFontState();
            footer();
            restoreFontState(fontInfo);
            isFooterWriting = false;
        }
    }

    /**
     * Returns the number of the current page.
     *
     * @return current page number, starting at {@code 1}
     */
    public int getPageNumber() {
        return pageNumber;
    }

    private void initialize() {
        document = new PDDocument();

        pageHeight = pageFormat.getHeight();
        pageWidth = pageFormat.getWidth();
        setDefaultMargins();
        setFont("HELVETICA");
    }

    private void setDefaultMargins() {
        setMargins(10, 10, 10);
    }

    /**
     * Sets the margins used for document content.
     *
     * <p>Margin values are expressed in the unit configured for the document.</p>
     *
     * @param left Left margin value
     * @param right Right margin value
     * @param top Top margin value
     */
    public void setMargins(float left, float right, float top) {
        marginLeft = left * pageUnit;
        marginRight = right * pageUnit;
        marginTop = top * pageUnit;
    }

    /**
     * Sets both the horizontal and vertical positions of the current text cursor simultaneously.
     *
     * <p>The positions are relative to the document content area,
     * taking the top and left margins into account.</p>
     *
     * @param x Horizontal position from the left margin.
     * @param y Vertical position from the top margin.
     */
    public void setXY(float x, float y) {
        setX(x);
        setY(y);
    }

    /**
     * Sets the horizontal position of the current text cursor.
     *
     * <p>The position is relative to the document content area,
     * taking the left margin into account.</p>
     *
     * @param x Horizontal position from the left margin.
     */
    public void setX(float x) {
        x *= pageUnit;
        float position = x < 0 ? (pageWidth - marginRight) : marginLeft;
        xPosition = position + x;
    }

    /**
     * Sets the vertical position of the current text cursor.
     *
     * <p>The position is relative to the document content area,
     * taking the top margin into account.</p>
     *
     * @param y Vertical position from the top margin.
     */
    public void setY(float y) {
        y *= pageUnit;
        float position = y < 0 ? 0 : (pageHeight - marginTop);
        yPosition = position - y;
    }

    /**
     * Returns the current horizontal position of the text cursor.
     *
     * <p>The position is relative to the document content area,
     * taking the left margin into account.</p>
     *
     * @return The horizontal position from the left margin.
     */
    public float getX() {
        return xPosition / pageUnit;
    }

    /**
     * Returns the current vertical position of the text cursor.
     *
     * <p>The position is relative to the document content area,
     * taking the top margin into account.</p>
     *
     * @return The vertical position from the top margin.
     */
    public float getY() {
        return (pageHeight - yPosition - marginTop) / pageUnit;
    }

    /**
     * Moves the current cursor to the next line using the height of the
     * last text element.
     */
    public void lineBreak() {
        lineBreak(currentHeightSize);
    }

    /**
     * Moves the current cursor to the next line using the specified height.
     *
     * <p>The height is always expressed in PDF points, regardless of the
     * unit configured for the document.</p>
     *
     * @param height Line height in PDF points
     */
    public void lineBreak(float height) {
        xPosition = marginLeft;
        yPosition -= height;
    }

    private void evaluateAvailableSpace(float lineHeight) {
        if (xPosition >= pageWidth - marginRight) {
            lineBreak();
        }
        if (yPosition - lineHeight <= pageBreak && !isFooterWriting){
            float x = (xPosition - marginLeft) / pageUnit;
            this.addPage();
            this.setX(x);
        }
    }

    /**
     * Sets the distance from the bottom of the page at which an automatic
     * page break is triggered.
     *
     * <p>The value is measured upward from the bottom edge of the page
     * and is expressed in the unit configured for the document.</p>
     *
     * @param height Distance from the bottom edge of the page
     */
    public void setPageBreak(float height) {
        pageBreak = height * pageUnit;
    }

    /**
     * Loads a font from an external file path and adds it to the document.
     *
     * <p>The font is loaded from the file system.</p>
     *
     * @param fontPath Path to the directory containing the font files
     * @param fontName Name used to reference the font in the document
     */
    public void addFontFromFile(String fontPath, String fontName) {
        try {
            fontManager.addCustomFontsFiles(fontPath, fontName);
        } catch (IOException e) {
            throw new RuntimeException("Font not found " + fontPath);
        }

    }

    /**
     * Loads a font from the application resources and adds it to the document.
     *
     * <p>The font is loaded from the application's classpath resources.</p>
     *
     * @param fontPath Path to the font resource
     * @param fontName Name used to reference the font in the document
     */
    public void addFontFromSource(String fontPath, String fontName) {
        try {
            fontPath = fontPath.startsWith("/") ? fontPath.substring(1) : fontPath;
            fontManager.addCustomFontsSources(fontPath, fontName);
        } catch (IOException e) {
            throw new RuntimeException("Font not found " + fontPath);
        }
    }

    /**
     * Sets the current font.
     *
     * <p>If {@code name} is empty, the currently selected font is reused.</p>
     *
     * @param name Font name, or an empty string to reuse the current font
     */
    public void setFont(String name) {
        setFont(name, "", 0);
    }

    /**
     * Sets the current font and style.
     *
     * <p>If {@code name} is empty, the currently selected font is reused.
     * The style can be empty for regular text, {@code I} for italic,
     * {@code B} for bold, or {@code BI} for bold italic.</p>
     *
     * @param name Font name, or an empty string to reuse the current font
     * @param style Font style: empty, {@code I}, {@code B}, or {@code BI}
     */
    public void setFont(String name, String style) {
        setFont(name, style, 0);
    }

    /**
     * Sets the current font, style, and size.
     *
     * <p>If {@code name} is empty, the currently selected font is reused.
     * The style can be empty for regular text, {@code I} for italic,
     * {@code B} for bold, or {@code BI} for bold italic.</p>
     *
     * @param name Font name, or an empty string to reuse the current font
     * @param style Font style: empty, {@code I}, {@code B}, or {@code BI}
     * @param size Font size in PDF points
     */
    public void setFont(String name, String style, float size) {
        if (size > 0) {
            fontSize = size;
        }

        if (name != null && !name.isBlank()) {
            if (fontManager.isBaseFont(name) || fontManager.isCustomFont(name)) {
                fontName = name;
            } else {
                throw new IllegalArgumentException("Unsupported font family: " + name);
            }
        }
        fontStyle = style;
    }

    private PDFont getFont(String name, String style) {
        if (fontManager.isCustomFont(name) && !fontManager.isLoaded(name, style)) {
            try {
                fontManager.loadCustomFont(document, name, style);
            } catch (IOException e) {
                throw new RuntimeException("It is not possible to load the file " + e);
            }
        }

        return fontManager.getFont(name, style);
    }

    private GBFontInfo saveFontState() {
        return new GBFontInfo(fontName, fontStyle, fontSize, fontColor);
    }

    private void restoreFontState(GBFontInfo fontInfo) {
        fontName = fontInfo.name();
        fontStyle = fontInfo.style();
        fontSize = fontInfo.size();
        fontColor = fontInfo.color();
    }

    /**
     * Sets the font color using a {@link Color} object.
     *
     * <p>All subsequent text rendered using this instance will use the specified color.</p>
     *
     * @param color {@link Color} to apply to the text. Must not be {@code null}.
     */
    public void setFontColor(Color color) {
        fontColor = color;
    }

    /**
     * Sets the font color using RGB color components.
     *
     * <p>All subsequent text rendered using this instance will use the specified color.
     * The RGB values are expected to be in the standard 8-bit range (0 to 255).</p>
     *
     * @param r Red component (0–255).
     * @param g Green component (0–255).
     * @param b Blue component (0–255).
     */
    public void setFontColor(int r, int g, int b) {
        setFontColor(new Color(r, g, b));
    }

    /**
     * Sets the background/fill color for shapes or text blocks.
     *
     * <p>This color is only applied if fill is enabled (e.g., via {@code setRenderFill(true)}).</p>
     *
     * @param color {@link Color} to apply to the text. Must not be {@code null}.
     */
    public void setFillColor(Color color) {
        fillColor = color;
    }

    /**
     * Sets the background/fill color using RGB components.
     *
     * <p>This color is only applied if fill is enabled (e.g., via {@code setFill(true)}).
     * The RGB values are expected to be in the standard 8-bit range (0 to 255).</p>
     *
     * @param r Red component (0–255).
     * @param g Green component (0–255).
     * @param b Blue component (0–255).
     */
    public void setFillColor(int r, int g, int b) {
        setFillColor(new Color(r, g, b));
    }

    /**
     * Sets the color used for drawing lines and shape borders.
     *
     * <p>All subsequent shapes, borders, and lines rendered using this instance
     * will use the specified color. This color is only applied if line rendering is enabled.
     * If render line is set to {@code false}, lines and borders will not be drawn.</p>
     *
     * @param color {@link Color} to apply to stroke operations. Must not be {@code null}.
     */
    public void setDrawColor(Color color) {
        drawColor = color;
    }

    /**
     * Sets the color used for drawing lines and shape borders using RGB components.
     *
     * <p>All subsequent shapes, borders, and lines rendered using this instance
     * will use the specified color. This color is only applied if line rendering is enabled.
     * If render line is set to {@code false}, lines and borders will not be drawn.
     * The RGB values are expected to be in the standard 8-bit range (0 to 255).</p>
     *
     * @param r Red component (0–255).
     * @param g Green component (0–255).
     * @param b Blue component (0–255).
     */
    public void setDrawColor(int r, int g, int b) {
        setDrawColor(new Color(r, g, b));
    }

    private boolean evaluateAlignment(String alignment) {
        return switch (alignment) {
            case "", "L", "R", "J", "C" -> true;
            default -> false;
        };
    }

    private float getXTranslationOffset(String alignment, float x, float textWidth, float boxWidth) {
        return switch (alignment) {
            case "C" -> {
                float tempTx;
                if (textWidth > boxWidth) {
                    tempTx = x - ((textWidth - boxWidth) / 2);
                } else {
                    tempTx = x + ((boxWidth - textWidth) / 2);
                }
                yield tempTx;
            }
            case "R" -> {
                float tempTx;
                if (textWidth > boxWidth) {
                    tempTx = x - (textWidth - boxWidth) - 2;
                } else {
                    tempTx = x + boxWidth - textWidth - 2;
                }
                yield tempTx;
            }
            default -> x + 1;
        };
    }

    /**
     * Prints a cell containing text using default width (extends to right margin), left alignment, no borders, and default next position (0 = to the right).
     *
     * @param content Text content to render inside the cell.
     */
    public void cell(String content) {
        cell(content, 0, "L", "0", 0);
    }

    /**
     * Prints a cell with text content and width, using default left alignment, no borders, and default next position (0 = to the right).
     *
     * @param content Text content to render inside the cell.
     * @param width Width of the cell.
     */
    public void cell(String content, float width) {
        cell(content, width, "L", "0", 0);
    }

    /**
     * Prints a cell with text content, width, and alignment, with no borders and default next cursor position (0 = to the right).
     *
     * @param content Text content to render inside the cell.
     * @param width Width of the cell.
     * @param alignment Text alignment ({@code "L"}, {@code "C"}, or {@code "R"}).
     */
    public void cell(String content, float width, String alignment) {
        cell(content, width, alignment, "0", 0);
    }

    /**
     * Prints a cell with text content, width, alignment, and borders using the
     * default next cursor position (0 = to the right).
     *
     * @param content Text content to render inside the cell.
     * @param width Width of the cell.
     * @param alignment Text alignment ({@code "L"}, {@code "C"}, or {@code "R"}).
     * @param borders Specifies which borders to draw.
     */
    public void cell(String content, float width, String alignment, String borders) {
        cell(content, width, alignment, borders, 0);
    }

    /**
     * Prints a cell with text content, width, alignment, borders, and background color
     * at the current cursor position.
     *
     * <p>The upper-left corner of the cell corresponds to the current cursor position.
     * The text can be aligned within the cell. After calling this method, the cursor position
     * updates according to the specified {@code nextPosition} parameter.</p>
     *
     * @param content Text content to render inside the cell.
     * @param width Width of the cell. If {@code 0}, the cell extends to the right margin of the page.
     * @param alignment Ttext alignment. Acceptable values are {@code "L"} (left), {@code "C"} (center), or {@code "R"} (right).
     * @param borders Specifies which borders to draw. String containing combination of {@code 'T'} (top), {@code 'B'} (bottom), {@code 'L'} (left), {@code 'R'} (right), or {@code "1"} for all sides. Pass empty string {@code ""} or {@code "0"} for no border.
     * @param nextPosition Defines where the cursor is moved after the cell is drawn:
     *                     <ul>
     *                       <li>{@code 0}: To the right of the cell (same line).</li>
     *                       <li>{@code 1}: To the beginning of the next line.</li>
     *                       <li>{@code 2}: Below the current cell.</li>
     *                     </ul>
     */
    public void cell(String content, float width, String alignment, String borders, int nextPosition) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }

        if (!evaluateAlignment(alignment) ) {
            throw new IllegalArgumentException ("Alignment option not valid!");
        }

        if ("J".equalsIgnoreCase(alignment)) {
            throw new IllegalArgumentException ("Justified text alignment is not supported by cell.");
        }

        PDFont font = getFont(fontName, fontStyle);
        float fontSize = this.fontSize;
        GBFont gbFont = new GBFont(font, fontSize, fontColor);

        evaluateAvailableSpace(fontSize + 3);

        float contentWidth = 0;
        try {
            contentWidth = font.getStringWidth(content) / 1000f * fontSize;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Line breaks are not supported by cell. Use multiCell instead.");
        } catch (IOException e) {
            throw new RuntimeException("Content evaluation error!");
        }

        float x = xPosition;
        float y = yPosition;
        width = width == 0 ? pageWidth - x - marginRight : width * pageUnit;

        float ty = y - fontSize;
        float tx = getXTranslationOffset(alignment, x, contentWidth, width);
        currentHeightSize = fontSize + 3;

        boolean[] borderSides = parseBorders(borders);

        float xLeft = getX();
        float xRight = xLeft + width / pageUnit;
        float yTop = getY() + marginTop / pageUnit;
        float yBottom = yTop + currentHeightSize / pageUnit;

        if (renderFill) {
            setRenderLine(false);
            drawRectangle(xLeft, yTop, xRight, yBottom);
            setRenderLine(true);
        }

        if (borderSides[0]) {
            drawLine(xLeft, yTop - 0.08f, xLeft, yBottom + 0.08f);
        }
        if (borderSides[1]) {
            drawLine(xRight, yTop - 0.08f, xRight, yBottom + 0.08f);
        }
        if (borderSides[2]) {
            drawLine(xLeft - 0.08f, yTop, xRight + 0.08f, yTop);
        }
        if (borderSides[3]) {
            drawLine(xLeft - 0.08f, yBottom, xRight + 0.08f, yBottom);
        }

        drawText(content, gbFont, tx, ty, 0);

        switch (nextPosition) {
            case 0 -> xPosition += width;
            case 1 -> lineBreak();
            case 2 -> yPosition -= currentHeightSize;
        }

    }

    /**
     * Prints a multi-line cell using default width (extends to right margin), left alignment, and no borders.
     *
     * <p>Positions the cursor automatically at the beginning of the next line below the multi-cell once finished.</p>
     *
     * @param content Text content to render.
     */
    public void multiCell(String content) {
        multiCell(content, 0, "J", "");
    }

    /**
     * Prints a multi-line cell with automatic line wrapping using default left alignment and no borders.
     *
     * <p>Positions the cursor automatically at the beginning of the next line below the multi-cell once finished.</p>
     *
     * @param content Text content to render.
     * @param width Width of the cell.
     */
    public void multiCell(String content, float width) {
        multiCell(content, width, "J", "");
    }

    /**
     * Prints a multi-line cell with automatic line wrapping, alignment, and no borders.
     *
     * <p>Positions the cursor automatically at the beginning of the next line below the multi-cell once finished.</p>
     *
     * @param content Text content to render.
     * @param width Width of the cell.
     * @param alignment Text alignment ({@code "L"}, {@code "C"}, {@code "R"}, or {@code "J"}).
     */
    public void multiCell(String content, float width, String alignment) {
        this.multiCell(content, width, alignment, "");
    }

    /**
     * Prints a multi-line cell with automatic line wrapping.
     *
     * <p>Unlike {@code cell()}, this method allows printing text across multiple lines.
     * Line breaks are performed automatically when the text reaches the right edge of the cell,
     * or whenever explicit line feed characters ({@code \n}) are encountered in the content.
     * Once completed, the cursor automatically positions itself at the beginning of the next line below the multi-cell.</p>
     *
     * @param content Text content to render. Can contain explicit line breaks ({@code \n}).
     * @param width Width of the cell. If {@code 0}, the cell extends to the right margin of the page.
     * @param alignment Text alignment within the cell. Acceptable values are {@code "L"} (left), {@code "C"} (center), {@code "R"} (right), or {@code "J"} (justified).
     * @param borders Specifies which borders to draw. String containing combination of {@code 'T'} (top), {@code 'B'} (bottom), {@code 'L'} (left), {@code 'R'} (right), or {@code "1"} for all sides. Pass empty string {@code ""} or {@code "0"} for no border.
     */
    public void multiCell(String content, float width, String alignment, String borders) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }

        evaluateAvailableSpace(fontSize + 3);
        if (!evaluateAlignment (alignment) ) {
            throw new IllegalArgumentException ("Alignment option not valid!");
        }

        boolean[] borderSides = parseBorders(borders);

        PDFont font = getFont(fontName, fontStyle);
        float fontSize = this.fontSize;
        GBFont gbFont = new GBFont(font, fontSize, fontColor);

        float x = xPosition;
        width = width == 0 ? pageWidth - x - marginRight : width * pageUnit;

        String[] paragraphs = content.split("\\R");
        int totalParagraphs = paragraphs.length;
        boolean isFirstParagraph = true;
        boolean isLastParagraph = false;

        for (int i = 0; i < totalParagraphs; i++) {
            if (i == totalParagraphs - 1) isLastParagraph = true;

            String paragraph = paragraphs[i];
            List<String> lines = splitIntoLines(paragraph, width, font);
            int totalLines = lines.size();
            boolean isLastLine = false;
            for (int j = 0; j < totalLines; j++) {
                if (j == totalLines - 1 ) isLastLine = true;

                String line = lines.get(j);
                float y = yPosition;

                float lineWidth = 0;
                try {
                    lineWidth = font.getStringWidth(line) / 1000f * fontSize;
                } catch (IOException e) {
                    throw new RuntimeException("Content evaluation error!");
                }

                float ty = y - fontSize;
                float tx = getXTranslationOffset(alignment, x, lineWidth, width);
                float tempH = fontSize + 3;

                float xLeft = getX();
                float xRight = xLeft + width / pageUnit;
                float yTop = getY() + marginTop / pageUnit;
                float yBottom = yTop + tempH / pageUnit;

                if (renderFill) {
                    setRenderLine(false);
                    drawRectangle(xLeft, yTop, xRight, yBottom);
                    setRenderLine(true);
                }

                if (borderSides[0]) {
                    drawLine(xLeft, yTop - 0.08f, xLeft, yBottom + 0.08f);
                }
                if (borderSides[1]) {
                    drawLine(xRight, yTop - 0.08f, xRight, yBottom + 0.08f);
                }
                if (borderSides[2] && isFirstParagraph && j == 0) {
                    drawLine(xLeft - 0.08f, yTop, xRight + 0.08f, yTop);
                }
                if (borderSides[3] && isLastParagraph && isLastLine) {
                    drawLine(xLeft - 0.08f, yBottom, xRight + 0.08f, yBottom);
                }

                float wordSpace = 0;
                if ("J".equalsIgnoreCase(alignment) && !isLastLine) {
                    float extraWidth = width - 2 - lineWidth;
                    int length = line.split(" ").length;
                    if (length > 1) length -= 1;
                    wordSpace = extraWidth / length;
                }

                drawText(line, gbFont, tx, ty, wordSpace);
                yPosition -= tempH;
                evaluateAvailableSpace(tempH);
            }
            isFirstParagraph = false;
        }

        currentHeightSize = fontSize + 3;
        lineBreak(0);
    }

    /**
     * Renders text inline starting from the current cursor position.
     *
     * <p>The text flows naturally from the current coordinates, automatically wrapping
     * to the next line at the left margin if it reaches the right margin of the page.
     * Upon completion, the cursor is left positioned immediately after the rendered text.</p>
     *
     * @param content Text content to render inline.
     */
    public void inlineText(String content) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }
        evaluateAvailableSpace(fontSize + 3);
        PDFont font = getFont(fontName, fontStyle);
        float fontSize = this.fontSize;
        GBFont gbFont = new GBFont(font, fontSize, fontColor);

        float tempH = fontSize + 3;
        float x = xPosition;
        float y = yPosition;
        float ty = y - fontSize;
        float width = pageWidth - x - marginRight;

        String[] paragraphs = content.split("\\R");
        int totalParagraphs = paragraphs.length;
        int last = totalParagraphs - 1;
        try {
            float spaceWidth = font.getStringWidth(" ") / 1000f * fontSize;

            for (int i = 0; i < totalParagraphs; i++) {
                String paragraph = paragraphs[i];
                StringBuilder lineBuilder = new StringBuilder();
                float currentLineWidth = 0;
                String[] words = paragraph.split(" ", -1);
                for (String word : words) {
                    if (word == null || word.isBlank()) {
                        word = "";
                    }
                    float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
                    float extraWidth = !lineBuilder.isEmpty() ? spaceWidth : 0;
                    if (wordWidth <= width) {

                        if (currentLineWidth + extraWidth + wordWidth <= width) {
                            if (!lineBuilder.isEmpty()) {
                                lineBuilder.append(" ");
                            }
                            lineBuilder.append(word);
                            currentLineWidth += extraWidth + wordWidth;
                        } else {
                            drawText(lineBuilder.toString(), gbFont, x, ty, 0);
                            lineBreak(tempH);
                            evaluateAvailableSpace(tempH);
                            x = xPosition;
                            y = yPosition;
                            ty = y - fontSize;
                            width = pageWidth - x - marginRight;
                            lineBuilder.setLength(0);
                            lineBuilder.append(word);
                            currentLineWidth = wordWidth;
                        }
                    }
                }
                if (!lineBuilder.isEmpty()) {
                    drawText(lineBuilder.toString(), gbFont, x, ty, 0);
                    evaluateAvailableSpace(tempH);
                    if (i == totalParagraphs - 1) {
                        xPosition += currentLineWidth;
                    } else {
                        lineBreak(tempH);
                        x = xPosition;
                        y = yPosition;
                        ty = y - fontSize;
                        width = pageWidth - x - marginRight;
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    private void drawText(String text, GBFont font, float tx, float ty, float wordSpacing) {
        try {
            writer.saveGraphicsState();
            writer.beginText();
            writer.setFont(font.name(), font.size());
            writer.setNonStrokingColor(font.color());
            writer.newLineAtOffset(tx, ty);
            writer.setWordSpacing(wordSpacing);
            writer.showText(text);
            writer.endText();
            writer.restoreGraphicsState();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean[] parseBorders(String borders) {
        char[] borderChars = borders.toCharArray();
        boolean[] borderSides = new boolean[4];

        borderSides[0] = false;
        borderSides[1] = false;
        borderSides[2] = false;
        borderSides[3] = false;

        for (char c : borderChars) {
            switch(c) {
                case '1':
                    borderSides[0] = true;
                    borderSides[1] = true;
                    borderSides[2] = true;
                    borderSides[3] = true;
                    break;
                case 'L':
                    borderSides[0] = true;
                    break;
                case 'R':
                    borderSides[1] = true;
                    break;
                case 'T':
                    borderSides[2] = true;
                    break;
                case 'B':
                    borderSides[3] = true;
                    break;
            }
        }
        return borderSides;
    }

    private List<String> splitIntoLines(String content, float width, PDFont font) {
        float spaceWidth = 0;
        try {
            spaceWidth = font.getStringWidth(" ") / 1000f * fontSize;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();
        float currentLineWidth = 0;
        String[] words = content.split(" ");
        for (String word : words) {
            if (word == null || word.isBlank()) {
                word = "\u00a0";
            }
            try {
                float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
                float extraWidth = !lineBuilder.isEmpty() ? spaceWidth : 0;
                if (wordWidth <= width) {

                    if (currentLineWidth + extraWidth + wordWidth < width) {
                        if (!lineBuilder.isEmpty()) {
                            lineBuilder.append(" ");
                        }
                        lineBuilder.append(word);
                        currentLineWidth += extraWidth + wordWidth;
                    } else {
                        lines.add(lineBuilder.toString());
                        lineBuilder.setLength(0);
                        currentLineWidth = wordWidth;
                        lineBuilder.append(word);
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
        if (!lineBuilder.isEmpty()) {
            lines.add(lineBuilder.toString());
            lineBuilder.setLength(0);
        }

        return lines;
    }

    /**
     * Draws a straight line between two specified coordinate points.
     *
     * <p>Coordinates are measured relative to the top-left edge of the page.
     * This operation uses the current draw color and line rendering settings.
     * If render line is set to {@code false}, the line will not be drawn.</p>
     *
     * @param startX Starting horizontal position from the left edge of the page.
     * @param startY Starting vertical position from the top edge of the page.
     * @param endX Ending horizontal position from the left edge of the page.
     * @param endY Ending vertical position from the top edge of the page.
     */
    public void drawLine(float startX, float startY, float endX, float endY) {
        startX *= pageUnit;
        startY = pageHeight - (startY * pageUnit);
        endX *= pageUnit;
        endY = pageHeight - (endY * pageUnit);

        try {
            writer.saveGraphicsState();
            writer.setLineWidth(.5f);
            writer.moveTo(startX, startY);
            writer.lineTo(endX, endY);
            if (renderLine) {
                writer.setStrokingColor(drawColor);
                writer.stroke();
            }
            writer.restoreGraphicsState();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Draws a rectangle defined by its upper-left and lower-right coordinates.
     *
     * <p>Coordinates are measured relative to the top-left edge of the page.
     * The border is rendered using the current draw color if line rendering is enabled.
     * The interior is filled using the current fill color if fill rendering is enabled.</p>
     *
     * @param startX Horizontal position of the upper-left corner from the left edge of the page.
     * @param startY Vertical position of the upper-left corner from the top edge of the page.
     * @param endX Horizontal position of the lower-right corner from the left edge of the page.
     * @param endY Vertical position of the lower-right corner from the top edge of the page.
     */
    public void drawRectangle(float startX, float startY, float endX, float endY) {
        startX *= pageUnit;
        startY = (pageHeight) - (startY * pageUnit);
        float width = (endX * pageUnit) - startX;
        float height = ((pageHeight) - (endY * pageUnit)) - startY;

        try {
            writer.saveGraphicsState();
            writer.setLineWidth(.5f);
            writer.addRect(startX, startY, width, height);

            if (renderLine && renderFill) {
                writer.setStrokingColor(drawColor);
                writer.setNonStrokingColor(fillColor);
                writer.fillAndStroke();
            } else if (renderLine) {
                writer.setStrokingColor(drawColor);
                writer.stroke();
            } else if (renderFill) {
                writer.setNonStrokingColor(fillColor);
                writer.fill();
            }
            writer.restoreGraphicsState();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Draws an image from a file path at the specified coordinates with scaling.
     *
     * <p>Coordinates define the upper-left corner relative to the top-left edge of the page.
     * The image dimensions are adjusted according to the provided scale factor.</p>
     *
     * @param imagePath File path to the image.
     * @param x Horizontal position from the left edge of the page.
     * @param y Vertical position from the top edge of the page.
     * @param scale Scaling factor percentage from 1 to 100 applied to the original image dimensions (a value of 0 is treated as 100%).
     */
    public void drawImage(String imagePath, float x, float y, float scale)  {
        try {
            PDImageXObject pdImage = null;
            if (images.containsKey(imagePath)) {
                pdImage = images.get(imagePath);
            } else {
                pdImage = PDImageXObject.createFromFile(imagePath, document);
                images.put(imagePath, pdImage);
            }

            if (scale == 0) {
                scale = 1;
            } else {
                scale /= 100;
            }

            float height = pdImage.getHeight() * scale;
            float width = pdImage.getWidth() * scale;

            x *= pageUnit;
            y = pageHeight - (y * pageUnit) - height;

            writer.drawImage(pdImage, x, y, width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Draws an image from a byte array at the specified coordinates with scaling.
     *
     * <p>Coordinates define the upper-left corner relative to the top-left edge of the page.
     * The image dimensions are adjusted according to the provided scale factor.</p>
     *
     * @param imageData Raw image data as a byte array.
     * @param x Horizontal position from the left edge of the page.
     * @param y Vertical position from the top edge of the page.
     * @param scale Scaling factor percentage from 1 to 100 applied to the original image dimensions (a value of 0 is treated as 100%).
     */
    public void drawImage(byte[] imageData, float x, float y, float scale)  {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(imageData);
            String imageKey = HexFormat.of().formatHex(hash);

            PDImageXObject pdImage = null;
            if (images.containsKey(imageKey)) {
                pdImage = images.get(imageKey);
            } else {
                pdImage = PDImageXObject.createFromByteArray(document, imageData, imageKey);
                images.put(imageKey, pdImage);
            }

            if (scale == 0) {
                scale = 1;
            } else {
                scale /= 100;
            }

            float height = pdImage.getHeight() * scale;
            float width = pdImage.getWidth() * scale;

            x *= pageUnit;
            y = pageHeight - (y * pageUnit) - height;

            writer.drawImage(pdImage, x, y, width, height);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Enables or disables line and border rendering for subsequent draw operations.
     *
     * <p>When set to {@code false}, lines, shape outlines, and borders will not be drawn.</p>
     *
     * @param renderLine {@code true} to enable line drawing; {@code false} to disable it.
     */
    public void setRenderLine(boolean renderLine) {
        this.renderLine = renderLine;
    }

    /**
     * Enables or disables background and interior fill rendering for subsequent draw operations.
     *
     * <p>When set to {@code false}, shape interiors and background fills will not be drawn.</p>
     *
     * @param renderFill {@code true} to enable fill operations; {@code false} to disable them.
     */
    public void setRenderFill(boolean renderFill) {
        this.renderFill = renderFill;
    }

    /**
     * Hook invoked immediately after a new page is created, before any page content is rendered.
     *
     * <p>Override this method to render custom header content (such as logos or titles)
     * on each new page.</p>
     */
    public void header() {}

    /**
     * Hook invoked when the current page is completed, including the last page when the document is closed.
     *
     * <p>Override this method to render custom footer content (such as page numbers or footers)
     * at the bottom of each page.</p>
     */
    public void footer() {}


}
