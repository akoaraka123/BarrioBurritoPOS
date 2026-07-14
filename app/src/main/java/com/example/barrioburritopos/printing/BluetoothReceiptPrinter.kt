package com.example.barrioburritopos.printing

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.barrioburritopos.R
import com.example.barrioburritopos.feature.pos.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class PrintResult {
    object Success : PrintResult()
    object PrinterNotConnected : PrintResult()
    data class Error(val message: String) : PrintResult()
}

data class BluetoothPrinterDevice(
    val name: String,
    val address: String
)

class BluetoothReceiptPrinter(private val context: Context) {
    suspend fun print(receipt: ReceiptData, currency: String, printerAddress: String?): PrintResult = withContext(Dispatchers.IO) {
        if (printerAddress.isNullOrBlank()) return@withContext PrintResult.Error("No printer selected")
        if (!hasBluetoothConnectPermission()) return@withContext PrintResult.Error("Bluetooth permission not granted")

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext PrintResult.Error("Bluetooth is not supported on this device")
        if (!adapter.isEnabled) return@withContext PrintResult.Error("Bluetooth is turned off")

        val device = findPrinter(adapter, printerAddress)
            ?: return@withContext PrintResult.Error(selectedPrinterMissingMessage(adapter, printerAddress))

        try {
            adapter.cancelDiscoveryIfAllowed()
            val socket = connectWithFallback(device)
            socket.outputStream.use { output ->
                output.write(buildReceiptBytes(receipt, currency))
                output.flush()
            }
            socket.close()
            PrintResult.Success
        } catch (e: SecurityException) {
            PrintResult.Error(e.message ?: "Bluetooth permission denied")
        } catch (e: Exception) {
            PrintResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun testConnection(printerAddress: String?): PrintResult = withContext(Dispatchers.IO) {
        if (printerAddress.isNullOrBlank()) return@withContext PrintResult.Error("No printer selected")
        if (!hasBluetoothConnectPermission()) return@withContext PrintResult.Error("Bluetooth permission not granted")

        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext PrintResult.Error("Bluetooth is not supported on this device")
        if (!adapter.isEnabled) return@withContext PrintResult.Error("Bluetooth is turned off")

        val device = findPrinter(adapter, printerAddress)
            ?: return@withContext PrintResult.Error(selectedPrinterMissingMessage(adapter, printerAddress))

        try {
            adapter.cancelDiscoveryIfAllowed()
            val socket = connectWithFallback(device)
            socket.close()
            PrintResult.Success
        } catch (e: SecurityException) {
            PrintResult.Error(e.message ?: "Bluetooth permission denied")
        } catch (e: Exception) {
            PrintResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    fun getPairedDevices(): List<BluetoothPrinterDevice> {
        if (!hasBluetoothConnectPermission()) return emptyList()
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            @SuppressLint("MissingPermission")
            adapter.bondedDevices
                .map { BluetoothPrinterDevice(it.name ?: "Unnamed device", it.address) }
                .sortedWith(compareBy<BluetoothPrinterDevice> { !it.name.contains("PT-210", ignoreCase = true) }.thenBy { it.name })
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothAdapter.cancelDiscoveryIfAllowed() {
        if (!hasBluetoothScanPermission()) return
        try {
            cancelDiscovery()
        } catch (_: SecurityException) {
            // Printing uses a selected paired device, so discovery cancellation is optional.
        }
    }

    @SuppressLint("MissingPermission")
    private fun findPrinter(adapter: BluetoothAdapter, printerAddress: String) =
        adapter.bondedDevices.firstOrNull { device ->
            device.address.equals(printerAddress, ignoreCase = true)
        }

    @SuppressLint("MissingPermission")
    private fun connectWithFallback(device: BluetoothDevice): BluetoothSocket {
        val uuid = UUID.fromString(SPP_UUID)
        val errors = mutableListOf<String>()

        fun tryConnect(label: String, createSocket: () -> BluetoothSocket): BluetoothSocket? {
            val socket = try {
                createSocket()
            } catch (e: Exception) {
                errors += "$label create failed: ${e.readableMessage()}"
                return null
            }

            return try {
                socket.connect()
                socket
            } catch (e: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                errors += "$label connect failed: ${e.readableMessage()}"
                null
            }
        }

        tryConnect("secure SPP") { device.createRfcommSocketToServiceRecord(uuid) }?.let { return it }
        pauseBeforeNextAttempt()
        tryConnect("insecure SPP") { device.createInsecureRfcommSocketToServiceRecord(uuid) }?.let { return it }
        pauseBeforeNextAttempt()
        tryConnect("secure channel 1") { device.createRfcommSocket(1) }?.let { return it }
        pauseBeforeNextAttempt()
        tryConnect("insecure channel 1") { device.createInsecureRfcommSocket(1) }?.let { return it }

        throw IllegalStateException(errors.joinToString(separator = "; "))
    }

    @SuppressLint("MissingPermission")
    private fun selectedPrinterMissingMessage(adapter: BluetoothAdapter, printerAddress: String): String {
        val pairedDevices = try {
            adapter.bondedDevices.joinToString { device ->
                "${device.name ?: "Unnamed"} (${device.address})"
            }
        } catch (e: Exception) {
            "unavailable: ${e.readableMessage()}"
        }
        return "Selected printer $printerAddress is not paired. Paired devices: $pairedDevices"
    }

    private fun pauseBeforeNextAttempt() {
        try {
            Thread.sleep(250)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun BluetoothDevice.createRfcommSocket(channel: Int): BluetoothSocket {
        return javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            .invoke(this, channel) as BluetoothSocket
    }

    private fun BluetoothDevice.createInsecureRfcommSocket(channel: Int): BluetoothSocket {
        return javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
            .invoke(this, channel) as BluetoothSocket
    }

    private fun Exception.readableMessage(): String {
        return message ?: cause?.message ?: javaClass.simpleName
    }

    private fun buildReceiptBytes(receipt: ReceiptData, currency: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write(INIT)
        bytes.write(ALIGN_CENTER)
        bytes.write(logoBytes())
        bytes.write(doubleHeight("BARRIOBURRITO POS\n"))
        bytes.write(normal())
        bytes.write(text("Brgy. Fatima,\n"))
        bytes.write(text("General Santos City\n"))
        bytes.write(facebookPageBytes())
        bytes.write(separator())
        bytes.write(ALIGN_LEFT)
        bytes.write(text(ReceiptFormatter.metaLines(receipt)))
        bytes.write(separator())
        bytes.write(text(ReceiptFormatter.itemHeader()))
        bytes.write(separator())
        bytes.write(text(ReceiptFormatter.itemLines(receipt, currency)))
        bytes.write(separator())
        bytes.write(text(ReceiptFormatter.subtotalLines(receipt, currency)))
        bytes.write(doubleWidthHeight(ReceiptFormatter.totalLine(receipt, currency)))
        bytes.write(text(ReceiptFormatter.paymentLines(receipt, currency)))
        bytes.write(separator())
        bytes.write(ALIGN_CENTER)
        bytes.write(emphasis("THANK YOU!\nCOME AGAIN SOON!\n"))
        bytes.write(text("<3\n"))
        bytes.write(lineFeed(1))
        return bytes.toByteArray()
    }

    private fun logoBytes(): ByteArray {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val trimmed = trimWhiteMargins(source)
        val width = minOf(LOGO_WIDTH_DOTS, trimmed.width)
        val height = (trimmed.height * (width.toFloat() / trimmed.width)).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(trimmed, width, height, true)
        val mono = toMonochrome(scaled)
        return rasterImage(mono)
    }

    private fun facebookPageBytes(): ByteArray {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.fb_logo)
        val icon = facebookIconBitmap(source, FACEBOOK_ICON_DOTS)
        val row = Bitmap.createBitmap(FACEBOOK_ROW_WIDTH_DOTS, FACEBOOK_ROW_HEIGHT_DOTS, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(row)
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            isFakeBoldText = true
        }
        val label = "Barrio Burrito"
        val textWidth = textPaint.measureText(label)
        val gap = 8f
        val groupWidth = icon.width + gap + textWidth
        val startX = ((row.width - groupWidth) / 2f).coerceAtLeast(0f)
        val iconTop = ((row.height - icon.height) / 2f).toFloat()
        val textBaseline = ((row.height - (textPaint.descent() + textPaint.ascent())) / 2f)

        canvas.drawBitmap(icon, startX, iconTop, null)
        canvas.drawText(label, startX + icon.width + gap, textBaseline, textPaint)

        return rasterImage(toMonochrome(row))
    }

    private fun facebookIconBitmap(source: Bitmap, size: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        val radius = size / 2f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x + 0.5f - radius
                val dy = y + 0.5f - radius
                if (dx * dx + dy * dy <= radius * radius) {
                    val pixel = scaled.getPixel(x, y)
                    val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
                    output.setPixel(x, y, if (luminance > 220) Color.WHITE else Color.BLACK)
                }
            }
        }
        canvas.drawCircle(radius, radius, radius - 1f, paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        })
        return output
    }

    private fun toMonochrome(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
                output.setPixel(x, y, if (alpha > 40 && luminance < 170) Color.BLACK else Color.WHITE)
            }
        }
        return output
    }

    private fun trimWhiteMargins(bitmap: Bitmap): Bitmap {
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (hasPrintableInk(bitmap.getPixel(x, y))) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        if (right < left || bottom < top) return bitmap

        val padding = 2
        val cropLeft = (left - padding).coerceAtLeast(0)
        val cropTop = (top - padding).coerceAtLeast(0)
        val cropRight = (right + padding).coerceAtMost(bitmap.width - 1)
        val cropBottom = (bottom + padding).coerceAtMost(bitmap.height - 1)
        return Bitmap.createBitmap(
            bitmap,
            cropLeft,
            cropTop,
            cropRight - cropLeft + 1,
            cropBottom - cropTop + 1
        )
    }

    private fun hasPrintableInk(pixel: Int): Boolean {
        val alpha = Color.alpha(pixel)
        val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
        return alpha > 40 && luminance < 245
    }

    private fun rasterImage(bitmap: Bitmap): ByteArray {
        val widthBytes = (bitmap.width + 7) / 8
        val imageBytes = ByteArrayOutputStream()
        imageBytes.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
        imageBytes.write(widthBytes and 0xFF)
        imageBytes.write((widthBytes shr 8) and 0xFF)
        imageBytes.write(bitmap.height and 0xFF)
        imageBytes.write((bitmap.height shr 8) and 0xFF)

        for (y in 0 until bitmap.height) {
            for (xByte in 0 until widthBytes) {
                var value = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < bitmap.width && bitmap.getPixel(x, y) == Color.BLACK) {
                        value = value or (0x80 shr bit)
                    }
                }
                imageBytes.write(value)
            }
        }
        return imageBytes.toByteArray()
    }

    private fun text(value: String) = value.toByteArray(Charsets.US_ASCII)
    private fun separator() = text(ReceiptFormatter.separatorLine())
    private fun lineFeed(count: Int) = "\n".repeat(count).toByteArray(Charsets.US_ASCII)
    private fun normal() = byteArrayOf(0x1B, 0x21, 0x00)
    private fun doubleHeight(value: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write(byteArrayOf(0x1B, 0x21, 0x10))
        bytes.write(text(value))
        bytes.write(normal())
        return bytes.toByteArray()
    }

    private fun doubleWidthHeight(value: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write(byteArrayOf(0x1B, 0x45, 0x01))
        bytes.write(byteArrayOf(0x1B, 0x21, 0x10))
        bytes.write(text(value))
        bytes.write(normal())
        bytes.write(byteArrayOf(0x1B, 0x45, 0x00))
        return bytes.toByteArray()
    }

    private fun emphasis(value: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write(byteArrayOf(0x1B, 0x45, 0x01))
        bytes.write(text(value))
        bytes.write(byteArrayOf(0x1B, 0x45, 0x00))
        return bytes.toByteArray()
    }

    companion object {
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val LOGO_WIDTH_DOTS = 220
        private const val FACEBOOK_ROW_WIDTH_DOTS = 240
        private const val FACEBOOK_ROW_HEIGHT_DOTS = 34
        private const val FACEBOOK_ICON_DOTS = 24
        private val INIT = byteArrayOf(0x1B, 0x40)
        private val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    }
}

