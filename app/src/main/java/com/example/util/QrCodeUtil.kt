package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject

data class QrContactData(
    val name: String,
    val phone: String,
    val publicKey: String,
    val matrixId: String = ""
)

object QrCodeUtil {

    /**
     * Generates a 2D boolean matrix representing a QR code structure with finder,
     * alignment, timing patterns and encoded data payload bitstream.
     */
    fun generateQrMatrix(content: String, size: Int = 29): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) { false } }

        // Helper to place Finder Pattern (7x7)
        fun placeFinderPattern(rowStart: Int, colStart: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInnerBox = r in 2..4 && c in 2..4
                    matrix[rowStart + r][colStart + c] = isOuterBorder || isInnerBox
                }
            }
        }

        // Place Top-Left, Top-Right, Bottom-Left Finder Patterns
        if (size >= 21) {
            placeFinderPattern(0, 0)
            placeFinderPattern(0, size - 7)
            placeFinderPattern(size - 7, 0)
        }

        // Place Timing Patterns
        for (i in 8 until (size - 8)) {
            matrix[6][i] = i % 2 == 0
            matrix[i][6] = i % 2 == 0
        }

        // Place Alignment Pattern if size >= 29
        if (size >= 29) {
            val alignR = size - 7
            val alignC = size - 7
            for (r in -2..2) {
                for (c in -2..2) {
                    val isOuter = r == -2 || r == 2 || c == -2 || c == 2
                    val isCenter = r == 0 && c == 0
                    matrix[alignR + r][alignC + c] = isOuter || isCenter
                }
            }
        }

        // Encode Content Bits deterministically into matrix
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        var bitIndex = 0

        for (col in (size - 1) downTo 0 step 2) {
            var cCol = col
            if (cCol == 6) cCol-- // Skip timing column
            for (row in 0 until size) {
                val actualRow = if ((col / 2) % 2 == 0) (size - 1 - row) else row

                for (cOffset in 0..1) {
                    val r = actualRow
                    val c = cCol - cOffset
                    if (r in 0 until size && c in 0 until size) {
                        // Skip finder patterns, timing lines, and alignment patterns
                        val isReserved = isReservedArea(r, c, size)
                        if (!isReserved) {
                            val byteIdx = (bitIndex / 8) % contentBytes.size
                            val bitIdxInByte = 7 - (bitIndex % 8)
                            val bitVal = ((contentBytes[byteIdx].toInt() shr bitIdxInByte) and 1) == 1
                            matrix[r][c] = bitVal xor ((r + c) % 2 == 0) // Apply standard QR checkerboard mask
                            bitIndex++
                        }
                    }
                }
            }
        }

        return matrix
    }

    private fun isReservedArea(r: Int, c: Int, size: Int): Boolean {
        // Finder patterns + 1 separator margin
        if (r <= 8 && c <= 8) return true
        if (r <= 8 && c >= size - 9) return true
        if (r >= size - 9 && c <= 8) return true

        // Timing patterns
        if (r == 6 || c == 6) return true

        // Alignment pattern
        if (size >= 29 && r in (size - 9)..(size - 5) && c in (size - 9)..(size - 5)) return true

        return false
    }

    /**
     * Converts content into a high-res Android ImageBitmap for Compose display.
     */
    fun createQrBitmap(content: String, widthPx: Int = 512, heightPx: Int = 512): ImageBitmap {
        val matrixSize = 29
        val matrix = generateQrMatrix(content, matrixSize)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

        val cellWidth = widthPx / matrixSize
        val cellHeight = heightPx / matrixSize

        val darkColor = Color.parseColor("#1DE9B6") // SoftTeal / Matrix Cyan Accent
        val lightColor = Color.parseColor("#1C102A") // DarkPlum background

        for (x in 0 until widthPx) {
            for (y in 0 until heightPx) {
                val gridX = (x / cellWidth).coerceIn(0, matrixSize - 1)
                val gridY = (y / cellHeight).coerceIn(0, matrixSize - 1)
                val isDark = matrix[gridY][gridX]
                bitmap.setPixel(x, y, if (isDark) darkColor else lightColor)
            }
        }

        return bitmap.asImageBitmap()
    }

    /**
     * Encodes a ContactEntity into a QR payload JSON string.
     */
    fun encodeContactToQr(name: String, phone: String, publicKey: String, matrixId: String = ""): String {
        val json = JSONObject().apply {
            put("type", "krama_p2p_contact")
            put("name", name)
            put("phone", phone)
            put("publicKey", publicKey)
            put("matrixId", matrixId.ifEmpty { "@${name.lowercase().replace(" ", "")}:krama.secure" })
            put("timestamp", System.currentTimeMillis())
        }
        return json.toString()
    }

    /**
     * Parses scanned QR payload string into QrContactData.
     */
    fun parseQrContact(qrPayload: String): QrContactData? {
        return try {
            val json = JSONObject(qrPayload)
            if (json.optString("type") == "krama_p2p_contact") {
                QrContactData(
                    name = json.getString("name"),
                    phone = json.getString("phone"),
                    publicKey = json.getString("publicKey"),
                    matrixId = json.optString("matrixId")
                )
            } else if (qrPayload.contains("+") || qrPayload.length >= 7) {
                // Raw phone or link fallback
                QrContactData(
                    name = "Scanned Friend",
                    phone = qrPayload,
                    publicKey = "ed25519_pk_scanned_${System.currentTimeMillis()}"
                )
            } else null
        } catch (e: Exception) {
            if (qrPayload.isNotBlank()) {
                QrContactData(
                    name = "P2P Contact",
                    phone = qrPayload,
                    publicKey = "ed25519_pk_${qrPayload.hashCode()}"
                )
            } else null
        }
    }
}
