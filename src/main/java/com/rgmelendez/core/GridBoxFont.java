package com.rgmelendez.core;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GridBoxFont {

    private final Map<String, FontFamily> customFontsStreams = new HashMap<>();
    private final Map<String, PDFont> baseFontsCache = new HashMap<>();
    private final Map<String, PDFont> customFontsCache = new HashMap<>();

    public GridBoxFont() {
        addDefaultCustomFonts();
    }

    public PDFont getFont(String familyName, String style) {
        String key = getKey(familyName, style);

        if (customFontsCache.containsKey(key)) {
            return customFontsCache.get(key);
        }

        if (!baseFontsCache.containsKey(key)) {
            PDFont newFont = getBaseFont(familyName, style);
            baseFontsCache.put(key, newFont);
        }
        return baseFontsCache.get(key);
    }

    public PDFont getBaseFont(String familyName, String style) {

        String family = normalize(familyName);

        boolean bold = hasStyle(style, 'B');
        boolean italic = hasStyle(style, 'I');

        return switch (family) {

            case "HELVETICA" -> {
                if (bold && italic)
                    yield new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);

                if (bold)
                    yield new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                if (italic)
                    yield new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                yield new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }

            case "COURIER" -> {
                if (bold && italic)
                    yield new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE);

                if (bold)
                    yield new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

                if (italic)
                    yield new PDType1Font(Standard14Fonts.FontName.COURIER_OBLIQUE);

                yield new PDType1Font(Standard14Fonts.FontName.COURIER);
            }

            case "TIMES" -> {
                if (bold && italic)
                    yield new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);

                if (bold)
                    yield new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

                if (italic)
                    yield new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);

                yield new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            }

            default -> throw new IllegalArgumentException("Unsupported font family: " + familyName);
        };
    }

    public void addCustomFontsFiles (String path, String fontFamily) throws IOException {
        Path directory = Path.of(path);

        Path regular = directory.resolve(fontFamily + "-Regular.ttf");
        Path bold = directory.resolve(fontFamily + "-Bold.ttf");
        Path italic = directory.resolve(fontFamily + "-Italic.ttf");
        Path boldItalic = directory.resolve(fontFamily + "-BoldItalic.ttf");

        validateFontFile(regular);
        validateFontFile(bold);
        validateFontFile(italic);
        validateFontFile(boldItalic);

        customFontsStreams.put(normalize(fontFamily), new FontFamily(Files.newInputStream(regular),
                                                                   Files.newInputStream(bold),
                                                                   Files.newInputStream(italic),
                                                                   Files.newInputStream(boldItalic)));
    }

    public void addCustomFontsSources (String resourcePath, String fontFamily) throws IOException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        if (contextClassLoader.getResource(resourcePath + fontFamily + "-Regular.ttf") == null) {
            throw new RuntimeException("Font not found in resources: " + resourcePath);
        }

        String regularPath = resourcePath + fontFamily + "-Regular.ttf";
        String boldPath = resourcePath + fontFamily + "-Bold.ttf";
        String italicPath = resourcePath + fontFamily + "-Italic.ttf";
        String boldItalicPath = resourcePath + fontFamily + "-BoldItalic.ttf";

        try (InputStream regularStream = contextClassLoader.getResourceAsStream(regularPath);
             InputStream boldStream = contextClassLoader.getResourceAsStream(boldPath);
             InputStream italicStream = contextClassLoader.getResourceAsStream(italicPath);
             InputStream boldItalicStream = contextClassLoader.getResourceAsStream(boldItalicPath)) {

            customFontsStreams.put(normalize(fontFamily),
                    new FontFamily(regularStream, boldStream, italicStream, boldItalicStream));
        } catch (Exception e) {
            throw new RuntimeException("Error loading font: " + e.getMessage());
        }
    }

    public void loadCustomFont(PDDocument document, String familyName, String style) throws IOException {
        FontFamily fontFamily = customFontsStreams.get(normalize(familyName));
        try (InputStream fontStream = fontFamily.getStream(style)) {
            PDType0Font font = PDType0Font.load(document, fontStream);
            customFontsCache.put(getKey(familyName, style), font);
        }
    }

    public boolean isBaseFont(String familyName) {
        String family = normalize(familyName);

        return switch (family) {
            case "HELVETICA", "COURIER", "TIMES" -> true;
            default -> false;
        };
    }

    public boolean isCustomFont(String familyName) {
        String family = normalize(familyName);
        return customFontsStreams.containsKey(family);
    }

    public boolean isLoaded(String familyName, String style) {
        return customFontsCache.containsKey(getKey(familyName, style));
    }

    private void addDefaultCustomFonts() {
        if (getClass().getResource("/fonts/liberation_sans/") == null) {
            throw new RuntimeException("Font not found in resources: /fonts/liberation_sans/");
        }

        registerBuiltInFont("/fonts/liberation_sans/", "LiberationSans");
    }

    private void registerBuiltInFont (String resourcePath, String fontFamily)  {
        String regularPath = resourcePath + fontFamily + "-Regular.ttf";
        String boldPath = resourcePath + fontFamily + "-Bold.ttf";
        String italicPath = resourcePath + fontFamily + "-Italic.ttf";
        String boldItalicPath = resourcePath + fontFamily + "-BoldItalic.ttf";

        try (InputStream regularStream = getClass().getResourceAsStream(regularPath);
             InputStream boldStream = getClass().getResourceAsStream(boldPath);
             InputStream italicStream = getClass().getResourceAsStream(italicPath);
             InputStream boldItalicStream = getClass().getResourceAsStream(boldItalicPath)) {

            customFontsStreams.put(normalize(fontFamily),
                    new FontFamily(regularStream, boldStream, italicStream, boldItalicStream));
        } catch (Exception e) {
            throw new RuntimeException("Error loading font: " + e.getMessage());
        }

    }

    private String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Font family cannot be null");
        }

        return value.trim().toUpperCase();
    }

    private void validateFontFile(Path path) throws FileNotFoundException {
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("Font file not found: " + path.getFileName());
        }
    }

    private boolean hasStyle(String style, char value) {
        return style != null && style.toUpperCase().indexOf(value) >= 0;
    }

    private String getKey(String name, String style) {
        boolean isBold = hasStyle(style, 'B');
        boolean isItalic = hasStyle(style, 'I');
        if (isBold && isItalic) {
            return name.toUpperCase() + ":" + "BI";
        }

        if (isBold)
            return name.toUpperCase() + ":" + "B";

        if (isItalic)
            return name.toUpperCase() + ":" + "I";

        return name.toUpperCase();
    }

    private static class FontFamily {

        public enum Style {
            REGULAR, BOLD, ITALIC, BOLD_ITALIC
        }

        private final Map<Style, byte[]> fontData = new EnumMap<>(Style.class);

        public FontFamily(InputStream regular, InputStream bold, InputStream italic, InputStream boldItalic) {
            storeFontStream(Style.REGULAR, regular);
            storeFontStream(Style.BOLD, bold);
            storeFontStream(Style.ITALIC, italic);
            storeFontStream(Style.BOLD_ITALIC, boldItalic);
        }

        private void storeFontStream(Style style, InputStream stream) {
            if (stream == null) throw new RuntimeException("Error reading font data for style: " + style);

            try (InputStream is = stream) {
                this.fontData.put(style, is.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException("Error reading font data for style: " + style, e);
            }
        }

        /**
         * Devuelve un Stream NUEVO cada vez que se solicita la fuente.
         */
        public InputStream getStream(String style) {

            boolean hasBold = style != null && style.toUpperCase().contains("B");
            boolean hasItalic = style != null && style.toUpperCase().contains("I");

            Style styleOption ;
            if (hasBold && hasItalic)
                styleOption = Style.BOLD_ITALIC;

            else if (hasBold)
                styleOption = Style.BOLD;

            else if (hasItalic)
                styleOption = Style.ITALIC;

            else styleOption = Style.REGULAR;


            byte[] bytes = fontData.get(styleOption);
            if (bytes == null) {
                // Fallback a REGULAR si el estilo solicitado no existe
                bytes = fontData.get(Style.REGULAR);
            }

            if (bytes == null) {
                throw new IllegalStateException("No font data available for requested style.");
            }

            return new ByteArrayInputStream(bytes);
        }
    }

}
