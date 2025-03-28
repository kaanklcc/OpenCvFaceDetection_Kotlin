package com.darkwhite.opencvfacedetection.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.darkwhite.opencvfacedetection.model.NFCViewModel
import com.darkwhite.opencvfacedetection.viewmodel.AuthenticationViewModel
import com.darkwhite.opencvfacedetection.viewmodel.FaceScreenViewModel
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel
import com.darkwhite.opencvfacedetection.viewmodel.TFLiteViewModel


import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Composable
fun SayfaGecisleri(
    navController: NavHostController,
    nfcViewModel: NFCViewModel,
    faceScreenViewModel: FaceScreenViewModel,
    matchViewModel: MatchViewModel,
    authenticationViewModel: AuthenticationViewModel,
    tfLiteViewModel: TFLiteViewModel



){
    NavHost(navController = navController, startDestination = "CardRecognation") {
        composable(
            route = "MainScreen?mrz={mrz}",
            arguments = listOf(navArgument("mrz") { defaultValue = "" })
        ) { backStackEntry ->
            val decodedMRZ  = URLDecoder.decode(backStackEntry.arguments?.getString("mrz") ?: "", StandardCharsets.UTF_8.toString())
            MainScreen(
                navController = navController,
                mrz = decodedMRZ,
                nfcViewModel,
                faceScreenViewModel,
                matchViewModel,
                authenticationViewModel

                )
        }
        composable("cameraPreview") {
            CameraPreviewScreen(
                navController = navController
                /*viewModel,
                onMRZDetected = { mrzText ->
                    viewModel.updateMRZData(mrzText)
                }*/
            )
        }
        composable("authentication?mrz={mrz}",
            arguments = listOf(navArgument("mrz") { defaultValue = "" })
        ){backStackEntry ->
            val decodedMRZ  = URLDecoder.decode(backStackEntry.arguments?.getString("mrz") ?: "", StandardCharsets.UTF_8.toString())
            Authentication( navController = navController,
                mrz = decodedMRZ,
                nfcViewModel,
                faceScreenViewModel,
                matchViewModel,authenticationViewModel,tfLiteViewModel
            )
        }
        composable("faceVerification"){
            FaceVerification(navController,matchViewModel,tfLiteViewModel)
        }
        composable("faceScreen"){
            FaceScreen(navController,faceScreenViewModel,
                authenticationViewModel,nfcViewModel,matchViewModel,tfLiteViewModel)
        }
        composable("CardRecognation"){
            CardRecognation(navController)
        }


    }

}
