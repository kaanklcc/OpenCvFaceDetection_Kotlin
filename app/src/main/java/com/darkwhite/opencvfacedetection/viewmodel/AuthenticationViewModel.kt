package com.darkwhite.opencvfacedetection.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class AuthenticationViewModel:ViewModel() {

    var name = mutableStateOf("")
    var surname = mutableStateOf("")
    var birthDate = mutableStateOf("")
    var personalId = mutableStateOf("")



    var base64nfcimage= mutableStateOf("")
}