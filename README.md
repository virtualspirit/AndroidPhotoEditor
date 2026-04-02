# AndroidPhotoEditor

[![JitPack](https://jitpack.io/v/virtualspirit/AndroidPhotoEditor.svg)](https://jitpack.io/#virtualspirit/AndroidPhotoEditor)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)
[![License](https://img.shields.io/badge/license-MIT-black)](LICENSE)

A fully-featured Android photo editor library with drawing shapes, text stickers, image stickers, filters, crop, undo/redo, and more. Built in Kotlin.

Forked from [burhanrashid52/PhotoEditor](https://github.com/burhanrashid52/PhotoEditor) with significant enhancements.

---

## Features

- **Shapes** — Draw brush strokes, lines, arrows, ovals, and rectangles with customizable color, stroke width, stroke style (solid / dashed / dotted), and fill color (oval & rect)
- **Text stickers** — Add and edit text with custom color, font family, font size, and background color
- **Image stickers** — Add images/stickers from URLs or local paths
- **Emoji** — Insert emoji with custom fonts
- **Filters** — Apply built-in photo filters
- **Crop** — Free-style crop powered by uCrop
- **Undo / Redo** — Full history for all operations including crop, filter, shape changes, color changes, and transforms
- **Duplicate** — Duplicate any selected view
- **Pinch to scale and rotate** — Works on all graphic elements
- **Selection** — Tap to select, tap outside to deselect; palette/delete/duplicate buttons auto-enable based on selection state

---

## Installation

Add JitPack to your project-level `build.gradle`:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency in your app-level `build.gradle`:

```groovy
implementation 'com.github.virtualspirit:AndroidPhotoEditor:v3.1.7'
```

---

## Setup

Add `PhotoEditorView` to your layout:

```xml
<com.virtualspirit.photoeditor.PhotoEditorView
    android:id="@+id/photoEditorView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:photo_src="@drawable/your_image" />
```

Build a `PhotoEditor` instance:

```kotlin
val photoEditor = PhotoEditor.Builder(this, photoEditorView)
    .setPinchTextScalable(true)
    .build()
```

---

## Usage

### Drawing Shapes

```kotlin
val shapeBuilder = ShapeBuilder()
    .withShapeType(ShapeType.Oval)
    .withShapeColor(Color.RED)
    .withShapeSize(25f)
    .withStrokeStyle(StrokeStyle.SOLID)
    .withFillColor(Color.YELLOW) // oval and rect only

photoEditor.setShape(shapeBuilder)
photoEditor.setBrushDrawingMode(true)
```

Shape types: `ShapeType.Brush`, `ShapeType.Line`, `ShapeType.Arrow()`, `ShapeType.Oval`, `ShapeType.Rectangle`

Stroke styles: `StrokeStyle.SOLID`, `StrokeStyle.DASHED`, `StrokeStyle.DOTTED`

### Text

```kotlin
val style = TextStyleBuilder()
    .withTextColor(Color.WHITE)
    .withTextSize(18f)

photoEditor.addText("Hello", style)
```

### Emoji

```kotlin
photoEditor.addEmoji("😊")
```

### Image Sticker

```kotlin
photoEditor.addImage(bitmap)
```

### Filters

```kotlin
photoEditor.setFilterEffect(PhotoFilter.BRIGHTNESS)
```

### Crop

Crop is handled by the activity via uCrop and is part of the undo/redo history.

### Undo / Redo

```kotlin
photoEditor.undo()
photoEditor.redo()
```

### Save

```kotlin
lifecycleScope.launch {
    val result = photoEditor.saveAsFile(filePath)
    if (result is SaveFileResult.Success) {
        // saved
    }
}
```

---

## EditImageActivity

The library ships a ready-to-use `EditImageActivity` that can be launched via intent:

```kotlin
val intent = Intent(context, EditImageActivity::class.java)
intent.putExtra("path", imagePath)
intent.putExtra("targetPath", outputPath)
intent.putExtra("tools", arrayOf("draw", "line", "arrow", "circle", "square", "clip", "textSticker", "imageSticker", "filter", "pointer"))
startActivityForResult(intent, REQUEST_CODE)
```

### Intent Options

| Key | Type | Description |
|-----|------|-------------|
| `path` | String | Source image path |
| `targetPath` | String | Output file path |
| `tools` | String[] | Tools to show. Values: `draw`, `line`, `arrow`, `circle`, `square`, `clip`, `textSticker`, `imageSticker`, `filter`, `pointer` |
| `stickerPaths` | String[] | URLs or local paths for image stickers |
| `defaultColors` | String[] | Custom color palette (hex strings, e.g. `"#FF0000"`) |
| `defaultStrokeColor` | String | Pre-selected stroke color (hex string) |
| `defaultStrokeWidth` | String | `"small"`, `"medium"` (default), or `"large"` |
| `defaultStrokeStyle` | String | `"solid"` (default), `"dashed"`, or `"dotted"` |
| `defaultTextColor` | String | Default text sticker color (hex string) |
| `defaultFontFamily` | String | Default font family for text stickers |
| `defaultFontSize` | Float | Default font size for text stickers |

---

## Dependencies

- [uCrop](https://github.com/Yalantis/uCrop) — crop
- [Glide](https://github.com/bumptech/glide) — image loading

---

## Build & Publish

See [PUBLISH.md](PUBLISH.md) for step-by-step instructions on bumping the version, building the AAR locally, and releasing a new version via GitHub Actions + JitPack.

---

## License

MIT — Copyright (c) virtualspirit
