# GridBoxPDF

**A lightweight Java PDF generation library built on top of Apache PDFBox.**

GridBoxPDF provides a simple, document-oriented API for generating PDF documents without requiring direct interaction with the low-level PDFBox API.

It keeps the flexibility of PDFBox while providing convenient features such as cells, text flow, images, fonts, colors, headers, footers, automatic page breaks, and physical units.

## Features

* Built on top of [Apache PDFBox](https://pdfbox.apache.org/)
* Simple and lightweight API
* Top-to-bottom, left-to-right document layout
* Millimeters as the default unit
* Support for millimeters, centimeters, inches, and points
* Automatic page breaks
* Manual page breaks when needed
* Text cells with alignment and borders
* Multi-line text
* Inline text
* Absolute image positioning
* Custom fonts
* Built-in fonts
* Font and drawing colors
* Headers and footers
* Page numbering
* Manual X/Y positioning
* Letter portrait pages by default
* Configurable page margins

## Quick Start

```java
GridBoxPDF pdf = new GridBoxPDF();

pdf.addPage();
pdf.cell("Hello GridBoxPDF!", 40);

pdf.close("hello.pdf");
```

That's it.

GridBoxPDF provides sensible defaults so a document can be created without configuring fonts, page size, margins, or measurement units.

## Default Configuration

A new `GridBoxPDF` document uses the following defaults:

| Property         | Default                      |
| ---------------- | ---------------------------- |
| Page size        | Letter                       |
| Orientation      | Portrait                     |
| Margins          | 10 mm                        |
| Unit             | Millimeters                  |
| Font             | Helvetica                    |
| Font size        | 12                           |
| Layout direction | Top-to-bottom, left-to-right |
| Page breaks      | Automatic                    |

These defaults can be changed when more control is required.

## Coordinate System

GridBoxPDF uses a top-to-bottom, left-to-right coordinate system.

For text and document layout, coordinates are relative to the content area defined by the page margins.

For example, with the default 10 mm margins:

```text
Page
┌──────────────────────────────────────┐
│  10 mm                               │
│    ┌──────────────────────────────┐  │
│    │ (0,0)                        │  │
│    │                              │  │
│    │       Content area           │  │
│    │                              │  │
│    └──────────────────────────────┘  │
│                                      │
└──────────────────────────────────────┘
```

Images use absolute coordinates relative to the physical page.

This makes it possible to combine normal document flow with precise positioning of elements such as logos, signatures, or other graphics.

## Units

Millimeters are used by default.

Other supported units include:

* Millimeters
* Centimeters
* Inches
* Points

This allows document dimensions and positioning to be expressed using physical measurements rather than PDF points.

## Fonts

GridBoxPDF provides several fonts out of the box:

* Helvetica
* Times
* Courier
* LiberationSans

They can be used directly:

```java
pdf.setFont("Times", "B", 16);
```

Additional fonts can be registered before use:

```java
pdf.addFontFromSource("fonts/roboto/", "Roboto");
pdf.setFont("Roboto", "B", 12);
```

## Text

Text can be written using cells:

```java
pdf.cell("Hello GridBoxPDF!", 40);
```

Alignment and borders can be specified when needed:

```java
pdf.cell("Hello GridBoxPDF!", 40, "C", "1");
```

For multi-line text:

```java
pdf.multiCell("This is a long text that can span multiple lines.", 0, "L", "1");
```

Inline text can be used when text should continue on the current line:

```java
pdf.inlineText("Hello ");
pdf.inlineText("GridBoxPDF!");
```

## Tables

Tables can be created using cells and line breaks without requiring a separate table API:

```java
pdf.cell("Country", 40, "C", "1");
pdf.cell("Capital", 40, "C", "1");
pdf.lineBreak();

pdf.cell("France", 40, "L", "LR");
pdf.cell("Paris", 40, "L", "LR");
pdf.lineBreak();
```

This keeps table generation consistent with the rest of the GridBoxPDF API.

## Headers and Footers

Headers and footers can be defined by extending `GridBoxPDF`:

```java
public class MyDocument extends GridBoxPDF {

    public void header() {
        setFont("Times", "B", 16);
        cell("My Document", 0, "C", "1");
    }

    public void footer() {
        setFont("Courier", "I", 10);
        cell("Page " + getPageNumber(), 0, "C");
    }
}
```

Headers and footers can therefore be reused across pages without manually managing the page lifecycle.

## Images

Images can be positioned using absolute coordinates on the page:

```java
pdf.drawImage(
    "images/logo.png",
    10,
    10,
    25
);
```

The image position is measured from the top-left corner of the physical page.

## Examples

The repository contains examples demonstrating the main GridBoxPDF features:

1. Minimal PDF
2. Header and Footer
3. Colors
4. Tables
5. Images
6. Text and multi-line text
7. ...
8. ...

See the `examples` directory for complete source code and generated documents.

## Why GridBoxPDF?

PDFBox provides a powerful and flexible foundation for creating PDF documents, but working directly with it often requires managing low-level PDF concepts.

GridBoxPDF adds a small layer of abstraction focused on everyday document generation while keeping the underlying PDFBox model accessible.

The goal is not to replace PDFBox, but to make common PDF generation tasks simpler.

## Apache PDFBox

GridBoxPDF is built on top of [Apache PDFBox](https://pdfbox.apache.org/).

PDFBox remains responsible for the underlying PDF creation and rendering operations, while GridBoxPDF provides the higher-level document API.

## License

See the `LICENSE` file for the license terms of GridBoxPDF.

GridBoxPDF uses Apache PDFBox as a dependency. See the Apache PDFBox project and its licensing information for details.
