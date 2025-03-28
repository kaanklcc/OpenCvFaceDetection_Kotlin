package com.darkwhite.opencvfacedetection.retrofit

class ApiUtils {
    companion object{
        val BASE_URL= "http://57.151.83.31:8080/"

        fun getNFZDao():NFCDao{
            return RetrofitClient.getClient(BASE_URL).create(NFCDao::class.java)
        }
    }
}