object ReceiptFormatter {
    private const val LINE_WIDTH = 30
    private const val ITEM_WIDTH = 15
    private const val PRICE_WIDTH = 10
    private const val DETAIL_WIDTH = LINE_WIDTH - 4
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun buildPreview(receipt: ReceiptData, currency: String): String {
        return buildString {
            appendLine("[Centered Barrio Burrito logo]")
            appendLine(center("BARRIOBURRITO POS"))
            append(centerLine("Brgy. Fatima,"))
            append(centerLine("General Santos City"))
            appendLine(center("[f] Barrio Burrito"))
            append(separatorLine())
            append(metaLines(receipt))
            append(separatorLine())
            append(itemHeader())
            append(separatorLine())
            append(itemLines(receipt, currency))
            append(separatorLine())
            append(subtotalLines(receipt, currency))
            append(totalLine(receipt, currency))
            append(paymentLines(receipt, currency))
            append(separatorLine())
            appendLine(center("THANK YOU!"))
            appendLine(center("COME AGAIN SOON!"))
            appendLine(center("<3"))
        }
    }

    fun separatorLine(): String = "-".repeat(LINE_WIDTH) + "\n"

    fun centerLine(value: String): String = center(value) + "\n"

    fun metaLines(receipt: ReceiptData): String = buildString {
        val date = dateFormat.format(Date(receipt.dateTime))
        val time = timeFormat.format(Date(receipt.dateTime))
        appendLine("Date: $date")
        appendLine("Time: $time")
        appendLine("Order #: ${receipt.orderId.toString().padStart(6, '0')}")
        appendLine("Cashier: ${sanitize(receipt.cashierName).ifBlank { "Soykier" }}")
    }

