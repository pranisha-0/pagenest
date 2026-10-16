package com.example.pdf

/**
 * Intelligent layout and structure reconstruction parser for PDF text.
 * Reconstructs original document flow, headings, paragraphs, lists, and quotations,
 * and fixes wrapped sentences while preventing unrelated paragraphs from squeezing together.
 */
object PdfStructureParser {

  private val HEADING_PREFIX_REGEX = Regex(
    "^(?:CHAPTER|Chapter|PART|Part|SECTION|Section|ACT|Act|SCENE|Scene|PROLOGUE|Prologue|EPILOGUE|Epilogue|INTRODUCTION|Introduction|APPENDIX|Appendix|PREFACE|Preface|FOREWORD|Foreword|CONCLUSION|Conclusion)\\b.*",
    RegexOption.IGNORE_CASE
  )

  private val BULLET_PREFIX_REGEX = Regex(
    "^[\\s]*([•*–—▪▫◦✦➢✓✔]|-)\\s+(.+)"
  )

  private val NUMBERED_LIST_PREFIX_REGEX = Regex(
    "^[\\s]*(\\d{1,3}[.)]|\\([0-9a-zA-Z]+\\)|[a-zA-Z][.)])\\s+(.+)"
  )

  private val PAGE_HEADER_FOOTER_NUMBER_REGEX = Regex(
    "^[\\s]*(-|–|—)?\\s*(?:Page|page|PAGE)?\\s*\\d{1,4}\\s*(?:of\\s*\\d{1,4})?\\s*(-|–|—)?\\s*$"
  )

  // Commonly naturally hyphenated words to preserve (never merge into a single word)
  private val NATURALLY_HYPHENATED_WORDS = setOf(
    "well-known", "part-time", "full-time", "high-risk", "low-risk", "self-esteem",
    "self-control", "self-awareness", "open-minded", "short-term", "long-term",
    "world-class", "twenty-one", "twenty-two", "twenty-three", "twenty-four", "twenty-five",
    "twenty-six", "twenty-seven", "twenty-eight", "twenty-nine", "thirty-one", "forty-two",
    "fifty-fifty", "up-to-date", "state-of-the-art", "user-friendly", "co-worker",
    "decision-making", "life-changing", "habit-forming", "step-by-step", "day-to-day",
    "mind-set", "peer-to-peer", "deep-seated", "time-tested"
  )

  /**
   * Reconstructs structured paragraphs and headings from raw lines extracted from a page.
   *
   * @param rawPageText The raw text extracted for the single page
   * @param pageNumber The 1-based page number
   * @param documentTitle Optional document title for stripping repeated running headers
   * @return Structured, formatted text where paragraphs and headings are cleanly separated by "\n\n"
   */
  fun reconstructPageStructure(
    rawPageText: String,
    pageNumber: Int,
    documentTitle: String = ""
  ): String {
    if (rawPageText.isBlank()) return ""

    val normalized = rawPageText.replace("\r\n", "\n").replace("\r", "\n")

    // Remove overlapping duplicate text blocks (drop shadow / faux bold artifacts)
    val dedupedText = removeDuplicateOverlaps(normalized)

    val rawLines = dedupedText.lines().map { it.trimEnd() }
    if (rawLines.isEmpty()) return ""

    // Strip top running headers and bottom running footers/page numbers
    val bodyLines = filterHeadersAndFooters(rawLines, pageNumber, documentTitle)
    if (bodyLines.isEmpty()) return ""

    // Group lines into semantic structural blocks (Paragraphs, Headings, Lists, Quotes)
    val blocks = groupIntoStructuralBlocks(bodyLines)

    // Filter out decorative artifacts or image placeholder tags
    val cleanedBlocks = blocks.mapNotNull { block ->
      val cleaned = cleanBlockContent(block)
      if (cleaned.isNotBlank() && !isIgnoredVisualPlaceholder(cleaned)) {
        cleaned
      } else {
        null
      }
    }

    return cleanedBlocks.joinToString("\n\n")
  }

