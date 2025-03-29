package com.darkwhite.opencvfacedetection.view

import LivenessViewModel
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

import android.util.Base64
import java.io.ByteArrayOutputStream

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.darkwhite.opencvfacedetection.R
import com.darkwhite.opencvfacedetection.model.NFCViewModel
import com.darkwhite.opencvfacedetection.ui.theme.anaRenkMavi
import com.darkwhite.opencvfacedetection.util.TFLiteHelper.loadCascadeFile
import com.darkwhite.opencvfacedetection.util.TFLiteHelper.loadModelFile
import com.darkwhite.opencvfacedetection.viewmodel.AuthenticationViewModel
import com.darkwhite.opencvfacedetection.viewmodel.FaceScreenViewModel
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel
import com.darkwhite.opencvfacedetection.viewmodel.TFLiteViewModel
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier

import org.tensorflow.lite.Interpreter

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min


@Composable
fun FaceScreen(
    navController: NavController,
    faceScreenViewModel: FaceScreenViewModel,
    authenticationViewModel: AuthenticationViewModel,
    nfcViewModel: NFCViewModel = viewModel(),
    matchViewModel: MatchViewModel,
    tfLiteViewModel: TFLiteViewModel
) {
    var resultMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
        }
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val personDetails = nfcViewModel.personDetails.observeAsState()
    val responseMessage = matchViewModel.responseMessage.observeAsState()
    var base64Image by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var detectedFace by remember { mutableStateOf<Bitmap?>(null) }
    var detectedFaceNfc by remember { mutableStateOf<Bitmap?>(null) }
    var faceMatchScore by remember { mutableStateOf<Float?>(null) }
    var faceMatchThreshold = 0.7f

    val livenessViewModel: LivenessViewModel = viewModel()
    val livenessResult = livenessViewModel.livenessResult.observeAsState()

    LaunchedEffect(Unit) {
        livenessViewModel.loadModel(context)
    }

    LaunchedEffect(responseMessage.value) {
        when (responseMessage.value) {
            "EŞLEŞTİRME BAŞARILI",
            "EŞLEŞTİRME BAŞARISIZ",
            "MANİPÜLASYON ALGILANDI" -> {
                navController.navigate("faceVerification") {
                    popUpTo("faceScreen") { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = { navController.navigate("faceVerification") },
            modifier = Modifier.offset(16.dp, 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIos,
                contentDescription = "Geri Dön",
            )
        }

        Box(
            modifier = Modifier
                .size(width = 230.dp, height = 310.dp)
                .align(Alignment.Center)
                .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(100.dp))
        )

        Button(
            onClick = {
                isProcessing = true
                capturePhoto(context, controller) { uri ->
                    capturedImageUri = uri
                    capturedImageUri?.let { safeUri ->
                        try {
                            val originalBitmap = uriToBitmap(context, safeUri)

                            detectedFace = originalBitmap?.let { detectFace(it, context) }

                            if (detectedFace == null) {
                                resultMessage = "Yüz tespit edilemedi, lütfen tekrar deneyin"
                                isProcessing = false
                                return@let
                            }

                            // Run liveness detection on the detected face
                            detectedFace?.let { faceBitmap ->
                                livenessViewModel.runLivenessDetection(faceBitmap)
                                Log.d("LivenessCheck", "Liveness detection triggered on detected face")
                            }

                            val resizedBitmap = detectedFace?.let { resizeBitmap(it, 224, 224) }
                            base64Image = resizedBitmap?.let { bitmapToBase64(it) }
                            faceScreenViewModel.base64Image.value = base64Image ?: ""

                            personDetails.value?.let { details ->
                                details.faceImage?.let { nfcFaceBitmapOriginal ->
                                    detectedFaceNfc = detectFace(nfcFaceBitmapOriginal, context)

                                    if (detectedFaceNfc == null) {
                                        resultMessage = "Kimlik kartında yüz tespit edilemedi"
                                        isProcessing = false
                                        return@let
                                    }

                                    val nfcFaceBitmap = detectedFaceNfc!!.copy(Bitmap.Config.ARGB_8888, true)
                                    val selfieBitmap = detectedFace!!.copy(Bitmap.Config.ARGB_8888, true)

                                    val modelFile = loadModelFile(context, "mobileFaceNet.tflite")
                                    val interpreter = Interpreter(modelFile)

                                    val similarity = tfLiteViewModel.getFaceSimilarity(nfcFaceBitmap, selfieBitmap, interpreter)
                                    faceMatchScore = similarity

                                    if (similarity > faceMatchThreshold) {
                                        resultMessage = "Yüzler eşleşiyor (Benzerlik: ${String.format("%.2f", similarity)})"
                                        Log.d("FaceRecognition", "Yüzler eşleşiyor! Skor: $similarity")
                                    } else {
                                        resultMessage = "Yüzler eşleşmiyor (Benzerlik: ${String.format("%.2f", similarity)})"
                                        Log.d("FaceRecognition", "Yüzler eşleşmiyor! Skor: $similarity")
                                    }

                                    interpreter.close()
                                } ?: run {
                                    resultMessage = "Kimlik kartında yüz verisi bulunamadı"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FaceScreen", "Yüz işleme hatası: ${e.message}", e)
                            resultMessage = "Hata: ${e.message}"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(anaRenkMavi),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Text(stringResource(R.string.FotoğrafÇek))
        }

        Text(
            text = resultMessage,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
            color = when {
                resultMessage.contains("eşleşiyor") -> Color.Green
                resultMessage.contains("eşleşmiyor") -> Color.Red
                else -> Color.White
            }
        )

        AnimatedVisibility(
            visible = isProcessing,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.YüzAlgılanıyor___),
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 50.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.SpaceAround, horizontalAlignment = Alignment.CenterHorizontally) {
            detectedFace?.let {
                FacePreviewScreen(detectedFace = it)
            }

            detectedFaceNfc?.let {
                FacePreviewScreen(detectedFace = it)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                detectedFaceNfc?.let {
                    Text("Kimlik Yüzü", fontSize = 16.sp, color = Color.White)
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Kimlik Yüzü",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Green, CircleShape)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                detectedFace?.let {
                    Text("Selfie Yüzü", fontSize = 16.sp, color = Color.White)
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Selfie Yüzü",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Green, CircleShape)
                    )
                }
            }
        }

        faceMatchScore?.let { score ->
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Benzerlik Skoru: ${String.format("%.2f", score)}",
                    fontSize = 18.sp,
                    color = if (score > faceMatchThreshold) Color.Green else Color.Red
                )
            }
        }

        livenessResult.value?.let { result ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(
                        color = if (result.isRealFace) Color(0x880A6E00) else Color(0x88C80000),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (result.isRealFace) "Gerçek Yüz Tespit Edildi ✅" else "Sahte Yüz Tespit Edildi ❌",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Text(
                        text = "Canlılık Skoru: ${String.format("%.2f", result.score)}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun capturePhoto(context: Context, controller: LifecycleCameraController, onImageCaptured: (Uri) -> Unit) {
    val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    controller.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onImageCaptured(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Fotoğraf çekme hatası: ${exception.message}", exception)
            }
        }
    )
}

fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun resizeBitmap(bitmap: Bitmap, targetWidth: Int = 300, targetHeight: Int = 400): Bitmap {
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun detectFace(bitmap: Bitmap, context: Context): Bitmap? {
    Log.d("OpenCV", "Yüz algılama başlatıldı")
    if (bitmap.isRecycled) {
        Log.e("OpenCV", "Bitmap is null or recycled!")
        return null
    }

    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val mat = Mat()
    Utils.bitmapToMat(mutableBitmap, mat)

    val faceDetector = loadCascadeFile(context, "haarcascade_frontalface_default.xml")
    if (faceDetector == null) {
        Log.e("OpenCV", "Cascade classifier could not be loaded!")
        return null
    }

    val grayMat = Mat()
    Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

    Imgproc.equalizeHist(grayMat, grayMat)

    val faces = MatOfRect()
    faceDetector.detectMultiScale(
        grayMat,
        faces,
        1.1,
        3,
        0,
        Size(30.0, 30.0),
        Size()
    )

    val faceArray = faces.toArray()

    if (faceArray.isEmpty()) {
        Log.e("Face Detection", "Yüz tespit edilemedi!")
        return null
    }

    var largestFace = faceArray[0]
    var maxArea = largestFace.width * largestFace.height

    for (face in faceArray) {
        val area = face.width * face.height
        if (area > maxArea) {
            maxArea = area
            largestFace = face
        }
    }

    val x = max(0, largestFace.x - (largestFace.width * 0.1).toInt())
    val y = max(0, largestFace.y - (largestFace.height * 0.1).toInt())
    val width = min(bitmap.width - x, (largestFace.width * 1.2).toInt())
    val height = min(bitmap.height - y, (largestFace.height * 1.2).toInt())

    val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
    return Bitmap.createScaledBitmap(croppedBitmap, 224, 224, true)
}

@Composable
fun FacePreviewScreen(detectedFace: Bitmap?) {
    var faceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(detectedFace) {
        faceBitmap = detectedFace
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Algılanan Yüz", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        faceBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Detected Face",
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.Red, CircleShape)
            )
        } ?: Text("Yüz Bulunamadı!", color = Color.Red)
    }
}




