package com.example.airwave

import com.example.airwave.util.QrCodeHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeHelperTest {

    @Test
    fun `valid payload parses to nickname`() {
        assertEquals("Alice", QrCodeHelper.parseVerifyPayload("AW/VERIFY|Alice"))
        assertEquals("Alice", QrCodeHelper.parseVerifyPayload("  AW/VERIFY|  Alice  "))
        assertEquals("Some Name", QrCodeHelper.parseVerifyPayload("AW/VERIFY|Some Name"))
    }

    @Test
    fun `unrelated or malformed payloads are rejected`() {
        assertNull(QrCodeHelper.parseVerifyPayload("https://example.com"))
        assertNull(QrCodeHelper.parseVerifyPayload("AW/HELLO|Alice"))
        assertNull(QrCodeHelper.parseVerifyPayload("AW/VERIFY|"))
        assertNull(QrCodeHelper.parseVerifyPayload("AW/VERIFY|   "))
        assertNull(QrCodeHelper.parseVerifyPayload("AW/VERIFY|a|b"))
        assertNull(QrCodeHelper.parseVerifyPayload("AW/VERIFY|" + "x".repeat(25)))
        assertNull(QrCodeHelper.parseVerifyPayload(null))
    }

    @Test
    fun `qr payload round-trips through encoding and nv21 decode`() {
        val payload = "AW/VERIFY|Bob"
        val size = 160
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)

        // Build a synthetic NV21 frame: Y=255 for white, Y=0 for black, UV neutral.
        val nv21 = ByteArray(size * size * 3 / 2) { 128.toByte() }
        for (y in 0 until size) {
            for (x in 0 until size) {
                nv21[y * size + x] = if (matrix.get(x, y)) 0.toByte() else 255.toByte()
            }
        }

        assertEquals(payload, QrCodeHelper.decodeQrFromNv21(nv21, size, size))
    }
}