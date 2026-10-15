package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.data.model.BookEntity
import com.example.data.model.BookPageTextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.InflaterInputStream

object PdfManager {

  suspend fun renderPdfPage(filePath: String, pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val file = File(filePath)
      if (!file.exists()) return@withContext null
      val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
      val renderer = PdfRenderer(pfd)
      if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
        renderer.close()
        pfd.close()
        return@withContext null
      }
      val page = renderer.openPage(pageIndex)
      val aspectRatio = page.height.toFloat() / page.width.toFloat()
      val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)
      val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
      // Fill with crisp white background before rendering
      val canvas = Canvas(bitmap)
      canvas.drawColor(Color.WHITE)
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      renderer.close()
      pfd.close()
      bitmap
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
    try {
      val file = File(filePath)
      if (!file.exists()) return@withContext 0
      val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
      val renderer = PdfRenderer(pfd)
      val count = renderer.pageCount
      renderer.close()
      pfd.close()
      count
    } catch (e: Exception) {
      e.printStackTrace()
      0
    }
  }

  suspend fun generateCoverThumbnail(context: Context, filePath: String, bookId: String): String? = withContext(Dispatchers.IO) {
    try {
      val bitmap = renderPdfPage(filePath, 0, targetWidth = 400) ?: return@withContext null
      val coversDir = File(context.filesDir, "covers")
      if (!coversDir.exists()) coversDir.mkdirs()
      val coverFile = File(coversDir, "$bookId.png")
      FileOutputStream(coverFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
      }
      coverFile.absolutePath
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun importPdfFromUri(context: Context, uri: Uri): Pair<BookEntity, List<BookPageTextEntity>> = withContext(Dispatchers.IO) {
    var fileName = "Imported Book.pdf"
    var fileSize = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
      }
    }

    val bookId = UUID.randomUUID().toString()
    val booksDir = File(context.filesDir, "books")
    if (!booksDir.exists()) booksDir.mkdirs()

    val destFile = File(booksDir, "$bookId.pdf")
    context.contentResolver.openInputStream(uri)?.use { input ->
      FileOutputStream(destFile).use { output ->
        input.copyTo(output)
      }
    }

    val pageCount = getPageCount(destFile.absolutePath).coerceAtLeast(1)
    val coverPath = generateCoverThumbnail(context, destFile.absolutePath, bookId)

    val cleanTitle = fileName.removeSuffix(".pdf").replace("_", " ")
    val extractedPages = extractTextFromFile(destFile, bookId, pageCount)

    val book = BookEntity(
      id = bookId,
      title = cleanTitle,
      author = "Personal Library",
      filePath = destFile.absolutePath,
      coverPath = coverPath,
      coverColorHex = "#5E4F8A",
      totalPages = pageCount,
      currentPage = 1,
      readingProgress = 0f,
      dateAdded = System.currentTimeMillis(),
      lastReadTime = System.currentTimeMillis(),
      isOfflineAudioReady = extractedPages.any { it.textContent.isNotBlank() },
      isSample = false,
      category = "Reading",
      description = "Imported PDF document (${pageCount} pages, ${(destFile.length() / 1024)} KB)"
    )

    Pair(book, extractedPages)
  }

  private fun extractTextFromFile(file: File, bookId: String, totalPages: Int): List<BookPageTextEntity> {
    val result = mutableListOf<BookPageTextEntity>()
    try {
      val bytes = file.readBytes()
      val extractedChunks = mutableListOf<String>()

      // Look for PDF streams
      var index = 0
      val streamMarker = "stream".toByteArray(Charsets.US_ASCII)
      val endStreamMarker = "endstream".toByteArray(Charsets.US_ASCII)

      while (index < bytes.size - 10) {
        val streamPos = indexOfBytes(bytes, streamMarker, index)
        if (streamPos == -1) break
        var dataStart = streamPos + streamMarker.size
        if (bytes[dataStart] == '\r'.code.toByte()) dataStart++
        if (bytes[dataStart] == '\n'.code.toByte()) dataStart++

        val endPos = indexOfBytes(bytes, endStreamMarker, dataStart)
        if (endPos == -1) break

        val streamBytes = bytes.copyOfRange(dataStart, endPos)
        val text = tryDecompressFlate(streamBytes) ?: String(streamBytes, Charsets.ISO_8859_1)
        val parsedStrings = parsePdfTextOperators(text)
        if (parsedStrings.isNotBlank()) {
          extractedChunks.add(parsedStrings)
        }
        index = endPos + endStreamMarker.size
      }

      for (p in 1..totalPages) {
        val content = if (p - 1 < extractedChunks.size) {
          extractedChunks[p - 1]
        } else if (extractedChunks.isNotEmpty()) {
          // Fallback if pages don't map 1-to-1 to streams
          extractedChunks.joinToString("\n\n")
        } else {
          "Page $p\n\n[Original PDF layout mode recommended for this document]"
        }

        val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
        result.add(
          BookPageTextEntity(
            bookId = bookId,
            pageNumber = p,
            chapterTitle = "Page $p",
            textContent = content,
            paragraphCount = paragraphs.size.coerceAtLeast(1)
          )
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
      for (p in 1..totalPages) {
        result.add(
          BookPageTextEntity(
            bookId = bookId,
            pageNumber = p,
            chapterTitle = "Page $p",
            textContent = "Page $p content.\n\nRead in Original PDF mode to view full diagrams and text.",
            paragraphCount = 1
          )
        )
      }
    }
    return result
  }

  private fun indexOfBytes(source: ByteArray, target: ByteArray, start: Int): Int {
    for (i in start..(source.size - target.size)) {
      var found = true
      for (j in target.indices) {
        if (source[i + j] != target[j]) {
          found = false
          break
        }
      }
      if (found) return i
    }
    return -1
  }

  private fun tryDecompressFlate(bytes: ByteArray): String? {
    return try {
      val inflater = InflaterInputStream(bytes.inputStream())
      val out = ByteArrayOutputStream()
      val buf = ByteArray(1024)
      var len: Int
      while (inflater.read(buf).also { len = it } > 0) {
        out.write(buf, 0, len)
      }
      out.toString("UTF-8")
    } catch (e: Exception) {
      null
    }
  }

  private fun parsePdfTextOperators(streamContent: String): String {
    val sb = StringBuilder()
    val tjRegex = Regex("""\((.*?)\)\s*Tj""")
    for (match in tjRegex.findAll(streamContent)) {
      val txt = match.groupValues[1]
        .replace("\\(", "(")
        .replace("\\)", ")")
        .replace("\\n", "\n")
      if (txt.isNotBlank()) {
        sb.append(txt).append(" ")
      }
    }
    // Also parse bracketed array [(T) 10 (e) 5 (x) (t)] TJ
    val arrayRegex = Regex("""\[(.*?)\]\s*TJ""")
    for (match in arrayRegex.findAll(streamContent)) {
      val inner = match.groupValues[1]
      val itemRegex = Regex("""\((.*?)\)""")
      for (item in itemRegex.findAll(inner)) {
        sb.append(item.groupValues[1]).append(" ")
      }
    }
    return sb.toString().trim()
  }

  /**
   * Generates a genuine multi-page PDF on disk using Android's native PdfDocument.
   * This creates a real, renderable PDF file with beautiful typography, margins,
   * chapters, and headers that PdfRenderer renders in high resolution.
   */
  suspend fun createSampleBookPdf(
    context: Context,
    bookId: String,
    title: String,
    author: String,
    themeColor: Int,
    pagesContent: List<Pair<String, List<String>>> // Chapter Title to Paragraphs
  ): String = withContext(Dispatchers.IO) {
    val booksDir = File(context.filesDir, "books")
    if (!booksDir.exists()) booksDir.mkdirs()
    val pdfFile = File(booksDir, "$bookId.pdf")

    val pdfDocument = PdfDocument()
    val pageWidth = 595 // standard A4 @ 72 dpi (595 x 842 pt)
    val pageHeight = 842

    val titlePaint = Paint().apply {
      color = themeColor
      textSize = 24f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      isAntiAlias = true
    }

    val subtitlePaint = Paint().apply {
      color = Color.DKGRAY
      textSize = 14f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      isAntiAlias = true
    }

    val chapterPaint = Paint().apply {
      color = Color.rgb(40, 36, 52)
      textSize = 18f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      isAntiAlias = true
    }

    val bodyPaint = Paint().apply {
      color = Color.rgb(50, 48, 56)
      textSize = 12f
      typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
      isAntiAlias = true
    }

    val headerPaint = Paint().apply {
      color = Color.LTGRAY
      textSize = 9f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      isAntiAlias = true
    }

    // --- Page 1: Elegant Cover Page ---
    val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val coverPage = pdfDocument.startPage(coverPageInfo)
    val coverCanvas = coverPage.canvas

    // Warm cream background
    coverCanvas.drawColor(Color.rgb(251, 248, 242))

    // Decorative frame
    val framePaint = Paint().apply {
      color = themeColor
      style = Paint.Style.STROKE
      strokeWidth = 3f
      isAntiAlias = true
    }
    coverCanvas.drawRect(40f, 40f, (pageWidth - 40).toFloat(), (pageHeight - 40).toFloat(), framePaint)

    // Inner thin border
    val innerFramePaint = Paint().apply {
      color = Color.argb(60, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
      style = Paint.Style.STROKE
      strokeWidth = 1f
      isAntiAlias = true
    }
    coverCanvas.drawRect(46f, 46f, (pageWidth - 46).toFloat(), (pageHeight - 46).toFloat(), innerFramePaint)

    // Cover Title block
    val coverTitlePaint = Paint().apply {
      color = Color.rgb(31, 29, 43)
      textSize = 30f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
      textAlign = Paint.Align.CENTER
      isAntiAlias = true
    }
    coverCanvas.drawText(title, (pageWidth / 2).toFloat(), 280f, coverTitlePaint)

    val coverAuthorPaint = Paint().apply {
      color = Color.rgb(107, 102, 119)
      textSize = 16f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
      textAlign = Paint.Align.CENTER
      isAntiAlias = true
    }
    coverCanvas.drawText("by $author", (pageWidth / 2).toFloat(), 330f, coverAuthorPaint)

    // Decorative divider line
    val dividerPaint = Paint().apply {
      color = themeColor
      strokeWidth = 2f
    }
    coverCanvas.drawLine((pageWidth / 2 - 40).toFloat(), 360f, (pageWidth / 2 + 40).toFloat(), 360f, dividerPaint)

    // Cover Tagline
    val coverTaglinePaint = Paint().apply {
      color = Color.rgb(138, 121, 179)
      textSize = 12f
      typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
      textAlign = Paint.Align.CENTER
      isAntiAlias = true
    }
    coverCanvas.drawText("Unfold Digital Library Edition", (pageWidth / 2).toFloat(), 720f, coverTaglinePaint)
    pdfDocument.finishPage(coverPage)

    // --- Content Pages ---
    var pageNum = 2
    for ((chapterTitle, paragraphs) in pagesContent) {
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas = page.canvas

      // Page background
      canvas.drawColor(Color.rgb(253, 252, 250))

      // Running header
      canvas.drawText("$title — $author", 50f, 45f, headerPaint)
      val pageNumStr = "$pageNum"
      canvas.drawText(pageNumStr, (pageWidth - 50 - headerPaint.measureText(pageNumStr)), 45f, headerPaint)

      val thinLinePaint = Paint().apply {
        color = Color.rgb(230, 225, 218)
        strokeWidth = 0.8f
      }
      canvas.drawLine(50f, 52f, (pageWidth - 50).toFloat(), 52f, thinLinePaint)

      var y = 100f
      // Chapter heading
      canvas.drawText(chapterTitle, 50f, y, chapterPaint)
      y += 35f

      // Draw paragraphs with simple line-wrapping
      val maxWidth = pageWidth - 100f
      for (paragraph in paragraphs) {
        val words = paragraph.split(" ")
        var line = ""
        for (word in words) {
          val testLine = if (line.isEmpty()) word else "$line $word"
          if (bodyPaint.measureText(testLine) > maxWidth) {
            canvas.drawText(line, 50f, y, bodyPaint)
            y += 20f
            line = word
          } else {
            line = testLine
          }
        }
        if (line.isNotEmpty()) {
          canvas.drawText(line, 50f, y, bodyPaint)
          y += 20f
        }
        y += 14f // extra paragraph spacing
      }

      pdfDocument.finishPage(page)
      pageNum++
    }

    FileOutputStream(pdfFile).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    pdfFile.absolutePath
  }
}
