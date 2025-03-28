package com.darkwhite.opencvfacedetection.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.darkwhite.opencvfacedetection.model.NFCViewModel
import com.darkwhite.opencvfacedetection.model.User
import com.darkwhite.opencvfacedetection.viewmodel.AuthenticationViewModel
import com.darkwhite.opencvfacedetection.viewmodel.FaceScreenViewModel
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel


import com.google.accompanist.systemuicontroller.rememberSystemUiController
import net.sf.scuba.data.Gender

import org.jmrtd.lds.icao.MRZInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    mrz: String,
    nfcViewModel: NFCViewModel = viewModel(),
    faceScreenViewModel: FaceScreenViewModel,
    matchViewModel: MatchViewModel,
    authenticationViewModel: AuthenticationViewModel

) {
    val context = LocalContext.current
    val nfcData = nfcViewModel.nfcData.observeAsState("")
    val personDetails = nfcViewModel.personDetails.observeAsState()
    val responseMessage= matchViewModel.responseMessage.observeAsState()

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(color = Color(0xFFA3E7E8))
    val base64Image= faceScreenViewModel.base64Image.value
    Log.d("Base644444", "Base6444444: $base64Image")

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = "ID CARD/PASSPORT READER",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                modifier = Modifier.background(Color.Transparent),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFA3E7E8),
                    scrolledContainerColor = Color(0xFFA3E7E8),
                    navigationIconContentColor = Color(0xFFA3E7E8),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color(0xFFA3E7E8)
                )
            )
        },
        containerColor = Color.White,
        modifier = Modifier.background(Color(0xFFA3E7E8))
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (mrz.isNotEmpty()) {
                val extractedInfo = remember(mrz) { mrz.extractMRZInfoAnnotated() }

                val documentNumber = mrz.extractDocumentNumber()
                val birthDate = mrz.extractBirthDate()
                val expirationDate = mrz.extractExpirationDate()
                nfcViewModel.setMRZData(documentNumber, birthDate, expirationDate)

                if (nfcData.value.isNotEmpty()) {
                    Text(
                        text = "NFC Data: ${nfcData.value}",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp),
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                }

                Button(
                    onClick = {

                        nfcViewModel.handleNfcIntent(context) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("NFC ile Doğrula")
                }

                Text(
                    text = extractedInfo,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(5.dp),
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 35.sp
                )
            } else {
                Text(text = "")
            }


            // Person details if available
            personDetails.value?.let { details ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    details.faceImage?.let { bitmap ->
                        val base64Imagee = bitmapToBase64(bitmap)

                        val detectedNfcFace = detectFace(bitmap, context)
                        val base64Imagenfc = detectedNfcFace?.let { bitmapToBase64(it) }
                        authenticationViewModel.base64nfcimage.value = base64Imagenfc ?: ""

                        // Base64 kodunu loga yazdırma
                        Log.d("Base64Image", "Base64 Encoded Image: $base64Imagee")
                        val user = User(
                            name = "kaan",
                            surname = "kılıç",
                            personalId = "10889378992",
                            date = "2002-04-21",
                            image = base64Imagee,
                            photo = base64Image
                        )
                        matchViewModel.checkNfc(user)
                        Log.d("cevap", "cevap: $responseMessage")

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "ID Photo",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Text("Name: ${details.name}", fontSize = 16.sp)
                    Text("Surname: ${details.surname}", fontSize = 16.sp)
                    Text("Birth Date: ${details.birthDate}", fontSize = 16.sp)
                    Text("Expiry Date: ${details.expiryDate}", fontSize = 16.sp)
                    Text("Serial Number: ${details.serialNumber}", fontSize = 16.sp)
                    Text("Nationality: ${details.nationality}", fontSize = 16.sp)
                    Text("Issuer: ${details.issuerAuthority}", fontSize = 16.sp)







                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    navController.navigate("cameraPreview") {
                        popUpTo("MainScreen") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(Color.LightGray),
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.5f)
                    .height(60.dp)
                    .width(50.dp),
            ) {
                Text(text = "Scan ID Card", color = Color.Black, fontSize = 20.sp)
            }

            Button(
                onClick = {
                    navController.navigate("cameraPreview")
                },
                colors = ButtonDefaults.buttonColors(Color.LightGray),
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(0.5f)
                    .height(60.dp)
                    .width(50.dp),
            ) {
                Text("Scan Passport", color = Color.Black, fontSize = 20.sp)
            }

            Button(
                onClick = { nfcViewModel.handleNfcIntent(context) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Başlat")
            }

            Button(
                onClick = { navController.navigate("nfcReader") },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("NFC ile Doğrula")
            }
        }
    }
}


/*fun AnnotatedString.Builder.appendLabeledInfo(label: String, value: String) {
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
}*/


private fun buildTempMrz(
    documentNumber: String,
    dateOfBirth: String,
    expiryDate: String
): MRZInfo? {
    var mrzInfo: MRZInfo? = null
    try {
        mrzInfo = MRZInfo(
            "P",
            "NNN",
            "",
            "",
            documentNumber,
            "NNN",
            dateOfBirth,
            Gender.UNSPECIFIED,
            expiryDate,
            ""
        )
    } catch (e: Exception) {
        Log.d(
            "mrzinfo",
            "MRZInfo error : " + e.localizedMessage
        )
    }

    return mrzInfo
}

/*fun handleNfc(context: Context){

    val user = User(
        name = "kaan",
        surname = "kılıç",
        personalId = "10889378992",
        date = "2002-04-21",
        image =
    )
}*/

fun bitmapTooBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

