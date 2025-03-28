package com.darkwhite.opencvfacedetection.view

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.Context
import android.util.Log
import java.io.FileInputStream
import java.io.IOException

class FaceRecognitionModel(context: Context) {
    private var tflite: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    init {
        try {
            // Modeli belleğe yükle
            val tfliteModel = loadModelFile(context, "mobileFaceNet.tflite")

            // GPU Delegate oluştur
            gpuDelegate = GpuDelegate()
            val options = Interpreter.Options().apply {
                addDelegate(gpuDelegate)  // GPU kullanımı
                setNumThreads(4)  // Çoklu iş parçacığı (opsiyonel)
            }

            // Interpreter başlat
            tflite = Interpreter(tfliteModel, options)
            Log.d("TFLite", "GPU delegate başarıyla eklendi!")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("TFLite", "GPU delegate eklenemedi: ${e.message}")
        }catch (e: IOException) {
            e.printStackTrace()
            Log.e("TFLite", "TensorFlow Lite hata verdi: ${e.message}")
        }
    }

    fun getInterpreter(): Interpreter? {
        return tflite
    }

    fun close() {
        tflite?.close()
        gpuDelegate?.close()
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }
}
