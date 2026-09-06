package com.rgmelendez.core;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.*;

public record GBFont(PDFont name, float size, Color color) {
}
