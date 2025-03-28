package com.darkwhite.opencvfacedetection.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkwhite.opencvfacedetection.model.User
import com.darkwhite.opencvfacedetection.retrofit.ApiUtils

import kotlinx.coroutines.launch

class MatchViewModel:ViewModel() {

    private val _responseMessage = MutableLiveData<String?>()
    val responseMessage: LiveData<String?> = _responseMessage

    private val _denemeHakki = MutableLiveData(0)
    val denemeHakki: LiveData<Int> = _denemeHakki

    private val nfcDao = ApiUtils.getNFZDao()

    fun checkNfc(user: User) {
        viewModelScope.launch {
            try {
                Log.d("MatchViewModel", "Kullanıcı verileri gönderiliyor: $user") // API çağrısından önce log

                val response = nfcDao.matchPhotos(user)

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("MatchViewModel", "sunucu yanıtı: $result")

                    Log.d("MatchViewModel", "Sunucu yanıtı: $result") // API cevabını logla

                    when (result) {
                        "1" -> {
                            _responseMessage.value = "EŞLEŞTİRME BAŞARILI"
                        }
                        "2" -> {
                            _responseMessage.value = "EŞLEŞTİRME BAŞARISIZ"
                        }
                        "3" -> {
                            _responseMessage.value = "MANİPÜLASYON ALGILANDI"
                        }
                        else -> {
                            _responseMessage.value = "Bilinmeyen hata: $result"
                        }
                    }

                    Log.d("MatchViewModel", "Güncellenmiş Yanıt: ${_responseMessage.value}") // Yanıtın değiştiğini kontrol et
                } else {
                    Log.e("MatchViewModel", "Hata kodu: ${response.errorBody()?.string()}") // Sunucu hata kodunu logla
                    Log.e("MatchViewModel", "Hata kodu: ${response.code()}") // Sunucu hata kodunu logla
                    _responseMessage.value = "Sunucu hatası: ${response.code()}"

                    Log.d("MatchViewModel", "Güncellenmiş Yanıt (Hata Durumu): ${_responseMessage.value}")
                }
            } catch (e: Exception) {
                Log.e("MatchViewModel", "API isteğinde hata oluştu: ${e.message}", e)
            }
        }
    }

    fun increaseDenemeHakki() {
        _denemeHakki.value = (_denemeHakki.value ?: 0) + 1
    }

    fun resetDenemeHakki() {
        _denemeHakki.value = 0
    }

    fun clearResponseMessage() {
        _responseMessage.value = null
    }



}