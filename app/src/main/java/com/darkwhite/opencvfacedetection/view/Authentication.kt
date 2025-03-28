@file:OptIn(ExperimentalMaterialApi::class)

package com.darkwhite.opencvfacedetection.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darkwhite.opencvfacedetection.R
import com.darkwhite.opencvfacedetection.model.NFCViewModel
import com.darkwhite.opencvfacedetection.ui.theme.anaRenkMavi
import com.darkwhite.opencvfacedetection.viewmodel.AuthenticationViewModel
import com.darkwhite.opencvfacedetection.viewmodel.FaceScreenViewModel
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel
import com.darkwhite.opencvfacedetection.viewmodel.TFLiteViewModel
import kotlinx.coroutines.launch


//applicationgit
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Authentication(navController: NavController,
                   mrz: String,
                   nfcViewModel: NFCViewModel = viewModel(),
                   faceScreenViewModel: FaceScreenViewModel,
                   matchViewModel: MatchViewModel,
                   authenticationViewModel: AuthenticationViewModel,
                   tfLiteViewModel: TFLiteViewModel
) {
    //arka plan rengi ve şekli ayarlama
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E5B8A), // Üstteki renk
            Color(0xFFB0C4DE),
            Color.White  // Alttaki renk
        ),
        startY = 200f,
        endY = 1100f // Bu değeri ekran yüksekliğine göre ayarlayabilirsiniz.
    )

    val context = LocalContext.current

    val personDetails = nfcViewModel.personDetails.observeAsState()
    var isScanning by remember { mutableStateOf(false) }
    val responseMessage= matchViewModel.responseMessage.observeAsState()
    val base64Image= faceScreenViewModel.base64Image.value


    Log.d("Base644444", "Base6444444: $base64Image")

    // ModalBottomSheetState oluşturma
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden) //ilk önce kapalı bir sheet daha sonrasında butona basılınca açılır
    val coroutineScope = rememberCoroutineScope()
    //modalbottomsheet kodları
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp,
            bottomEnd = 50.dp, bottomStart = 50.dp),
        sheetContent = {
            // ModalBottomSheet içeriği
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {


                Text(
                    "Kimliğinizi hazırlayın",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Kimlik kartınızı telefonun ön yüzeyinde kamera hizasında okuma başlayana " +
                            "kadar turunuz. Hata almanız durumunda,telfonunuzun kılıfı varsa " +
                            "çıkarıp tekrar deneyiniz.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                if (!isScanning) {
                    Image(
                        painter = painterResource(R.drawable.read),
                        contentDescription = "kimlik okuma",
                        modifier = Modifier.size(150.dp)
                    )
                } else {
                    NFCLoadingAnimation(isScanning)
                }

                if (mrz.isNotEmpty()) {
                    /* val extractedInfo = remember(mrz) { mrz.extractMRZInfoAnnotated() }


                     val documentNumber = mrz.extractDocumentNumber()
                     val birthDate = mrz.extractBirthDate()
                     val expirationDate = mrz.extractExpirationDate()*/
                    val isPassport = mrz.startsWith("P<")

                    val documentNumber = if (isPassport) mrz.extractDocumentNumberPassport() else mrz.extractDocumentNumber()
                    val birthDate = if (isPassport) mrz.extractBirthDatePassport() else mrz.extractBirthDate()
                    val expirationDate = if (isPassport) mrz.extractExpirationDatePassport() else mrz.extractExpirationDate()
                    nfcViewModel.setMRZData(documentNumber, birthDate, expirationDate)
                    val name = if (isPassport) mrz.extractNamePassport() else mrz.extractName()
                    val surname = if (isPassport) mrz.extractSurnamePassport() else mrz.extractSurname()
                    val personalId = if (isPassport) mrz.extractDocumentNumberPassport() else mrz.extractPersonaId()
                    authenticationViewModel.name.value= name
                    authenticationViewModel.surname.value=surname
                    authenticationViewModel.personalId.value=personalId
                    authenticationViewModel.birthDate.value=birthDate




                    Button(
                        onClick = {

                            nfcViewModel.handleNfcIntent(context)
                            isScanning = true  // Animasyonu başlat
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("NFC ile Doğrula")
                    }

                    /* if (isScanning) {
                         NFCLoadingAnimation(isScanning)
                     }*/
                }


                LaunchedEffect(personDetails.value?.faceImage) {
                    personDetails.value?.faceImage?.let { bitmap ->
                        //tfLiteViewModel.bitmapToByteBuffer(bitmap)
                        val nfcFaceImage=personDetails.value?.faceImage


                        val base64Imagenfc = bitmapToBase64(bitmap)

                        Log.d("Base64", "Base64 Image: $base64Imagenfc")
                        authenticationViewModel.base64nfcimage.value = base64Imagenfc ?: ""
                        isScanning=false
                        navController.navigate("faceVerification")

                    }

                }
            }
        }
    ) { //ana ekran ve scaffold oluşturma
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                             text = "Kimlik Kartı Okutma",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier.background(Color.Transparent),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = anaRenkMavi, // Arka plan rengi
                        titleContentColor = Color.White   // Başlık rengi
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.background(gradientBackground)
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(WindowInsets.systemBars.asPaddingValues()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {




                PageIndicator(2,0, listOf("NFC", "Yüz Tanıma")) //pageindicatör aktif etme
                Image(
                    painter = painterResource(R.drawable.nfcsymbol),
                    contentDescription = "NFC symbol",
                    modifier = Modifier
                        .size(85.dp)
                        .offset(y = (-30.dp))
                )

                Image(
                    painter = painterResource(R.drawable.nfcrec),
                    contentDescription = "NFC",
                    modifier = Modifier
                        .size(230.dp)
                        .offset(y = (-45.dp))
                )
                //Spacer(modifier = Modifier.height(4.dp))
                Text(

                    "İşleme başlamadan önce TC Kimlik Kartınızı hazırlayınız.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y=(-45.dp))


                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        // ModalBottomLayoutu butona basınca ekranda gösterme
                        /*coroutineScope.launch {
                            sheetState.show()
                        }*/
                        coroutineScope.launch {
                            sheetState.show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(anaRenkMavi),
                    shape = RoundedCornerShape(8.dp), // Kenarları yuvarlat
                    modifier = Modifier
                        .fillMaxWidth() // Buton tam genişlikte
                        .height(50.dp) // Butonun yüksekliğini artır
                        .padding(horizontal = 16.dp) // Sağ ve sol boşluk

                ) {
                    Text(

                        "Kimliğimi Tara",
                        fontSize = 18.sp, // Yazı boyutunu artır
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }

            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int, labels: List<String>) { //ekranın üstünde gözzüken hangi sayfada olduğunu ve işlemin tamamlanıp tamamlanmadığını gösteren yapı
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(pageCount) { index -> //her sayfada tekrar etmesini sağlar.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (index == currentPage) Color.White else Color.Gray, //aynı ekrandaysa beyaz yoksa gri renk verme.
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp)) // Yuvarlak ile yazı arasında boşluk
                Text(
                    text = labels.getOrElse(index) { "" }, //yazı varsa yaz
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp), fontWeight = FontWeight.Bold, // Yazı stili
                    color = Color.Black,
                    textAlign = TextAlign.Center, // Yazıyı ortala
                    modifier = Modifier.width(52.dp) // Genişliği sabitleyerek hizalamayı koru
                )
            }
        }
    }
}


