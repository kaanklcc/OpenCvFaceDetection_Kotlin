package com.darkwhite.opencvfacedetection

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.darkwhite.opencvfacedetection.model.NFCViewModel
import com.darkwhite.opencvfacedetection.ui.MainContent
import com.darkwhite.opencvfacedetection.ui.theme.OpenCVFaceDetectionTheme
import com.darkwhite.opencvfacedetection.util.TFLiteHelper
import com.darkwhite.opencvfacedetection.view.SayfaGecisleri
import com.darkwhite.opencvfacedetection.viewmodel.AuthenticationViewModel
import com.darkwhite.opencvfacedetection.viewmodel.FaceScreenViewModel
import com.darkwhite.opencvfacedetection.viewmodel.MatchViewModel
import com.darkwhite.opencvfacedetection.viewmodel.TFLiteViewModel
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    private var navController: NavController? = null // NavController referansı ekleyin
    private lateinit var nfcAdapter: NfcAdapter
    private var scannedMRZ: String = ""
    private val nfcViewModel by viewModels<NFCViewModel>()
    private val faceScreenViewModel by viewModels<FaceScreenViewModel>()
    private val matchViewModel by viewModels<MatchViewModel>()
    private val authenticationViewModel by viewModels<AuthenticationViewModel>()
    private val tfLiteViewModel by viewModels<TFLiteViewModel>()

    //kaankilicccc



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        
        /*ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            0
        )*/

        // Modeli yükleyip log mesaji ver
        val mobileFaceNetModel = TFLiteHelper.loadModelFile(this, "mobileFaceNet.tflite")
        val retinaFaceModel = TFLiteHelper.loadModelFile(this, "RetinaFaceMobileNet-1080x1920.tflite")
        val livenessModel = TFLiteHelper.loadModelFile(this, "efficientnet-lite3-int8.tflite")


        if (mobileFaceNetModel != null) {
            Log.d("ModelTest", "✅ MobileFaceNet başarıyla yüklendi!")
        } else {
            Log.e("ModelTest", "❌ MobileFaceNet yüklenemedi!")
        }

        if (livenessModel != null) {
            Log.d("ModelTest", "✅ livenessModel başarıyla yüklendi!")
        } else {
            Log.e("ModelTest", "❌ livenessModel yüklenemedi!")
        }

        if (retinaFaceModel != null) {
            Log.d("ModelTest", "✅ RetinaFace başarıyla yüklendi!")
        } else {
            Log.e("ModelTest", "❌ RetinaFace yüklenemedi!")
        }

        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV yüklendi")
        } else {
            Log.e("OpenCV", "OpenCV yüklenemedi")
        }
        
        if (OpenCVLoader.initLocal()) {
            Log.d("opencvinital", "onCreate: OpenCVLoader LOADED SUCCESSFULLY")
        } else {
            Log.e("opencvinital", "onCreate: OpenCVLoader LOADING FAILED")
        }
        
        val faceDetector = MyUtils.initFaceDetector(this)
        
        setContent {
            OpenCVFaceDetectionTheme {
                /*Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (faceDetector == null) {
                        Text(text = "Face Detector is null")
                    } else {
                        MainContent(faceDetector = faceDetector)
                    }
                }*/

                /* val navController = rememberNavController()
                SayfaGecisleri(navController)*/
                val navController = rememberNavController()
                SayfaGecisleri(
                    navController = navController,
                    nfcViewModel,
                    faceScreenViewModel,
                    matchViewModel,
                    authenticationViewModel,
                    tfLiteViewModel,

                )


            }
        }
    }

    override fun onResume() {
        super.onResume()

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter != null) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE // FLAG_UPDATE_CURRENT yerine FLAG_MUTABLE kullanıldı.
            )

            // NFC için gerekli intent filtreleri
            val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            val intentFiltersArray = arrayOf(techFilter, tagFilter, ndefFilter)


            val techList = arrayOf(
                arrayOf(
                    IsoDep::class.java.name
                ),
                arrayOf(NfcA::class.java.name),
                arrayOf(NfcB::class.java.name)
            )

            adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techList)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("wkkk", "onNewIntent: Yeni bir NFC intent alındı...")

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            Log.d("wkkk", "onNewIntent: ACTION_TECH_DISCOVERED tetiklendi.")

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                Log.d("wkkk", "onNewIntent: Tag bulundu. Kullanılabilir teknolojiler: ${it.techList.joinToString(", ")}")

                if (it.techList.contains("android.nfc.tech.IsoDep")) {
                    Log.d("wkkk", "onNewIntent: IsoDep destekleniyor, işleme devam ediliyor...")

                    // Tag'i ViewModel'e geçiriyoruz
                    nfcViewModel.setTag(it)

                    // NFC işlemini başlatabiliriz
                    nfcViewModel.handleNfcIntent(this)

                    Log.d("wkkk", "onNewIntent: Arayüz yükleme moduna geçirildi.")
                } else {
                    Log.e("wkkk", "onNewIntent: Tag IsoDep desteklemiyor.")
                }
            } ?: run {
                Log.e("wkkk", "onNewIntent: Tag alınamadı. NFC etiketi null döndü.")
            }
        } else {
            Log.d("wkkk", "onNewIntent: NFC için tanımlı olmayan bir intent alındı: ${intent.action}")
        }
    }


    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        adapter?.disableForegroundDispatch(this)
    }


    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.NFC // NFC izni burada mevcut
        )
    }
    
    /*companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ).toTypedArray()
    }*/
}
