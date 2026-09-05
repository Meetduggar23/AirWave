package com.example.airwave.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Local, offline QR helpers for the Verify feature.
 *
 * The payload only carries the user's AirWave nickname (the same identity shown
 * everywhere in the app) - no device address, no Bluetooth details, no other
 * private data. Everything is generated and decoded on-device.
 */
object QrCodeHelper {

    /** Prefix that marks a QR as an AirWave verification QR. */
    const val VERIFY_PREFIX = "AW/VERIFY|"

    private const val MAX_NAME_LENGTH = 24

    /** Builds the payload for the user's own verification QR. */
    fun buildVerifyPayload(nickname: String): String {
        return VERIFY_PREFIX + nickname.trim()
    }

    /**
     * Parses a scanned payload. Returns the AirWave nickname only when the QR is
     * a valid AirWave verification QR; returns null for unrelated/malformed QR.
     */
    fun parseVerifyPayload(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith(VERIFY_PREFIX)) return null
        val name = trimmed.removePrefix(VERIFY_PREFIX).trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) return null
        if (name.any { it == '|' || it == '\n' || it == '\r' }) return null
        return name
    }

    /** Renders [content] as a white-background QR bitmap of [sizePx] x [sizePx]. */
    fun generateQr(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matrix: BitMatrix = QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                val offset = y * sizePx
                for (x in 0 until sizePx) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            null
        }
    }

    /** Decodes any QR content from a bitmap, or null if nothing was found. */
    fun decodeQr(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            QRCodeReader().decode(binaryBitmap, hints).text
        } catch (e: Exception) {
            // NotFoundException / ChecksumException / FormatException and friends.
            null
        }
    }

    /**
     * Decodes any QR content from NV21 camera data (Y plane with interleaved V/U).
     * Width/height are the image dimensions; cropping and mirroring are unused
     * for the rear camera.
     */
    fun decodeQrFromNv21(data: ByteArray, width: Int, height: Int): String? {
        return try {
            val source = PlanarYUVLuminanceSource(
                data, width, height,
                0, 0, width, height, false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            QRCodeReader().decode(binaryBitmap, hints).text
        } catch (e: Exception) {
            null
        }
    }
}