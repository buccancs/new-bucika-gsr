package com.topdon.commons.util

import java.io.*

class UnicodeReader(inputStream: InputStream, private val defaultEnc: String?) : Reader() {
    
    private val internalIn: PushbackInputStream = PushbackInputStream(inputStream, BOM_SIZE)
    private var internalIn2: InputStreamReader? = null
    
    companion object {
        private const val BOM_SIZE = 4
    }

    fun getDefaultEncoding(): String? = defaultEnc

    fun getEncoding(): String? = internalIn2?.encoding

    private fun init() {
        if (internalIn2 != null) return

        val bom = ByteArray(BOM_SIZE)
        val n = internalIn.read(bom, 0, bom.size)

        val (encoding, unread) = when {
            n >= 4 && bom[0] == 0x00.toByte() && bom[1] == 0x00.toByte() && 
            bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte() -> "UTF-32BE" to (n - 4)
            
            n >= 4 && bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() && 
            bom[2] == 0x00.toByte() && bom[3] == 0x00.toByte() -> "UTF-32LE" to (n - 4)
            
            n >= 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && 
            bom[2] == 0xBF.toByte() -> "UTF-8" to (n - 3)
            
            n >= 2 && bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte() -> "UTF-16BE" to (n - 2)
            
            n >= 2 && bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() -> "UTF-16LE" to (n - 2)
            
            else -> defaultEnc to n
        }

        if (unread > 0) {
            internalIn.unread(bom, n - unread, unread)
        }

        internalIn2 = if (encoding == null) {
            InputStreamReader(internalIn)
        } else {
            InputStreamReader(internalIn, encoding)
        }
    }

    override fun close() {
        init()
        internalIn2?.close()
    }

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        init()
        return internalIn2?.read(cbuf, off, len) ?: -1
    }
}