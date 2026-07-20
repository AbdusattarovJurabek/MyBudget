package uz.mybudget.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import uz.mybudget.app.data.model.Transaction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExcelBackupManager(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver

    fun export(uri: Uri, transactions: List<Transaction>) {
        contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                zip.writeEntry("[Content_Types].xml", contentTypesXml)
                zip.writeEntry("_rels/.rels", rootRelationshipsXml)
                zip.writeEntry("xl/workbook.xml", workbookXml)
                zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelationshipsXml)
                zip.writeEntry("xl/styles.xml", stylesXml)
                zip.writeEntry("xl/worksheets/sheet1.xml", sheetXml(transactions))
            }
        } ?: error("Tanlangan faylni ochib bo‘lmadi")
    }

    fun import(uri: Uri): List<Transaction> {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Tanlangan faylni o‘qib bo‘lmadi")
        return if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            parseXlsx(bytes)
        } else {
            parseCsv(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        }
    }

    private fun parseXlsx(bytes: ByteArray): List<Transaction> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    zip.copyTo(buffer)
                    entries[entry.name] = buffer.toByteArray()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheet = entries["xl/worksheets/sheet1.xml"]
            ?: error("Excel faylida birinchi jadval topilmadi")
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        return rowsToTransactions(parseSheet(sheet, sharedStrings))
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser().apply {
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var insideItem = false
        var insideText = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> {
                        insideItem = true
                        current = StringBuilder()
                    }
                    "t" -> if (insideItem) insideText = true
                }
                XmlPullParser.TEXT -> if (insideText) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> insideText = false
                    "si" -> {
                        result += current.toString()
                        insideItem = false
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val parser = Xml.newPullParser().apply {
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableMapOf<Int, String>()
        var currentColumn = 0
        var currentType: String? = null
        var currentValue = StringBuilder()
        var insideValue = false
        var insideInlineString = false
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableMapOf()
                    "c" -> {
                        currentColumn = columnIndex(parser.getAttributeValue(null, "r"))
                        currentType = parser.getAttributeValue(null, "t")
                        currentValue = StringBuilder()
                    }
                    "v" -> insideValue = true
                    "is" -> insideInlineString = true
                }
                XmlPullParser.TEXT -> if (insideValue || insideInlineString) {
                    currentValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> insideValue = false
                    "is" -> insideInlineString = false
                    "c" -> {
                        var value = currentValue.toString()
                        if (currentType == "s") {
                            value = sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                        }
                        currentRow[currentColumn] = value
                    }
                    "row" -> {
                        val maxColumn = currentRow.keys.maxOrNull() ?: -1
                        rows += (0..maxColumn).map { currentRow[it].orEmpty() }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun parseCsv(content: String): List<Transaction> = rowsToTransactions(parseCsvRows(content))

    private fun parseCsvRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        var value = StringBuilder()
        var quoted = false
        var index = 0
        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && quoted && index + 1 < content.length && content[index + 1] == '"' -> {
                    value.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    row += value.toString()
                    value = StringBuilder()
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < content.length && content[index + 1] == '\n') index++
                    row += value.toString()
                    if (row.any(String::isNotBlank)) rows += row
                    row = mutableListOf()
                    value = StringBuilder()
                }
                else -> value.append(char)
            }
            index++
        }
        row += value.toString()
        if (row.any(String::isNotBlank)) rows += row
        return rows
    }

    private fun rowsToTransactions(rows: List<List<String>>): List<Transaction> {
        if (rows.isEmpty()) return emptyList()
        val headers = rows.first().map(::normalizeHeader)
        fun indexOf(vararg names: String): Int = headers.indexOfFirst { it in names }

        val idIndex = indexOf("id")
        val typeIndex = indexOf("turi", "type")
        val amountIndex = indexOf("summa", "amount")
        val categoryIndex = indexOf("kategoriya", "category")
        val noteIndex = indexOf("izoh", "note")
        val dateIndex = indexOf("sana", "date")
        val createdIndex = indexOf("yaratilgan", "yaratilganvaqt", "createdat")
        val updatedIndex = indexOf("yangilangan", "yangilanganvaqt", "updatedat")

        require(typeIndex >= 0 && amountIndex >= 0 && categoryIndex >= 0 && dateIndex >= 0) {
            "Excel ustunlari tanilmadi. Turi, Summa, Kategoriya va Sana ustunlari kerak."
        }

        return rows.drop(1).mapNotNull { row ->
            val amount = row.valueAt(amountIndex).replace(" ", "").replace(',', '.').toDoubleOrNull()
                ?: return@mapNotNull null
            val now = System.currentTimeMillis()
            Transaction(
                id = row.valueAt(idIndex).ifBlank { UUID.randomUUID().toString() },
                type = normalizeType(row.valueAt(typeIndex)),
                amount = amount,
                category = row.valueAt(categoryIndex).ifBlank { "Boshqa" },
                note = row.valueAt(noteIndex),
                date = parseDate(row.valueAt(dateIndex)) ?: now,
                createdAt = parseDate(row.valueAt(createdIndex)) ?: now,
                updatedAt = parseDate(row.valueAt(updatedIndex)) ?: now
            )
        }
    }

    private fun sheetXml(transactions: List<Transaction>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<cols><col min=\"1\" max=\"1\" width=\"38\" customWidth=\"1\"/>")
        append("<col min=\"2\" max=\"2\" width=\"12\" customWidth=\"1\"/>")
        append("<col min=\"3\" max=\"3\" width=\"16\" customWidth=\"1\"/>")
        append("<col min=\"4\" max=\"5\" width=\"24\" customWidth=\"1\"/>")
        append("<col min=\"6\" max=\"8\" width=\"20\" customWidth=\"1\"/></cols><sheetData>")
        val headers = listOf("ID", "Turi", "Summa", "Kategoriya", "Izoh", "Sana", "Yaratilgan vaqt", "Yangilangan vaqt")
        append("<row r=\"1\">")
        headers.forEachIndexed { index, header -> append(inlineCell(index, 1, header, style = 2)) }
        append("</row>")
        transactions.forEachIndexed { rowIndex, item ->
            val rowNumber = rowIndex + 2
            append("<row r=\"").append(rowNumber).append("\">")
            append(inlineCell(0, rowNumber, item.id))
            append(inlineCell(1, rowNumber, item.type))
            append(numberCell(2, rowNumber, item.amount))
            append(inlineCell(3, rowNumber, item.category))
            append(inlineCell(4, rowNumber, item.note))
            append(numberCell(5, rowNumber, toExcelDate(item.date), style = 1))
            append(numberCell(6, rowNumber, toExcelDate(item.createdAt), style = 1))
            append(numberCell(7, rowNumber, toExcelDate(item.updatedAt), style = 1))
            append("</row>")
        }
        append("</sheetData><autoFilter ref=\"A1:H1\"/></worksheet>")
    }

    private fun inlineCell(column: Int, row: Int, value: String, style: Int = 0): String {
        return "<c r=\"${columnName(column)}$row\" t=\"inlineStr\" s=\"$style\"><is><t xml:space=\"preserve\">${escapeXml(value)}</t></is></c>"
    }

    private fun numberCell(column: Int, row: Int, value: Double, style: Int = 0): String {
        return "<c r=\"${columnName(column)}$row\" s=\"$style\"><v>$value</v></c>"
    }

    private fun parseDate(value: String): Long? {
        val normalized = value.trim()
        normalized.toLongOrNull()?.let { raw ->
            return if (raw > 10_000_000_000L) raw else fromExcelDate(raw.toDouble())
        }
        normalized.toDoubleOrNull()?.let { return fromExcelDate(it) }
        for (pattern in listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy")) {
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(normalized)?.time
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun normalizeType(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
        "income", "kirim", "daromad" -> "income"
        else -> "expense"
    }

    private fun normalizeHeader(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace("‘", "")
        .replace("’", "")
        .replace("'", "")
        .filter(Char::isLetterOrDigit)

    private fun List<String>.valueAt(index: Int): String = if (index >= 0) getOrNull(index).orEmpty() else ""

    private fun columnIndex(reference: String?): Int {
        val letters = reference.orEmpty().takeWhile(Char::isLetter).uppercase(Locale.ROOT)
        var value = 0
        letters.forEach { value = value * 26 + (it - 'A' + 1) }
        return (value - 1).coerceAtLeast(0)
    }

    private fun columnName(index: Int): String {
        var value = index + 1
        val result = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            result.append(('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return result.reverse().toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun toExcelDate(epochMillis: Long): Double = epochMillis / MILLIS_PER_DAY + EXCEL_EPOCH_OFFSET
    private fun fromExcelDate(serial: Double): Long = ((serial - EXCEL_EPOCH_OFFSET) * MILLIS_PER_DAY).toLong()

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000.0
        private const val EXCEL_EPOCH_OFFSET = 25_569.0

        fun defaultFileName(): String {
            val format = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            return "MyBudget_${format.format(Date())}.xlsx"
        }
    }
}

private fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private const val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

private const val rootRelationshipsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

private const val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Operatsiyalar" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

private const val workbookRelationshipsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

private const val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2"><font/><font><b/></font></fonts>
  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
  <borders count="1"><border/></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