fun AnnotatedString.Builder.appendLabeledInfo(label: String, value: String) {
    // Başlık ve değeri eklerken texti  düzenleyen fonksiyon
    this.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 25.sp))
    this.append("$label: ")
    this.pushStyle(SpanStyle(fontWeight = FontWeight.W400))
    this.pop()
    this.pushStyle(SpanStyle(fontWeight = FontWeight.W400))
    this.append("$value\n")

}

fun String.extractMRZInfoAnnotated(): AnnotatedString {
    val lines = this.split("\n")
    if (lines.isEmpty()) return AnnotatedString("")

    val builder = AnnotatedString.Builder()

    val firstLine = lines[0]
    val secondLine = lines[1]
    val thirdLine = lines[2]

    val documentType = when {
        firstLine.startsWith("P<") -> "Passport"
        firstLine.startsWith("I<") -> "ID Card"
        else -> "Unknown"
    }

    val citizenship = if (firstLine.length > 4) firstLine.substring(2, 5) else "Unknown"
    val cleanedIDNumber = if (firstLine.length > 18) firstLine.substring(16, 30).filterNot { it == '<' || it == ' ' } else "Unknown"
    val IDNumber = if (cleanedIDNumber.isNotEmpty()) cleanedIDNumber else "Unknown"
    val dateOfBirthDay = if (secondLine.length > 5) secondLine.substring(0, 6) else "Unknown"
    val expiryDate = if (secondLine.length > 5) secondLine.substring(8, 14) else "Unknown"
    val documentNumber = if (firstLine.length > 5) firstLine.substring(5, 14).replace("O", "0") else "Unknown"

    var gender = "Unknown"
    for (char in secondLine) {
        if (char == 'M' || char == 'F') {
            gender = if (char == 'M') "Male" else "Female"
            break
        }
    }

    val Surname = thirdLine.split('<').firstOrNull() ?: "Unknown"
    val Name = thirdLine.split('<').filter { it.isNotBlank() }.getOrNull(1) ?: "Unknown"


    builder.appendLabeledInfo("Document Type", documentType)
    builder.appendLabeledInfo("Citizenship", citizenship)
    builder.appendLabeledInfo("ID Number", IDNumber)
    builder.appendLabeledInfo("Birth Day", dateOfBirthDay)
    builder.appendLabeledInfo("Gender", gender)
    builder.appendLabeledInfo("Surname", Surname)
    builder.appendLabeledInfo("Name", Name)
    builder.appendLabeledInfo("expirationDate", expiryDate)
    builder.appendLabeledInfo("documentNumber", documentNumber)

    Log.d("bilgi","Scanned Text Buffer ID Card ->>>> " +"Doc Number:"+documentNumber+" DateOfBirth:"+dateOfBirthDay + "ExpiryDate:"+expiryDate)
    //val mrzInfo: MRZInfo = buildTempMrz(documentNumber, dateOfBirthDay, expiryDate)

    Log.d("wkkk", "onNewIntent: BAC anahtar bilgileri mevcut. Okuma işlemi başlatılıyor...")
    return builder.toAnnotatedString()
}

