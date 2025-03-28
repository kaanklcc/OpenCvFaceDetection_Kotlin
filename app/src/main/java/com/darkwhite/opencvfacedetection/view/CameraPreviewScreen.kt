package com.darkwhite.opencvfacedetection.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.darkwhite.opencvfacedetection.R

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("RememberReturnType", "SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(navController: NavController) {
    val context = LocalContext.current
    val scaffoldState = rememberBottomSheetScaffoldState()
    val mrzResult = remember { mutableStateOf("") } // MRZ sonucu burada tutulacak
    val mrzBuffer = remember { mutableStateListOf<String>() } // MRZ sonuçlarını geçici olarak tutacak
    val isMRZDetected = remember { mutableStateOf(false) } // MRZ'nin tespit edilip edilmediğini kontrol etmek için

    //kamera kontrolleri ve görsel yakalama analiz etme
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
        }
    }



    //Mlkit ile mrz ve text tanıma işlemi burada yapılıyor.
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
        processMRZImage(imageProxy, textRecognizer) { mrzText -> //kamera görüntüsü alınıyor ve tanınan görüntü mrzText olarak dönüyor
            if (mrzText.isNotEmpty() && !isMRZDetected.value) {
                //tanınan mrz geçici olarak mrzBuffere ekleniyor.
                mrzBuffer.add(mrzText)

                // Eğer mrz arka arakya 2 defa okunmuşsa doğru kabul edilir.
                //bu mrzResulta aktarılır.
                //mrzResulttaki değer isMrzDetectede aktarılır. Son ve kalıcı mrz budur.
                if (mrzBuffer.count { it == mrzText } >= 2) {
                    mrzResult.value = mrzText
                    isMRZDetected.value = true // Artık doğru MRZ tespit edildi
                    Log.d("MRZReader", "Sabitledi MRZ: $mrzText")
                }

                //Bufferde 5ten fazka metin varsa sırayla sil.
                if (mrzBuffer.size > 5) {
                    mrzBuffer.removeAt(0)
                }
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {

        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            CameraPreview(
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )


            if (mrzResult.value.isNotEmpty()) { //mrzResult boş değilse döndür.






                LaunchedEffect(mrzResult.value) { //mrz resulttaki değer alındığında sadece bir kez çalışan kod
                    //ismrzDetectedi de true döndürdüğümüz için artık birdaha bu kod çalışmayacak
                    val encodedMRZ = URLEncoder.encode(mrzResult.value, StandardCharsets.UTF_8.toString())
                    navController.navigate("authentication?mrz=$encodedMRZ") {
                        popUpTo("cameraPreview"){inclusive=true}
                    }

                    isMRZDetected.value = true  //artık mrz işlenmez
                }
            }



        }

        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.idphoto),
                contentDescription = "idcard",
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .height(400.dp),// %90 genişlikte olacak, ihtiyaca göre değiştirebilirsin.
                // .aspectRatio(3f / 4f),
                contentScale = ContentScale.Fit
                // En-boy oranını belirler. ID kartları genellikle 2:3 oranındadır.
            )

        }

        IconButton(
            onClick = { //icon varsayılan olarak arka kamera başlar basınca ön kameraya geçer.
                controller.cameraSelector =
                    if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
            },
            modifier = Modifier
                .offset(16.dp, 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
            )

        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
fun processMRZImage(
    imageProxy: ImageProxy,
    textRecognizer: TextRecognizer,
    onTextDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image // kameradan gelen görüntü erişimi sağlar.
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val mrzText = visionText.textBlocks
                    .joinToString("\n") { it.text }
                    .filterMRZ()

                Log.d("MRZReader", "Tespit edilen MRZ Metni: $mrzText")


                if (mrzText.isNotEmpty()) {
                    onTextDetected(mrzText) // MRZ sonucunu gönder
                }
            }
            .addOnFailureListener { e ->
                Log.e("MRZReader", "Error reading MRZ: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        Log.e("MRZReader", "ImageProxy.image is null")
        imageProxy.close()
    }
}


fun String.filterMRZ(): String {
    val lines = this.split("\n")
        .map { it.replace(" ", "").trim() } // Boşlukları temizle
        .filter { it.length in 30..44 }

    return when {
        // Eğer MRZ 3 satırsa ve ID kartı regexine uyuyorsa, ID kartı olarak işle
        lines.size == 3 &&
                Regex(MRZRegex.ID_CARD_TD_1_LINE_1_REGEX).matches(lines[0]) &&
                Regex(MRZRegex.ID_CARD_TD_1_LINE_2_REGEX).matches(lines[1]) &&
                Regex(MRZRegex.ID_CARD_TD_1_LINE_3_REGEX).matches(lines[2]) -> {
            lines.joinToString("\n").trim()
        }

        // Eğer MRZ 2 satırsa ve pasaport regexine uyuyorsa, pasaport olarak işle
        lines.size == 2 &&
                Regex(MRZRegex.PASSPORT_TD_3_LINE_1_REGEX).matches(lines[0]) &&
                Regex(MRZRegex.PASSPORT_TD_3_LINE_2_REGEX).matches(lines[1]) -> {
            lines.joinToString("\n").trim()
        }

        // Eğer ilk satırın başında "P" harfi varsa, pasaport olarak kabul et
        lines.isNotEmpty() && lines[0].startsWith("P") -> {
            lines.joinToString("\n").trim()
        }

        else -> "" // Eğer uygun format bulunamazsa boş döndür
    }
}


object MRZRegex {


    const val ID_CARD_TD_1_LINE_1_REGEX = "([A|C|I][A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{25})"
    const val ID_CARD_TD_1_LINE_2_REGEX ="([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9]{1})"
    const val ID_CARD_TD_1_LINE_3_REGEX =  "([A-Z0-9<]{30})"


    const val PASSPORT_TD_3_LINE_1_REGEX = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})"
    const val PASSPORT_TD_3_LINE_2_REGEX = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9<]{1})([0-9]{1})"



}