  /**
   * Strips repetitive running headers (top) and page numbers (bottom).
   */
  private fun filterHeadersAndFooters(
    lines: List<String>,
    pageNumber: Int,
    documentTitle: String
  ): List<String> {
    if (lines.isEmpty()) return emptyList()
    val result = lines.toMutableList()

    // 1. Check top line for running header
    if (result.isNotEmpty()) {
      val firstLine = result.first().trim()
      if (isPageHeaderOrNumber(firstLine, pageNumber, documentTitle)) {
        result.removeAt(0)
      }
    }

    // Check second line if first was an empty line or number
    if (result.isNotEmpty() && result.first().isBlank()) {
      result.removeAt(0)
    }

    // 2. Check bottom line for page number / footer
    if (result.isNotEmpty()) {
      val lastLine = result.last().trim()
      if (isPageFooterOrNumber(lastLine, pageNumber)) {
        result.removeAt(result.size - 1)
      }
    }

    // Clean any trailing blank lines
    while (result.isNotEmpty() && result.last().isBlank()) {
      result.removeAt(result.size - 1)
    }

    return result
  }

  private fun isPageHeaderOrNumber(line: String, pageNumber: Int, documentTitle: String): Boolean {
    if (PAGE_HEADER_FOOTER_NUMBER_REGEX.matches(line)) return true
    if (line.equals("$pageNumber", ignoreCase = true)) return true

    if (documentTitle.isNotBlank() && line.length < 70) {
      val cleanDocTitle = documentTitle.replace("_", " ").trim()
      if (line.contains(cleanDocTitle, ignoreCase = true)) return true
    }

    // Match lines like: "12 | Chapter 3" or "Atomic Habits — 45"
    if (line.matches(Regex("^(?:\\d{1,4}\\s*[|•—–-]\\s*.{2,40}|.{2,40}\\s*[|•—–-]\\s*\\d{1,4})$"))) {
      return true
    }

    return false
  }

  private fun isPageFooterOrNumber(line: String, pageNumber: Int): Boolean {
    if (PAGE_HEADER_FOOTER_NUMBER_REGEX.matches(line)) return true
    if (line.equals("$pageNumber", ignoreCase = true)) return true
    if (line.matches(Regex("^[\\s]*[0-9]{1,4}\\s*/\\s*[0-9]{1,4}[\\s]*$"))) return true
    return false
  }

  /**
   * Groups physical lines into logical structural blocks:
   * - Preserves headings as distinct blocks
   * - Preserves bullet points and numbered list items
   * - Glues wrapped sentence lines together smoothly without accidental line breaks
   * - Preserves genuine paragraph breaks
   */
  private fun groupIntoStructuralBlocks(lines: List<String>): List<String> {
    val blocks = mutableListOf<String>()
    val currentBlockLines = mutableListOf<String>()

    fun flushCurrentBlock() {
      if (currentBlockLines.isNotEmpty()) {
        val joined = reconstructParagraphText(currentBlockLines)
        if (joined.isNotBlank()) {
          blocks.add(joined)
        }
        currentBlockLines.clear()
      }
    }

    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      val trimmed = line.trim()

      if (trimmed.isEmpty()) {
        // Blank line is an explicit paragraph separator
        flushCurrentBlock()
        i++
        continue
      }

      // Check if current line is a distinct heading
      if (isHeadingLine(trimmed)) {
        flushCurrentBlock()
        blocks.add(trimmed)
        i++
        continue
      }

      // Check if current line is a list item (bullet or numbered)
      if (isListItemLine(trimmed)) {
        flushCurrentBlock()
        // Collect multi-line list item if it wraps
        val listLines = mutableListOf(trimmed)
        var nextIdx = i + 1
        while (nextIdx < lines.size) {
          val nextLine = lines[nextIdx].trim()
          if (nextLine.isEmpty() || isHeadingLine(nextLine) || isListItemLine(nextLine)) {
            break
          }
          // Indented continuation line for list item
          if (lines[nextIdx].startsWith("   ") || lines[nextIdx].startsWith("\t") || !endsWithSentenceTerminator(listLines.last())) {
            listLines.add(nextLine)
            nextIdx++
          } else {
            break
          }
        }
        blocks.add(reconstructParagraphText(listLines))
        i = nextIdx
        continue
      }

      // Check if previous block was completed (e.g. ended with terminal punctuation)
      // and this line is clearly a new paragraph (e.g., indented or starts a new thought)
      if (currentBlockLines.isNotEmpty()) {
        val prevLine = currentBlockLines.last().trim()
        val isPrevSentenceTerminated = endsWithSentenceTerminator(prevLine)
        val isCurrentIndented = line.startsWith("  ") || line.startsWith("\t")
        val isPrevVeryShort = prevLine.length < 40 && prevLine.length < (currentBlockLines.first().length * 0.6f)

        if (isPrevSentenceTerminated && (isCurrentIndented || isPrevVeryShort)) {
          // Paragraph break detected!
          flushCurrentBlock()
        }
      }

      currentBlockLines.add(line)
      i++
    }