    fun itemHeader(): String = buildString {
        appendLine("${"QTY".padStart(3)} ${"ITEM".padEnd(ITEM_WIDTH)} ${"PRICE".padStart(PRICE_WIDTH)}")
    }

    fun itemLines(receipt: ReceiptData, currency: String): String = buildString {
        receipt.items.forEach { item ->
            val itemChunks = wrapText(item.name, ITEM_WIDTH)
            appendLine(itemLine(item.quantity, itemChunks.firstOrNull().orEmpty(), money(item.subtotal, currency)))
            itemChunks.drop(1).forEach { chunk ->
                appendLine("    ${chunk.padEnd(ITEM_WIDTH).take(ITEM_WIDTH)}")
            }
            item.details?.lineSequence()
                ?.filter { it.isNotBlank() }
                ?.flatMap { wrapText(it, DETAIL_WIDTH) }
                ?.take(3)
                ?.forEach { appendLine("    ${it.take(DETAIL_WIDTH)}") }
        }
    }

    fun subtotalLines(receipt: ReceiptData, currency: String): String = buildString {
        appendLine(twoColumn("Subtotal", money(receipt.totalAmount, currency)))
        appendLine(twoColumn("Discount", money(0.0, currency)))
    }

    fun totalLine(receipt: ReceiptData, currency: String): String {
        return twoColumn("TOTAL", money(receipt.totalAmount, currency)) + "\n"
    }