fun String.extractDocumentNumber(): String {
    val lines = this.split("\n")
    val firstLine = lines[0]
    return if (firstLine.length > 5) firstLine.substring(5, 14).replace("O", "0") else "Unknown"
}

fun String.extractBirthDate(): String {
    val lines = this.split("\n")
    val secondLine = lines[1]
    return if (secondLine.length > 5) secondLine.substring(0, 6) else "Unknown"
}

fun String.extractExpirationDate(): String {
    val lines = this.split("\n")
    val secondLine = lines[1]
    return if (secondLine.length > 5) secondLine.substring(8, 14) else "Unknown"
}

fun String.extractDocumentNumberPassport(): String {
    val lines = this.split("\n")
    val firstLine = lines[1]
    return if (firstLine.length > 5) firstLine.substring(0, 9).replace("O", "0") else "Unknown"
}
fun String.extractBirthDatePassport(): String {
    val lines = this.split("\n")
    val secondLine = lines[1]
    return if (secondLine.length > 5) secondLine.substring(13, 19) else "Unknown"
}
fun String.extractExpirationDatePassport(): String {
    val lines = this.split("\n")
    val secondLine = lines[1]
    return if (secondLine.length > 5) secondLine.substring(21, 27) else "Unknown"
}

fun String.extractPersonaId(): String {
    val lines = this.split("\n")
    val firstLine = lines[0]
    return if (firstLine.length > 18) firstLine.substring(16, 27) else "Unknown"
}

fun String.extractName(): String {
    val lines = this.split("\n")
    val thirdLine = lines[2]
    val Name = thirdLine.split('<').filter { it.isNotBlank() }.getOrNull(1) ?: "Unknown"
    return Name
}
fun String.extractSurname(): String {
    val lines = this.split("\n")
    val thirdLine = lines[2]
    val Surname = thirdLine.split('<').firstOrNull() ?: "Unknown"
    return Surname
}


fun String.extractNamePassport(): String {
    val lines = this.split("\n")
    if (lines.size < 2) return "Unknown"

    val firstLine = lines[0]  // İlk satır (Soyadı ve Adı içerir)
    val namePart = firstLine.substring(5)  // İlk 5 karakteri at (P<GBP kısmı)

    val parts = namePart.split("<<", limit = 2) // Soyadı ve Adı ayır
    return parts.getOrNull(1)?.replace("<", " ") ?: "Unknown"
}

fun String.extractSurnamePassport(): String {
    val lines = this.split("\n")
    if (lines.size < 2) return "Unknown"

    val firstLine = lines[0]  // İlk satır (Soyadı ve Adı içerir)
    val namePart = firstLine.substring(5)  // İlk 5 karakteri at (P<GBP kısmı)

    val parts = namePart.split("<<", limit = 2) // Soyadı ve Adı ayır
    return parts.getOrNull(0)?.replace("<", "") ?: "Unknown"
}

@Composable
fun NFCLoadingAnimation(isScanning: Boolean) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = isScanning,
        restartOnPlay = true,
        speed = 0.18f
    )

    LottieAnimation(
        composition = composition,
        progress = progress,
        modifier = Modifier.size(100.dp)
    )
}
