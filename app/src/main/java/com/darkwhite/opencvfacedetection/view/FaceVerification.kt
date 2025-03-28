package com.darkwhite.opencvfacedetection.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkwhite.opencvfacedetection.R
import com.darkwhite.opencvfacedetection.ui.theme.anaRenkMavi
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel
import com.darkwhite.opencvfacedetection.viewmodel.TFLiteViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerification(navController: NavController,matchViewModel: MatchViewModel,tfLiteViewModel: TFLiteViewModel) {
    val checkboxDurum= remember { mutableStateOf(false) } //checkbox ilk önce false yani işaretlenmemiş olarak durur.
    val responseMessage = matchViewModel.responseMessage.observeAsState()
    val acilisKontrol = remember { mutableStateOf(false) }
    //val denemeHakki by matchViewModel.denemeHakki.observeAsState(0) // ViewModel'den takip et
    val denemeHakki by tfLiteViewModel.denemeHakki.observeAsState(0) // ViewModel'den takip et
    val resultMessage = tfLiteViewModel.resultMessage.observeAsState()

    // val denemeHakki = remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    var showDialog3 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    val scope= rememberCoroutineScope()


    val snackbarHostState= remember { mutableStateOf(false) }

    var disableButton by remember { mutableStateOf(false) }

    LaunchedEffect(showDialog2) {
        if (showDialog2) {
            disableButton = true
        }
    }

    //arka plan rengi için animasyon ve renk seçenekleri
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E5B8A), // Üstteki renk
            Color(0xFFB0C4DE),
            Color.White  // Alttaki renk
        ),
        startY = 200f,
        endY = 1100f // Bu değeri ekran yüksekliğine göre ayarlayabilirsiniz.
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( //ana başlık yapılandırması
                title = {
                    Text(
                        stringResource(R.string.YüzDoğrulama)
                        /*text = "Yüz Doğrulama"*/,
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
    ){paddingValues -> //sayfa giriş
        Box( modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)){
            Column ( //sayfanın öğrelerinin alt alta sıralanması için column yapısı
                modifier = Modifier
                    .fillMaxSize(),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly

            ){
                /*LaunchedEffect(responseMessage.value) {
                    if (responseMessage.value == "EŞLEŞTİRME BAŞARISIZ" || responseMessage.value == "MANİPÜLASYON ALGILANDI") {
                        matchViewModel.increaseDenemeHakki() // ViewModel üzerinden artır

                        if (denemeHakki < 2 ) {
                            showDialog = true
                            matchViewModel.clearResponseMessage()
                        } else if(denemeHakki == 2){
                            showDialog3 = true
                            matchViewModel.clearResponseMessage()

                        } else {
                            showDialog2 = true
                            matchViewModel.clearResponseMessage()
                        }
                    }
                    if (responseMessage.value == "EŞLEŞTİRME BAŞARILI") {
                        showDialog2=true
                        matchViewModel.clearResponseMessage()

                    }
                }*/

                LaunchedEffect(resultMessage.value) {
                    if (resultMessage.value == "Yüzler eşleşmiyor") {
                        tfLiteViewModel.increaseDenemeHakki() // ViewModel üzerinden artır

                        if (denemeHakki < 2 ) {
                            showDialog = true

                        } else if(denemeHakki == 2){
                            showDialog3 = true
                            tfLiteViewModel.clearResponseMessage()

                        } else {
                            showDialog2 = true
                            tfLiteViewModel.clearResponseMessage()
                        }
                    }
                    if (resultMessage.value == "Yüzler eşleşiyor") {
                        showDialog2=true
                        tfLiteViewModel.clearResponseMessage()

                    }
                }


                if (showDialog) {
                    SimpleAlertDialog(
                        showDialog = showDialog,
                        onDismiss = { showDialog = false },
                        onConfirm = { showDialog = false }
                    )
                }

                if (showDialog3) {
                    SimpleAlertDialog3(
                        showDialog = showDialog3,
                        //onDismiss = { showDialog3 = false },
                        onConfirm = { showDialog3 = false }
                    )
                }


                if (showDialog2) {
                    SimpleAlertDialog2(
                        showDialog2 = showDialog2,
                        //onDismiss = { showDialog3 = false },
                        onConfirm = { showDialog2 = false }
                    )
                }


                PageIndicatorFace(2,1, listOf("NFC", stringResource(R.string.YüzTanıma)/* "Yüz Tanıma"*/)) //sayfa üstünde olan hangi sayfada ve tamamlanma durmunu gösteren fonksyionu çağırma


                Image(painter = painterResource(R.drawable.facet),
                    contentDescription = "NFC",
                    modifier = Modifier
                        .size(270.dp)
                        .padding(bottom = 24.dp))


                Text(
                    stringResource(R.string.YüzUyari)
                    /*text = "Lütfen gün ışığının yoğun olmadığı bir konumda işleminizi yapınız." +
                            "Güneş gözlüğü, maske ve bere gibi aksesuar kullanıyorsanız yüz tanıma işlemine başlamadan çıkartınız."*/,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp,) // Sağdan ve soldan 16dp boşluk
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {


                    Checkbox( //checkbox kutusu ve renk düzenlemeleri.
                        checked = checkboxDurum.value,
                        onCheckedChange = {checkboxDurum.value= it},
                        colors = CheckboxDefaults.colors(
                            checkedColor = anaRenkMavi,
                            uncheckedColor = anaRenkMavi,
                            checkmarkColor = Color.White,
                            disabledColor = anaRenkMavi,
                            disabledIndeterminateColor = anaRenkMavi
                        )


                    )

                    Text(
                        stringResource(R.string.checkbox)/*"VISIGHT TEKNOLOJİ tarafından kişisel verilerimin işlenmesini kabul ediyorum"*/,fontSize = 14.sp,
                        color = Color.DarkGray)
                }


                androidx.compose.material3.Button( //buton yapısı eğer checkbox butonuna tıklanmazsa butona tıklanmaz eğer tıklanırsa buton tıklanabilir olur
                    onClick = { navController.navigate("faceScreen") },
                    colors = ButtonDefaults.buttonColors(anaRenkMavi),
                    shape = RectangleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.End)
                        .padding(start = 12.dp, end = 12.dp)
                    ,

                    enabled = checkboxDurum.value && denemeHakki <2 && !disableButton
                ) {
                    Text(stringResource(R.string.DEVAM)/*"DEVAM"*/)
                }


            }

        }



    }

}


