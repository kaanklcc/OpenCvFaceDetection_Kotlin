package com.darkwhite.opencvfacedetection.util

import android.content.Context
import android.util.Log
import com.darkwhite.opencvfacedetection.R
import org.opencv.objdetect.CascadeClassifier
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TFLiteHelper {
    private const val TAG = "TFLiteHelper"

    /*fun loadModelFile(context: Context, modelName: String = "mobileFaceNet.tflite"): MappedByteBuffer? {
        return try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            Log.d("yüklendi", "Model başarıyla yüklendi: $modelName") // Model yüklendi mesajı
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("yüklenmedi", "Model yüklenirken hata oluştu: ${e.localizedMessage}") //  Hata mesajı
            null
        }
    }*/

    fun loadModelFile(context: Context, modelName: String = "mobileFaceNet.tflite"): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        Log.d("ModelYükleme", "Model başarıyla yüklendi: $modelName") // Log ekledik
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun loadCascadeFile(context: Context, filename: String): CascadeClassifier? {
        try {
            val inputStream = context.assets.open(filename)
            val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
            val cascadeFile = File(cascadeDir, filename)
            val outputStream = FileOutputStream(cascadeFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Log.d("kaskadeyüklendi", "Model başarıyla yüklendi: $filename") // Log ekledik
            return CascadeClassifier(cascadeFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}