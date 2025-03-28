@file:OptIn(ExperimentalMaterial3Api::class)

package com.darkwhite.opencvfacedetection.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.darkwhite.opencvfacedetection.R
import com.darkwhite.opencvfacedetection.ui.theme.anaRenkMavi


@Composable
fun CardRecognation(
    navController: NavController
) {

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
                        stringResource(R.string.kimlikDogrulamaYazi)
                        /*text = "Kimlik Kartı Doğrulama"*/,
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
    ){paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.tckimlik),"tc kimlik örnek",
                Modifier.size(270.dp)
            )

            Text(
                stringResource(R.string.kimlikHazırlamaUyari)
                /*"Lütfen Kimliğinizin arka yüzünü hazırlayınız ve kamerayı aç butonuna tıklayınız. Kamera kimliğinizi " +
                    " okuyana kadar sabit tutunuz."*/,
                fontSize = 16.sp,
                color = Color.DarkGray,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 1.dp,).padding(bottom = 50.dp) )

            Button(
                onClick ={
                    navController.navigate("cameraPreview")
                }

            ) {
                Text(
                    stringResource(R.string.kamerayiAc)
                    /*"Kamerayı aç"*/)
            }





        }



    }

}

@Preview
@Composable
private fun CardRecognationprev() {
    val navController = rememberNavController()
    CardRecognation(navController)

}