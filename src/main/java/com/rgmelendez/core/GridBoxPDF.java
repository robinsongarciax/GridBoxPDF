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

public class GridBoxPDF {

    private String fileName;

    private PDDocument document;
    private PDPageContentStream writer;
    private PDRectangle pageFormat = PDRectangle.LETTER;
    private final GridBoxFontManager fontManager = new GridBoxFontManager();

    public enum PageFormat {
        LETTER, A4, LEGAL, A5
    }

    private String pageOrientation;

    public enum PageUnit {
        CENTIMETERS(PDFConfig.PAGEUNIT_CM),
        MILIMETERS(PDFConfig.PAGEUNIT_MM),
        INCHES(PDFConfig.PAGEUNIT_INCH),
        POINTS(PDFConfig.PAGEUNIT_PTS);

        private final float value;

        private PageUnit(float value) {
            this.value = value;
        }

        public float getValue() {
            return value;
        }
    }

    public enum PageOrientation {
        LANDSCAPE,
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
    private float marginBottom = 0;
    private float pageBreak = 10 * pageUnit;

    private boolean renderLine = true;
    private boolean renderFill = false;

    // colors
    private Color fontColor = PDFConfig.BLACK;
    private Color drawColor = PDFConfig.BLACK;
    private Color fillColor = PDFConfig.WHITE;

    // images
    private final Map<String, PDImageXObject> images = new HashMap<>();

    public GridBoxPDF() {
    }

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
    }

    public void create() {
        document = new PDDocument();
        initialize();
    }

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

    public void addPage() {
        if (pageNumber > 0 && !isFooterWriting && !isHeaderWriting) {
            isFooterWriting = true;
            this.setXY(0, -(pageBreak - 4)/pageUnit);
            footer();
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
            header();
            this.lineBreak();
            isHeaderWriting = false;
        }
    }

    private void printFooter() {
        if (pageNumber > 0) {
            isFooterWriting = true;
            this.setXY(0, -(pageBreak - 4)/pageUnit);
            footer();
            isFooterWriting = false;
        }
    }

    public int getPageNumber() {
        return pageNumber;
    }

    private void initialize() {
        pageHeight = pageFormat.getHeight();
        pageWidth = pageFormat.getWidth();
        setDefaultMargins();
        setFont("HELVETICA");
    }

    public void setDefaultMargins() {
        setMargins(10, 10, 10);
    }

    public void setMargins(float left, float right, float top) {
        marginLeft = left * pageUnit;
        marginRight = right * pageUnit;
        marginTop = top * pageUnit;
    }

    public void setXY(float x, float y) {
        setX(x);
        setY(y);
    }

    public void setX(float x) {
        x *= pageUnit;
        float position = x < 0 ? (pageWidth - marginRight) : marginLeft;
        xPosition = position + x;
    }

    public void setY(float y) {
        y *= pageUnit;
        float position = y < 0 ? marginBottom : (pageHeight - marginTop);
        yPosition = position - y;
    }

    public float getX() {
        return xPosition / pageUnit;
    }

    public float getY() {
        return (pageHeight - yPosition - marginTop) / pageUnit;
    }

    public void lineBreak() {
        lineBreak(currentHeightSize);
    }

    public void lineBreak(float height) {
        xPosition = marginLeft;
        yPosition -= height;
    }

    private void evaluateAvailableSpace() {
        if (xPosition >= pageWidth - marginRight) {
            lineBreak();
        }
        if (yPosition <= pageBreak && !isFooterWriting){
            float x = (xPosition - marginLeft) / pageUnit;
            this.addPage();
            this.setX(x);
        } else if (yPosition <= marginBottom) {
            float x = (xPosition - marginLeft) / pageUnit;
            this.addPage();
            this.setX(x);
        }
    }

    public void setPageBreak(float height) {
        pageBreak = height * pageUnit;
    }

    public void addFontFromFile(String fontPath, String fontName) {
        try {
            fontManager.addCustomFontsFiles(fontPath, fontName);
        } catch (IOException e) {
            throw new RuntimeException("Font not found " + fontPath);
        }

    }

    public void addFontFromSource(String fontPath, String fontName) {
        try {
            fontPath = fontPath.startsWith("/") ? fontPath.substring(1) : fontPath;
            fontManager.addCustomFontsSources(fontPath, fontName);
        } catch (IOException e) {
            throw new RuntimeException("Font not found " + fontPath);
        }
    }

    public void setFont(String name) {
        setFont(name, "", 0);
    }

    public void setFont(String name, String style) {
        setFont(name, style, 0);
    }

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

    public void setFontColor(Color color) {
        fontColor = color;
    }

    public void setFontColor(int r, int g, int b) {
        setFontColor(new Color(r, g, b));
    }

    public void setFillColor(Color color) {
        fillColor = color;
    }

    public void setFillColor(int r, int g, int b) {
        setFillColor(new Color(r, g, b));
    }

    public void setDrawColor(Color color) {
        drawColor = color;
    }