    fun paymentLines(receipt: ReceiptData, currency: String): String = buildString {
        appendLine(labelValue("Payment Method", receipt.paymentMethod.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }))
        appendLine(labelValue("Amount Received", money(receipt.amountReceived, currency)))
        appendLine(labelValue("Change", money(receipt.changeAmount, currency)))
    }

    private fun itemLine(quantity: Int, name: String, price: String): String {
        val qty = quantity.toString().take(3).padStart(3)
        val item = sanitize(name).take(ITEM_WIDTH).padEnd(ITEM_WIDTH)
        val amount = sanitize(price).takeLast(PRICE_WIDTH).padStart(PRICE_WIDTH)
        return "$qty $item $amount"
    }

    private fun labelValue(label: String, value: String): String {
        val left = sanitize(label).take(15).padEnd(15)
        return "$left: ${sanitize(value).take(13)}"
    }

    fun twoColumn(left: String, right: String): String {
        val cleanLeft = sanitize(left)
        val cleanRight = sanitize(right)
        val spaces = (LINE_WIDTH - cleanLeft.length - cleanRight.length).coerceAtLeast(1)
        return cleanLeft + " ".repeat(spaces) + cleanRight
    }

    private fun center(value: String): String {
        val clean = sanitize(value).take(LINE_WIDTH)
        val leftPadding = ((LINE_WIDTH - clean.length) / 2).coerceAtLeast(0)
        return " ".repeat(leftPadding) + clean
    }

    private fun money(amount: Double, currency: String): String {
        return "P${"%.2f".format(amount)}"
        val printableCurrency = when {
            currency.contains("₱") || currency.contains("â") -> "PHP"
            currency.isBlank() -> "PHP"
            else -> sanitize(currency)
        }
        return "$printableCurrency${"%.2f".format(amount)}"
    }

    private fun wrapText(value: String, width: Int): List<String> {
        val words = sanitize(value).split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var current = ""

        words.forEach { word ->
            word.chunked(width).forEach { chunk ->
                current = when {
                    current.isBlank() -> chunk
                    current.length + 1 + chunk.length <= width -> "$current $chunk"
                    else -> {
                        lines += current
                        chunk
                    }
                }
            }
        }

        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun sanitize(value: String): String {
        return value
            .replace("₱", "PHP")
            .replace("×", "x")
            .replace("—", "-")
            .replace("–", "-")
            .replace("ñ", "n")
            .replace(Regex("[^\\x20-\\x7E]"), "")
    }
}
