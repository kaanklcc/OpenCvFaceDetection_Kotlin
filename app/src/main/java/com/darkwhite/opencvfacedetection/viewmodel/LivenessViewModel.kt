import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkwhite.opencvfacedetection.util.TFLiteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class LivenessResult(
    val isRealFace: Boolean,
    val score: Float
)

class LivenessViewModel : ViewModel() {
    private val _livenessResult = MutableLiveData<LivenessResult>()
    val livenessResult: LiveData<LivenessResult> = _livenessResult

    private var interpreter: Interpreter? = null
    private var inputImageWidth = 224
    private var inputImageHeight = 224
    private var isQuantized = true
    private var outputClasses = 1000

    // Son sıfırlama zamanı ve sayaç
    private var resetCounter = 0
    private val resetThreshold = 25

    // ImageNet sınıfları için kategoriler - genişletilmiş sahte/ekran listesi
    private val humanIndices = (400..500).toList()
    private val screenIndices = listOf(
        782, 783, 784, 785, 786, 787, 788, 789, 790, // Ekran/Monitör türleri - genişletilmiş
        713, 714, 715, 716, 717, 718, 719, 720,      // Bilgisayar donanımı - genişletilmiş
        650, 651, 652, 653, 654, 655,                // Mobil cihazlar - genişletilmiş
        722, 723, 724, 725, 726, 727, 728, 729, 730  // Kullanıcı arayüzü - genişletilmiş
    )

    // Tespit geçmişi - biraz daha uzun tutuyoruz
    private val detectionHistory = mutableListOf<Pair<Boolean, Long>>() // Boolean ve zaman damgası
    private val maxHistorySize = 5 // 3 yerine 5 - daha uzun geçmiş

