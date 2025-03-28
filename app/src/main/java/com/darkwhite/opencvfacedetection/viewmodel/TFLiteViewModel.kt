package com.darkwhite.opencvfacedetection.viewmodel

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class TFLiteViewModel : ViewModel() {

    //kaan

    private val _resultMessage = MutableLiveData<String?>()
    val resultMessage: LiveData<String?> = _resultMessage

    private val _denemeHakki = MutableLiveData(0)
    val denemeHakki: LiveData<Int> = _denemeHakki


    fun setResultMessage(message: String) {
        _resultMessage.value = message
    }


    fun clearResponseMessage() {
        _resultMessage.value = null
    }
    fun increaseDenemeHakki() {
        _denemeHakki.value = (_denemeHakki.value ?: 0) + 1
    }

    fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 112 // MobileFaceNet genellikle 112x112 boyutlarında çalışır
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 4 byte (Float32) * width * height * 3 (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixelIndex = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[pixelIndex++]

                // Normalize ve ByteBuffer'a ekle
                byteBuffer.putFloat((((pixelValue shr 16) and 0xFF) / 127.5f) - 1.0f) // R
                byteBuffer.putFloat((((pixelValue shr 8) and 0xFF) / 127.5f) - 1.0f)  // G
                byteBuffer.putFloat(((pixelValue and 0xFF) / 127.5f) - 1.0f)         // B
            }
        }
        return byteBuffer
    }
    fun recognizeFace(bitmap: Bitmap, tflite: Interpreter): FloatArray? {
        val inputByteBuffer = bitmapToByteBuffer(bitmap)

        // Çıkışı 2 boyutlu array olarak tanımlıyoruz
        //val outputFeatureVector = Array(1) { FloatArray(512) }
        val outputFeatureVector = Array(1) { FloatArray(512) }
        tflite.run(inputByteBuffer, outputFeatureVector)
        return outputFeatureVector[0]



        // Modeli çalıştır
        tflite.run(inputByteBuffer, outputFeatureVector)

        Log.d("TFLite Output", "TFLite Output: ${outputFeatureVector[0].size}, Values: ${outputFeatureVector[0].joinToString()}")

        Log.d("Model Output", "Çıkış Vektörü Uzunluğu: ${outputFeatureVector[0].size}")
        Log.d("TFLite Output Embeddings", "Face Embeddings: ${outputFeatureVector[0].joinToString()}")

        return outputFeatureVector[0] // İlk elemanı döndür
    }


    fun calculateEuclideanDistance(vector1: FloatArray, vector2: FloatArray): Float {
        var sum = 0.0f
        for (i in vector1.indices) {
            sum += Math.pow((vector1[i] - vector2[i]).toDouble(), 2.0).toFloat()
        }
        return Math.sqrt(sum.toDouble()).toFloat()
    }

    fun compareFaces(face1Bitmap: Bitmap, face2Bitmap: Bitmap, interpreter: Interpreter): Boolean {
        val face1Features = recognizeFace(face1Bitmap, interpreter)
        val face2Features = recognizeFace(face2Bitmap, interpreter)

        if (face1Features != null && face2Features != null) {
            val distance = calculateEuclideanDistance(face1Features, face2Features)

            Log.d("FaceRecognition", "Euclidean Distance: $distance")
            return distance < 1.0f // Karşılaştırma eşiği
        }
        return false
    }

    // Yeni bir fonksiyon ekle - benzerlik skorunu döndürür
    fun getFaceSimilarity(face1: Bitmap, face2: Bitmap, interpreter: Interpreter): Float {
        // Her iki yüz için embedding vektörlerini hesapla
        val embedding1 = getFaceEmbedding(face1, interpreter)
        val embedding2 = getFaceEmbedding(face2, interpreter)

        // İki vektör arasındaki benzerliği hesapla (kosinüs benzerliği)
        return calculateCosineSimilarity(embedding1, embedding2)
    }

    // MobileFaceNet'ten yüz embedding'i elde et
    /*private fun getFaceEmbedding(face: Bitmap, interpreter: Interpreter): FloatArray {
        // Görüntüyü ön işle
        val inputImage = preprocessImage(face)

        // Output tanımla (512 boyutlu embedding vektörü için)
        val outputEmbedding = Array(1) { FloatArray(512) }

        // Inference çalıştır
        interpreter.run(inputImage, outputEmbedding)

        // Embedding vektörünü normalize et
        val embedding = outputEmbedding[0]
        val norm = sqrt(embedding.map { it * it }.sum())

        // L2 normalizasyonu uygula
        return embedding.map { it / norm }.toFloatArray()
    }*/

    fun getFaceEmbedding(face: Bitmap, interpreter: Interpreter): FloatArray {
        val inputImage = preprocessImage(face)
        val outputEmbedding = Array(1) { FloatArray(512) }

        val startTime = SystemClock.elapsedRealtime() // Başlangıç zamanı
        interpreter.run(inputImage, outputEmbedding)
        val endTime = SystemClock.elapsedRealtime() // Bitiş zamanı

        val processingTime = endTime - startTime
        Log.d("TFLite", "Model çalıştırma süresi: $processingTime ms")

        return outputEmbedding[0]
    }


    // Görüntüyü ön işleme
    private fun preprocessImage(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        // MobileFaceNet için 112x112 RGB görüntü
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 112, 112, true)

        // Görüntüyü normalizasyon için RGB float dizisine dönüştür
        val intValues = IntArray(112 * 112)
        resizedBitmap.getPixels(intValues, 0, 112, 0, 0, 112, 112)

        // Model girişi için 1x112x112x3 boyutlu dizi oluştur
        val inputBuffer = Array(1) {
            Array(112) {
                Array(112) {
                    FloatArray(3)
                }
            }
        }

        // Pikselleri [0,1] aralığına normalize et ve RGB değerlerini ayrı ayrı kaydet
        for (i in 0 until 112 * 112) {
            val pixel = intValues[i]

            // BGR sırasında (OpenCV ile uyumlu olması için)
            inputBuffer[0][i / 112][i % 112][0] = ((pixel shr 16) and 0xFF) / 255.0f
            inputBuffer[0][i / 112][i % 112][1] = ((pixel shr 8) and 0xFF) / 255.0f
            inputBuffer[0][i / 112][i % 112][2] = (pixel and 0xFF) / 255.0f
        }

        return inputBuffer
    }

    // Kosinüs benzerliğini hesapla
    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }


        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
}