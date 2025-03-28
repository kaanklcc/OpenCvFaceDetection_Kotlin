package com.darkwhite.opencvfacedetection.util

import java.io.InputStream

data class ImageInfo(
    val mimeType: String,  // Resmin MIME tipi
    val imageLength: Int,  // Resmin uzunluğu (byte cinsinden)
    val imageInputStream: InputStream  // Resmin byte'larını içeren InputStream
)