    fun loadModel(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val modelFile = TFLiteHelper.loadModelFile(context, "efficientnet-lite3-int8.tflite")
                    val options = Interpreter.Options().apply {
                        setNumThreads(4)
                    }
                    interpreter = Interpreter(modelFile, options)

                    val inputTensor = interpreter?.getInputTensor(0)
                    val inputShape = inputTensor?.shape() ?: intArrayOf(1, 224, 224, 3)
                    inputImageHeight = inputShape[1]
                    inputImageWidth = inputShape[2]

                    val outputTensor = interpreter?.getOutputTensor(0)
                    val outputShape = outputTensor?.shape() ?: intArrayOf(1, 1000)
                    outputClasses = outputShape[1]

                    Log.d("LivenessViewModel", "Model loaded with input dimensions: ${inputImageWidth}x${inputImageHeight}")
                    Log.d("LivenessViewModel", "Output tensor type: ${outputTensor?.dataType()}, shape: ${outputShape.contentToString()}")
                } catch (e: Exception) {
                    Log.e("LivenessViewModel", "Failed to load EfficientNet model: ${e.message}", e)
                }
            }
        }
    }

    fun runLivenessDetection(faceBitmap: Bitmap) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                detectLiveness(faceBitmap)
            }
            _livenessResult.value = result
        }
    }

    private fun detectLiveness(faceBitmap: Bitmap): LivenessResult {
        if (interpreter == null) {
            Log.e("LivenessViewModel", "Interpreter not initialized")
            return LivenessResult(isRealFace = false, score = 0.0f)
        }

        try {
            val safeBitmap = ensureBitmapIsMutable(faceBitmap)
            val resizedBitmap = Bitmap.createScaledBitmap(
                safeBitmap, inputImageWidth, inputImageHeight, true
            )

            // Input buffer oluşturma
            val bytePerChannel = if (isQuantized) 1 else 4
            val inputBuffer = ByteBuffer.allocateDirect(
                1 * inputImageWidth * inputImageHeight * 3 * bytePerChannel
            )
            inputBuffer.order(ByteOrder.nativeOrder())

            if (isQuantized) {
                processInputBitmapQuantized(resizedBitmap, inputBuffer)
            } else {
                processInputBitmapFloat(resizedBitmap, inputBuffer)
            }

            val outputBuffer = ByteBuffer.allocateDirect(outputClasses * java.lang.Byte.BYTES)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Modeli çalıştır
            interpreter?.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val outputScores = ByteArray(outputClasses)
            outputBuffer.get(outputScores)

            val probabilities = outputScores.map { (it.toInt() and 0xFF) / 255.0f }

            // En yüksek sınıfları logla
            val topIndices = probabilities.indices.sortedByDescending { probabilities[it] }.take(10)
            Log.d("LivenessViewModel", "Top 10 classes: ${topIndices.joinToString { "$it: ${probabilities[it]}" }}")

            // Sınıf skorlarını hesapla
            val topHumanScore = topIndices.filter { it in humanIndices }
                .map { probabilities[it] }
                .maxOrNull() ?: 0f

            val topScreenScore = topIndices.filter { it in screenIndices }
                .map { probabilities[it] }
                .maxOrNull() ?: 0f

            // İyileştirilmiş imaj analizi
            val faceTexture = calculateTextureScore(resizedBitmap)
            val edgeSharpness = calculateEdgeSharpness(resizedBitmap)
            val moirePattern = detectMoirePattern(resizedBitmap) * 0.6f
            val colorConsistency = colorConsistencyAnalysis(resizedBitmap)
            val noisePattern = detectNoisePattern(resizedBitmap) * 0.5f

            // YENİ: Ekrandan fotoğraf çekimi için özel özellikler
            val blurLevel = detectBlurLevel(resizedBitmap)
            val lightingUniformity = detectLightingUniformity(resizedBitmap)

            // Doku skorunu iyileştir
            val adjustedTexture = maxOf(faceTexture, 0.15f)

            // İyileştirilmiş metrik ağırlıkları - insan yüzü lehine düzenlendi
            val humanScore = topHumanScore * 0.30f +           // 0.25 yerine 0.30
                    adjustedTexture * 0.15f +
                    edgeSharpness * 0.2f +
                    (1f - moirePattern) * 0.15f +
                    colorConsistency * 0.1f +
                    (1f - noisePattern) * 0.08f +      // 0.1 yerine 0.08
                    (1f - blurLevel) * 0.02f           // 0.05 yerine 0.02

            val screenScore = topScreenScore * 0.25f +         // 0.3 yerine 0.25
                    (1f - adjustedTexture) * 0.1f +
                    (1f - edgeSharpness) * 0.15f +
                    moirePattern * 0.2f +
                    (1f - colorConsistency) * 0.1f +
                    noisePattern * 0.1f +
                    lightingUniformity * 0.1f          // 0.05 yerine 0.1

            // YENİ: Zamanlayıcı bazlı sıfırlama mekanizması
            resetCounter++
            if (resetCounter >= resetThreshold) {
                detectionHistory.clear()
                resetCounter = 0
                Log.d("LivenessViewModel", "History reset after $resetThreshold frames")
            }

            // Çok daha sıkı karar mantığı
            val isRealFace = (humanScore > screenScore * 1.15f) && // 1.35'ten 1.15'e düşürüldü
                    (humanScore >= 0.40f) &&              // 0.45'ten 0.40'a düşürüldü
                    (screenScore < 0.40f) &&              // 0.35'ten 0.40'a yükseltildi
                    (moirePattern < 0.35f) &&             // 0.3'ten 0.35'e yükseltildi
                    (blurLevel < 0.75f)                   // 0.7'den 0.75'e yükseltildi

            // Şimdiki zamanı al
            val currentTime = System.currentTimeMillis()

            // Geçmişe ekle (zaman damgasıyla)
            detectionHistory.add(Pair(isRealFace, currentTime))
            if (detectionHistory.size > maxHistorySize) {
                detectionHistory.removeAt(0)
            }

            // ÇOK ÖNEMLİ: Geliştirilmiş son karar mantığı
            // 1. Şimdiki kare kesinlikle sahte ise, her zaman sahte de
            // 2. Gerçek ise, son 3 karenin 2'si gerçek VE son kare de gerçek olmalı
            val currentDecision = isRealFace

            // Son 1000ms (1 saniye) içindeki kararları say
            val recentTrueCount = detectionHistory.count {
                it.first && (currentTime - it.second < 1000)
            }

            // Kesinlikle sahte olup olmadığını kontrol et
            val definitelyFake = moirePattern > 0.5f || blurLevel > 0.85f || lightingUniformity > 0.85f

            // Son karar - daha esnek hale getirildi
            val finalDecision = if (definitelyFake) {
                false // Kesinlikle sahte özellikler varsa her zaman sahte
            } else if (!currentDecision) {
                // Şimdiki kare sahte diyorsa, geçmiş kareler çoğunlukla gerçek ise yine de gerçek kabul et
                val recentHistoryTrueRatio = if (detectionHistory.size > 0) {
                    detectionHistory.count { it.first } / detectionHistory.size.toFloat()
                } else 0f

                recentHistoryTrueRatio > 0.66f // Geçmiş karelerin 2/3'ü gerçek ise bu kareyi de gerçek kabul et
            } else {
                // Şimdiki kare gerçek diyorsa, daha esnek bir karar mantığı
                recentTrueCount >= 1 &&         // En az 1 gerçek kare yeterli (2 yerine)
                        moirePattern < 0.4f             // Moire için daha yüksek tolerans (0.3 yerine 0.4)
            }

            val confidenceScore = if (finalDecision) {
                val scoreDifference = humanScore - screenScore
                minOf(0.8f, humanScore * (1f + minOf(scoreDifference, 0.4f))) // 0.7 yerine 0.8 maksimum
            } else {
                minOf(0.85f, screenScore * 1.1f) // 0.9 yerine 0.85 maksimum, 1.2 yerine 1.1 çarpan
            }

            // Tüm metrikleri logla
            Log.d("LivenessViewModel", "Human: $humanScore, Screen: $screenScore, Blur: $blurLevel, Light: $lightingUniformity")
            Log.d("LivenessViewModel", "Texture: $adjustedTexture, EdgeSharp: $edgeSharpness, Moire: $moirePattern")
            Log.d("LivenessViewModel", "ColorCons: $colorConsistency, Noise: $noisePattern")
            Log.d("LivenessViewModel", "Current: $currentDecision, DefFake: $definitelyFake, RecentTrue: $recentTrueCount")
            Log.d("LivenessViewModel", "Final decision: $finalDecision, Confidence: $confidenceScore")

            return LivenessResult(isRealFace = finalDecision, score = maxOf(confidenceScore, 0.05f))
        } catch (e: Exception) {
            Log.e("LivenessViewModel", "Liveness detection error: ${e.message}", e)
            return LivenessResult(isRealFace = false, score = 0.0f)
        }
    }

    /**
     * Texture score calculation - improved for better screen detection
     */
    private fun calculateTextureScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalVariation = 0f
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val neighbors = listOf(
                    pixels[(y-1) * width + x],     // top
                    pixels[(y+1) * width + x],     // bottom
                    pixels[y * width + (x-1)],     // left
                    pixels[y * width + (x+1)]      // right
                )

                var variation = 0f
                for (neighbor in neighbors) {
                    val rDiff = abs(((center shr 16) and 0xFF) - ((neighbor shr 16) and 0xFF))
                    val gDiff = abs(((center shr 8) and 0xFF) - ((neighbor shr 8) and 0xFF))
                    val bDiff = abs((center and 0xFF) - (neighbor and 0xFF))
                    variation += (rDiff + gDiff + bDiff) / 3f
                }
                totalVariation += variation / neighbors.size
            }
        }

        val averageVariation = totalVariation / ((height - 2) * (width - 2))

        // Gerçek yüzlerde doku 10-25 aralığında olma eğiliminde - Aralığı genişlet
        return if (averageVariation > 6f && averageVariation < 30f) { // 8-25 aralığı yerine 6-30
            minOf(1f, averageVariation / 18f)                         // 20 yerine 18
        } else {
            // Çok düşük veya çok yüksek varyasyon (ekran veya aşırı gürültü)
            minOf(1f, averageVariation / 35f) * 0.6f                  // 30 yerine 35, 0.5 yerine 0.6
        }
    }

    /**
     * Edge sharpness calculation - screens often have sharper edges
     */
    private fun calculateEdgeSharpness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalGradient = 0f
        val sampleSize = 30  // Daha fazla örnek nokta

        for (i in 0 until sampleSize) {
            val y = (Math.random() * (height - 2)).toInt() + 1
            for (j in 0 until sampleSize) {
                val x = (Math.random() * (width - 2)).toInt() + 1

                val center = pixels[y * width + x]
                val right = pixels[y * width + (x+1)]
                val bottom = pixels[(y+1) * width + x]

                // Yatay gradyan
                val rDiffH = abs(((center shr 16) and 0xFF) - ((right shr 16) and 0xFF))
                val gDiffH = abs(((center shr 8) and 0xFF) - ((right shr 8) and 0xFF))
                val bDiffH = abs((center and 0xFF) - (right and 0xFF))

                // Dikey gradyan
                val rDiffV = abs(((center shr 16) and 0xFF) - ((bottom shr 16) and 0xFF))
                val gDiffV = abs(((center shr 8) and 0xFF) - ((bottom shr 8) and 0xFF))
                val bDiffV = abs((center and 0xFF) - (bottom and 0xFF))

                val gradient = (rDiffH + gDiffH + bDiffH + rDiffV + gDiffV + bDiffV) / 6f
                totalGradient += gradient
            }
        }

        val averageGradient = totalGradient / (sampleSize * sampleSize)

        // Yumuşak kenarlar için dönüştürülmüş skor
        return if (averageGradient > 30f) {
            // Çok yüksek gradyan - keskin kenarlar (ekran olabilir)
            0.3f
        } else {
            1f - minOf(1f, averageGradient / 25f)
        }
    }

    /**
     * Ekranlarda oluşabilecek moire (dalgalı) desenleri tespit et - düzeltilmiş versiyon
     */
    private fun detectMoirePattern(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var moireScore = 0f
        val patternSize = 6  // Daha büyük bir desen boyutu

        // Daha az yatay ve dikey tarama yap (performans için)
        val sampleStride = 3

        // Yatay moire desenleri ara
        for (y in 0 until height step sampleStride) {
            for (x in 0 until width - patternSize) {
                val patterns = mutableListOf<Int>()
                for (i in 0 until patternSize) {
                    patterns.add(pixels[y * width + x + i] and 0xFF) // Mavi kanal
                }

                // Ekranlar için daha spesifik desenleri ara
                // (ardışık yüksek/düşük yoğunlukta değişimler)
                var alternatingCount = 0
                var risingCount = 0
                var fallingCount = 0

                for (i in 1 until patterns.size) {
                    val diff = patterns[i] - patterns[i-1]
                    if (abs(diff) > 20) {  // Daha yüksek eşik - daha az hassas
                        alternatingCount++
                        if (diff > 0) risingCount++
                        else fallingCount++
                    }
                }

                // Daha sıkı bir örüntü şartı
                if (alternatingCount >= patternSize / 2 &&
                    (risingCount >= 2 && fallingCount >= 2)) {
                    moireScore += 0.08f  // Daha düşük katkı
                }
            }
        }

        // Dikey moire desenleri ara - benzer şekilde daha az hassas
        for (x in 0 until width step sampleStride) {
            for (y in 0 until height - patternSize) {
                val patterns = mutableListOf<Int>()
                for (i in 0 until patternSize) {
                    patterns.add(pixels[(y + i) * width + x] and 0xFF)
                }

                var alternatingCount = 0
                var risingCount = 0
                var fallingCount = 0

                for (i in 1 until patterns.size) {
                    val diff = patterns[i] - patterns[i-1]
                    if (abs(diff) > 20) {
                        alternatingCount++
                        if (diff > 0) risingCount++
                        else fallingCount++
                    }
                }

                if (alternatingCount >= patternSize / 2 &&
                    (risingCount >= 2 && fallingCount >= 2)) {
                    moireScore += 0.08f
                }
            }
        }

        // Skoru normalize et - daha düşük maksimum değer
        return minOf(1f, moireScore / 35f) // 30 yerine 35 - moire tespitini zorlaştır
    }

    /**
     * Renk tutarlılığını analiz eder - gerçek yüzlerde daha yumuşak geçişler olur
     */
    private fun colorConsistencyAnalysis(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Cilt tonu bölgelerini bul (yüz bölgesinde)
        val faceRegionY = height / 3
        val faceRegionHeight = height / 2
        val faceRegionX = width / 4
        val faceRegionWidth = width / 2

        val skinPixels = mutableListOf<Int>()

        for (y in faceRegionY until faceRegionY + faceRegionHeight) {
            for (x in faceRegionX until faceRegionX + faceRegionWidth) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Basit cilt tonu tespiti
                if (r > g && r > b && r > 80 && g > 40 && b > 20) {
                    skinPixels.add(pixel)
                }
            }
        }

        if (skinPixels.isEmpty()) {
            return 0.3f // Cilt tonu tespit edilemedi - şüpheli
        }

        // Renk varyasyonunu ölç
        var totalVariation = 0f
        val sampleSize = minOf(200, skinPixels.size)
        for (i in 0 until sampleSize) {
            val index1 = (Math.random() * skinPixels.size).toInt()
            val index2 = (Math.random() * skinPixels.size).toInt()

            val p1 = skinPixels[index1]
            val p2 = skinPixels[index2]

            val r1 = (p1 shr 16) and 0xFF
            val g1 = (p1 shr 8) and 0xFF
            val b1 = p1 and 0xFF

            val r2 = (p2 shr 16) and 0xFF
            val g2 = (p2 shr 8) and 0xFF
            val b2 = p2 and 0xFF

            // Öklid mesafesi
            val distance = sqrt(
                (r1 - r2).toDouble().pow(2) +
                        (g1 - g2).toDouble().pow(2) +
                        (b1 - b2).toDouble().pow(2)
            ).toFloat()

            totalVariation += distance
        }

        val averageVariation = totalVariation / sampleSize

        // Gerçek yüzlerde orta düzeyde renk varyasyonu olur (10-30 arası)
        // Çok düşük = düz renk (ekran), çok yüksek = gürültü
        return if (averageVariation > 8f && averageVariation < 35f) {
            minOf(1f, (averageVariation - 5f) / 25f)
        } else {
            0.4f
        }
    }

    /**
     * Ekran gürültüsü tespiti - ekranlarda düzenli piksel gürültüsü olabilir
     */
    private fun detectNoisePattern(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var regularPatternCount = 0
        val blockSize = 8

        // Görüntüyü bloklara böl ve her bloğu analiz et
        for (yBlock in 0 until height / blockSize) {
            for (xBlock in 0 until width / blockSize) {
                val startX = xBlock * blockSize
                val startY = yBlock * blockSize

                // Blok içindeki piksellerdeki düzenli desenleri kontrol et
                val patterns = mutableMapOf<Int, Int>() // diff -> count

                for (y in startY until minOf(startY + blockSize, height) - 1) {
                    for (x in startX until minOf(startX + blockSize, width) - 1) {
                        val pixel = pixels[y * width + x]
                        val nextPixel = pixels[y * width + x + 1]

                        // Renk kanalları arasındaki fark
                        val rDiff = abs(((pixel shr 16) and 0xFF) - ((nextPixel shr 16) and 0xFF))
                        val gDiff = abs(((pixel shr 8) and 0xFF) - ((nextPixel shr 8) and 0xFF))
                        val bDiff = abs((pixel and 0xFF) - (nextPixel and 0xFF))

                        val avgDiff = (rDiff + gDiff + bDiff) / 3
                        patterns[avgDiff] = patterns.getOrDefault(avgDiff, 0) + 1
                    }
                }

                // En yaygın farklılığı bul
                val mostCommon = patterns.maxByOrNull { it.value }

                // Eğer belirli bir fark çok yaygınsa (düzenli gürültü) bu bir ekran olabilir
                if (mostCommon != null && mostCommon.value > (blockSize * blockSize * 0.4)) {
                    regularPatternCount++
                }
            }
        }

        val totalBlocks = (width / blockSize) * (height / blockSize)
        return minOf(1f, regularPatternCount.toFloat() / (totalBlocks * 0.3f))
    }

    /**
     * YENI: İyileştirilmiş bulanıklık seviyesi tespiti - daha hoşgörülü
     */
    private fun detectBlurLevel(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalGradient = 0f
        val sampleStride = 2 // Performans için her piksel yerine her 2 pikselde bir ölç

        for (y in 1 until height - 1 step sampleStride) {
            for (x in 1 until width - 1 step sampleStride) {
                val centerIdx = y * width + x
                val center = pixels[centerIdx]

                // Yatay ve dikey komşular
                val left = pixels[centerIdx - 1]
                val right = pixels[centerIdx + 1]
                val top = pixels[(y-1) * width + x]
                val bottom = pixels[(y+1) * width + x]

                // Gri tonlamaya dönüştür (sadece tek kanala ihtiyacımız var)
                val centerGray = (((center shr 16) and 0xFF) + ((center shr 8) and 0xFF) + (center and 0xFF)) / 3
                val leftGray = (((left shr 16) and 0xFF) + ((left shr 8) and 0xFF) + (left and 0xFF)) / 3
                val rightGray = (((right shr 16) and 0xFF) + ((right shr 8) and 0xFF) + (right and 0xFF)) / 3
                val topGray = (((top shr 16) and 0xFF) + ((top shr 8) and 0xFF) + (top and 0xFF)) / 3
                val bottomGray = (((bottom shr 16) and 0xFF) + ((bottom shr 8) and 0xFF) + (bottom and 0xFF)) / 3

                // Yatay ve dikey gradyanları hesapla
                val horizontalGradient = abs(rightGray - leftGray)
                val verticalGradient = abs(bottomGray - topGray)

                // Toplam gradyanı ekle (ne kadar büyük olursa o kadar keskin)
                totalGradient += (horizontalGradient + verticalGradient) / 2f
            }
        }

        // Normalize et: düşük değer = keskin (düşük bulanıklık)
        val avgGradient = totalGradient / ((width / sampleStride) * (height / sampleStride))

        // Bulanıklık seviyesi (1 - normalize gradyan, 0-1 arası)
        // Düşük değerler keskin, yüksek değerler bulanık - daha hoşgörülü
        return (1f - minOf(1f, avgGradient / 30f)) * 0.9f // 25 yerine 30, %10 azaltma faktörü
    }

    /**
     * YENI: İyileştirilmiş aydınlatma tekdüzeliği tespiti
     */
    private fun detectLightingUniformity(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val brightnessValues = mutableListOf<Int>()
        val blockSize = 16 // Performans için bloklar halinde analiz et

        // Örnek noktalar alarak parlaklık değerlerini topla
        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Parlaklık: 0.2126*R + 0.7152*G + 0.0722*B (özel bir ağırlıklı ortalama)
                val brightness = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt()
                brightnessValues.add(brightness)
            }
        }

        if (brightnessValues.isEmpty()) return 0.5f

        // Standart sapmayı hesapla
        val mean = brightnessValues.average()
        val variance = brightnessValues.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        // Normalize et: düşük standart sapma = tekdüze aydınlatma (ekran) = yüksek değer
        // Normal fotoğraflarda parlaklık varyasyonu daha fazladır = düşük değer
        return (1f - minOf(1f, (stdDev / 45).toFloat())) * 0.9f // 40 yerine 45, %10 azaltma faktörü
    }

    // Process bitmap functions remain the same
    private fun processInputBitmapQuantized(bitmap: Bitmap, inputBuffer: ByteBuffer) {
        // Mevcut kodun aynısı
        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        inputBuffer.rewind()
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val pixelValue = intValues[i * inputImageWidth + j]
                inputBuffer.put((pixelValue shr 16 and 0xFF).toByte())
                inputBuffer.put((pixelValue shr 8 and 0xFF).toByte())
                inputBuffer.put((pixelValue and 0xFF).toByte())
            }
        }
    }

    private fun processInputBitmapFloat(bitmap: Bitmap, inputBuffer: ByteBuffer) {
        // Mevcut kodun aynısı
        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        inputBuffer.rewind()
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val pixelValue = intValues[i * inputImageWidth + j]
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)
                inputBuffer.putFloat((r / 127.5f) - 1)
                inputBuffer.putFloat((g / 127.5f) - 1)
                inputBuffer.putFloat((b / 127.5f) - 1)
            }
        }
    }

    private fun ensureBitmapIsMutable(bitmap: Bitmap): Bitmap {
        // Mevcut kodun aynısı
        if (!bitmap.isMutable() || needsBitmapConversion(bitmap)) {
            Log.d("LivenessViewModel", "Converting bitmap to ARGB_8888")
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        return bitmap
    }

    private fun needsBitmapConversion(bitmap: Bitmap): Boolean {
        // Mevcut kodun aynısı
        val config = bitmap.config ?: return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return config.name == "HARDWARE"
        }
        return config != Bitmap.Config.ARGB_8888
    }

    override fun onCleared() {
        super.onCleared()
        interpreter?.close()
    }
}