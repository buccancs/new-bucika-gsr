package com.topdon.commons.util

object PDFUtils {
    fun getPdfName(name: String): String {
        return name
            .replace('+', '-')
            .replace(' ', '-')
            .replace('/', '-')
            .replace('?', '-')
            .replace('%', '-')
            .replace('#', '-')
            .replace('&', '-')
            .replace('=', '-')
            .replace('\\', '-')
            .replace(':', '-')
            .replace('*', '-')
            .replace('|', '-')
            .replace('<', '-')
            .replace('>', '-')
            .replace('"', '-')
    }
}