@Composable
fun PageIndicatorFace(pageCount: Int, currentPage: Int, labels: List<String>) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(pageCount) { index ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 2.dp)

            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (index == currentPage) Color.White else Color.Gray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(4.dp)) // Yuvarlak ile yazı arasında boşluk
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, // Yazı stili
                    color = Color.Black,
                    textAlign = TextAlign.Center, // Yazıyı ortala
                    modifier = Modifier.width(100.dp) // Genişliği sabitleyerek hizalamayı koru
                )
            }
        }
    }
}


@Composable
fun SimpleAlertDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {  },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.errorview),
                        contentDescription = "error",
                        modifier = Modifier.size(120.dp)
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.HATA)/*"HATA"*/, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.başarisiz)/*text = "Kimlik Doğrulama Başarısız. Tekrar Deneyin."*/, Modifier.padding(horizontal = 4.dp),textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm() }) {
                    Text(stringResource(R.string.TekrarDeneyiniz)/*"Tekrar Deneyiniz"*/,color = anaRenkMavi, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.İPTAL)/*"İPTAL"*/, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun SimpleAlertDialog3(
    showDialog: Boolean,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {  },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.errorview),
                        contentDescription = "error",
                        modifier = Modifier.size(120.dp)
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text =stringResource(R.string.HATA)/*"HATA"*/, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.maksimumDeneme)/*text = "Maksimum Deneme Sayısına Ulaştınız."*/, Modifier.padding(horizontal = 4.dp),textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm() }) {
                    Text(stringResource(R.string.İPTAL)/*"İPTAL"*/, fontWeight = FontWeight.Bold, color = Color.Red, textAlign = TextAlign.Center)
                }
            },

            )
    }
}

@Composable
fun SimpleAlertDialog2(
    showDialog2: Boolean,
    onConfirm: () -> Unit
) {
    if (showDialog2) {
        AlertDialog(
            onDismissRequest = {  },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ✅ Başlık üstüne ikon veya görsel ekleme
                    Image(painter = painterResource(R.drawable.confirm),
                        contentDescription = "erroe",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp))

                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ONAY)/*text = "ONAY"*/, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.GenelDogrulamaYazi)/*text = "Kimlik Doğrulama Başarıyla Onaylandı"*/, Modifier.padding(horizontal = 4.dp),textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm() }) {
                    Text(stringResource(R.string.Tamam)/*"Tamam"*/)
                }
            },


            )
    }
}