    public void setDrawColor(int r, int g, int b) {
        setDrawColor(new Color(r, g, b));
    }

    private boolean evaluateAlignment(String alignment) {
        return switch (alignment) {
            case "L", "R", "J", "C" -> true;
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

    public void cell(String content) {
        cell(content, 0, "L", "0", 0);
    }

    public void cell(String content, float width) {
        cell(content, width, "L", "0", 0);
    }

    public void cell(String content, float width, String alignment) {
        cell(content, width, alignment, "0", 0);
    }

    public void cell(String content, float width, String alignment, String borders) {
        cell(content, width, alignment, borders, 0);
    }

    public void cell(String content, float width, String alignment, String borders, int nextPosition) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }

        if (!evaluateAlignment(alignment) ) {
            throw new IllegalArgumentException ("Alignment option not valid!");
        }

        PDFont font = getFont(fontName, fontStyle);
        float fontSize = this.fontSize;
        GBFont gbFont = new GBFont(font, fontSize, fontColor);

        evaluateAvailableSpace();

        float contentWidth = 0;
        try {
            contentWidth = font.getStringWidth(content) / 1000f * fontSize;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Line breaks are not supported by Cell. Use MultiCell instead.");
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

        drawText(content, gbFont, tx, ty);

        switch (nextPosition) {
            case 0 -> xPosition += width;
            case 1 -> lineBreak();
            case 2 -> yPosition -= currentHeightSize;
        }

    }

    public void multiCell(String content) {
        multiCell(content, 0, "J", "");
    }

    public void multiCell(String content, float width) {
        multiCell(content, width, "J", "");
    }

    public void multiCell(String content, float width, String alignment) {
        this.multiCell(content, width, alignment, "");
    }

    public void multiCell(String content, float width, String alignment, String borders) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }

        evaluateAvailableSpace();
        if (!evaluateAlignment (alignment) ) {
            throw new IllegalArgumentException ("Alignment option not valid!");
        }

        PDFont font = getFont(fontName, fontStyle);
        float fontSize = this.fontSize;
        GBFont gbFont = new GBFont(font, fontSize, fontColor);

        float x = xPosition;
        width = width == 0 ? pageWidth - x - marginRight : width * pageUnit;

        List<String> lines = splitIntoLines(content, width, font);

        int totalLines = lines.size();
        int last = totalLines - 1;
        for (int i = 0; i < totalLines; i++) {
            String line = lines.get (i);
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

            boolean[] borderSides = parseBorders(borders);

            float xLeft = getX();
            float xRight = xLeft + width / pageUnit;
            float yTop = getY() + marginTop / pageUnit;
            float yBottom = yTop + tempH / pageUnit;

            if (borderSides[0]) {
                drawLine(xLeft, yTop - 0.08f, xLeft, yBottom + 0.08f);
            }
            if (borderSides[1]) {
                drawLine(xRight, yTop - 0.08f, xRight, yBottom + 0.08f);
            }
            if (borderSides[2] && i == 0) {
                drawLine(xLeft - 0.08f, yTop, xRight + 0.08f, yTop);
            }
            if (borderSides[3] && i == last) {
                drawLine(xLeft - 0.08f, yBottom, xRight + 0.08f, yBottom);
            }

            drawText(line, gbFont, tx, ty);
            yPosition -= tempH;
            evaluateAvailableSpace();
        }

        currentHeightSize = fontSize + 3;
        lineBreak(0);
    }

    public void inlineText(String content) {
        if (content == null || content.isBlank()) {
            content = "\u00a0";
        }
        evaluateAvailableSpace();
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
                        word = "\u00a0";
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
                            drawText(lineBuilder.toString(), gbFont, x, ty);
                            lineBreak(tempH);
                            evaluateAvailableSpace();
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
                    drawText(lineBuilder.toString(), gbFont, x, ty);
                    evaluateAvailableSpace();
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

    private void drawText(String text, GBFont font, float tx, float ty) {
        try {
            writer.saveGraphicsState();
            writer.beginText();
            writer.setFont(font.name(), font.size());
            writer.setNonStrokingColor(font.color());
            writer.newLineAtOffset(tx, ty);
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
        String[] paragraphs = content.split("\\R");
        for (String paragraph : paragraphs) {
            StringBuilder lineBuilder = new StringBuilder();
            float currentLineWidth = 0;
            String[] words = paragraph.split(" ");
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
        }

        return lines;
    }

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

    public void drawRoundedRectangle(float startX, float startY, float endX, float endY, float ratio) {
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



    public void setRenderLine(boolean renderLine) {
        this.renderLine = renderLine;
    }

    public void setRenderFill(boolean renderFill) {
        this.renderFill = renderFill;
    }

    /**
     * Called when a new page is created, before its content is written.
     * Override to add custom header content.
     */
    public void header() {}

    /**
     * Called when a page is finished, or when the document is closed
     * if the document contains only one page.
     * Override to add custom footer content.
     */
    public void footer() {}


}