    flushCurrentBlock()
    return blocks
  }

  /**
   * Reconstructs wrapped lines within a single paragraph or list item:
   * - Fixes line-ending hyphens ("infor-\nmation" -> "information")
   * - Preserves genuine hyphens ("well-known")
   * - Reconstructs sentences across lines with single space
   */
  private fun reconstructParagraphText(lines: List<String>): String {
    if (lines.isEmpty()) return ""
    if (lines.size == 1) return lines[0].trim()

    val sb = StringBuilder()
    for (idx in lines.indices) {
      val rawLine = lines[idx].trim()
      if (rawLine.isEmpty()) continue

      if (sb.isEmpty()) {
        sb.append(rawLine)
        continue
      }

      // Check for split word hyphen at previous line's end
      if (sb.endsWith("-")) {
        val wordBeforeHyphen = sb.toString().substringAfterLast(" ").removeSuffix("-")
        val wordAfterHyphen = rawLine.substringBefore(" ")

        val candidateCompound = "$wordBeforeHyphen-$wordAfterHyphen".lowercase()
        val candidateMerged = "$wordBeforeHyphen$wordAfterHyphen"

        if (NATURALLY_HYPHENATED_WORDS.contains(candidateCompound)) {
          // Keep hyphen
          sb.append(rawLine)
        } else if (candidateCompound.contains("-") && (wordBeforeHyphen.length <= 2 || wordAfterHyphen.length <= 2)) {
          // Short syllable hyphenation across line breaks: merge without hyphen
          sb.setLength(sb.length - 1)
          sb.append(rawLine)
        } else {
          // Default PDF line break hyphenation: merge seamlessly
          sb.setLength(sb.length - 1)
          sb.append(rawLine)
        }
      } else {
        // Normal line wrap: join with single space
        sb.append(" ")
        sb.append(rawLine)
      }
    }

    return sb.toString().trim()
  }

  private fun endsWithSentenceTerminator(line: String): Boolean {
    val clean = line.trimEnd(' ', '\t', '"', '”', '’', '\'')
    return clean.endsWith(".") || clean.endsWith("?") || clean.endsWith("!") || clean.endsWith(":")
  }

  private fun isHeadingLine(trimmed: String): Boolean {
    if (trimmed.length > 75) return false
    if (trimmed.isBlank()) return false

    // Explicit keywords like "Chapter 1", "Part II", etc.
    if (HEADING_PREFIX_REGEX.matches(trimmed)) return true

    // ALL CAPS line of modest length without ending punctuation (e.g. "THE LAW OF EFFECT")
    if (trimmed.length in 4..60 && trimmed == trimmed.uppercase() && trimmed.any { it.isLetter() }) {
      if (!trimmed.endsWith(".") && !trimmed.endsWith(",")) {
        return true
      }
    }

    // Markdown or styled heading prefixes
    if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
      return true
    }

    return false
  }

  private fun isListItemLine(trimmed: String): Boolean {
    if (BULLET_PREFIX_REGEX.matches(trimmed)) return true
    if (NUMBERED_LIST_PREFIX_REGEX.matches(trimmed)) return true
    return false
  }

  /**
   * Identifies and ignores decorative image placeholders like [Image], [Illustration], etc.
   */
  private fun isIgnoredVisualPlaceholder(text: String): Boolean {
    val trimmed = text.trim()
    val lower = trimmed.lowercase()
    if (lower == "[image]" || lower == "[illustration]" || lower == "[figure]" || lower == "[photo]" || lower == "[diagram]") {
      return true
    }
    if (lower.startsWith("[illustration") && lower.endsWith("]")) {
      return true
    }
    return false
  }

  /**
   * Deduplicates exact repeated tokens caused by faux-bold or shadow text layers in PDFs.
   */
  private fun removeDuplicateOverlaps(text: String): String {
    // Replaces duplicate phrases like "Word Word" if generated by duplicated text overlay
    return text.replace(Regex("(?i)\\b(\\w{3,})\\s+\\1\\b"), "$1")
  }

  private fun cleanBlockContent(block: String): String {
    // Normalize spaces within the block
    return block.replace(Regex("[ \\t]+"), " ").trim()
  }
}
