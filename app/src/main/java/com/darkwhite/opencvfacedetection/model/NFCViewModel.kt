package com.darkwhite.opencvfacedetection.model

import android.content.Context
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.darkwhite.opencvfacedetection.util.Image
import com.darkwhite.opencvfacedetection.util.ImageUtil
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKey
import org.jmrtd.PassportService
import org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE
import org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH
import org.jmrtd.lds.CardSecurityFile
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG15File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo
import java.text.SimpleDateFormat
import java.util.Date




class NFCViewModel : ViewModel() {
    private val _nfcData = MutableLiveData<String>()
    val nfcData: LiveData<String> = _nfcData



    private val _personDetails = MutableLiveData<PersonDetails>()
    val personDetails: LiveData<PersonDetails> = _personDetails

    private var passportNumber: String = ""
    private var birthDate: String = ""
    private var expirationDate: String = ""
    private var documentNumber: String = ""

    private var tag: Tag? = null

    fun setTag(tag: Tag) {
        this.tag = tag
    }

    fun setMRZData(documentNumber: String, birthDate: String, expirationDate: String) {
        this.documentNumber = documentNumber
        this.birthDate = birthDate
        this.expirationDate = expirationDate
    }

    fun handleNfcIntent(context: Context) {
        tag?.let {
            val bacKey = BACKey(documentNumber, birthDate, expirationDate)

            Log.d("wkkk", "handleNfcIntent: BAC anahtar bilgileri mevcut. Okuma işlemi başlatılıyor...")

            // NFC işlemi başlatılıyor
            ReadTask(IsoDep.get(it), bacKey, context, object : ReadTask.Callback {
                override fun onPersonDetailsRetrieved(personDetails: PersonDetails) {
                    _personDetails.postValue(personDetails)
                }

                override fun onError(exception: Exception) {
                    Log.e("wkkk", "Error: ${exception.message}")
                }
            }).execute()
        } ?: run {
            Log.e("wkkk", "handleNfcIntent: Tag null, işlem yapılamaz.")
        }
    }



}

private class ReadTask(
    private val isoDep: IsoDep,
    private val bacKey: BACKey,
    private val context: Context,
    private val callback: Callback
) : AsyncTask<Void, Void, Exception>() {



    interface Callback {
        fun onPersonDetailsRetrieved(personDetails: PersonDetails)
        fun onError(exception: Exception)
    }

    private val eDocument = EDocument()
    private var docType = DocType.OTHER
    private var personDetails = PersonDetails()
    private var additionalPersonDetails = AdditionalPersonDetails()


    override fun doInBackground(vararg params: Void?): Exception? {
        return try {
            val cardService = CardService.getInstance(isoDep)
            cardService.open()

            val service = PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, true, false)
            service.open()

            var paceSucceeded = false
            try {
                val cardSecurityFile = CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY))
                val securityInfoCollection = cardSecurityFile.securityInfos
                for (securityInfo in securityInfoCollection) {
                    if (securityInfo is PACEInfo) {
                        service.doPACE(bacKey, securityInfo.objectIdentifier, PACEInfo.toParameterSpec(securityInfo.parameterId), null)
                        paceSucceeded = true
                    }
                }
            } catch (e: Exception) {
                // Handle exception
            }

            service.sendSelectApplet(paceSucceeded)

            if (!paceSucceeded) {
                try {
                    service.getInputStream(PassportService.EF_COM).read()
                } catch (e: Exception) {
                    service.doBAC(bacKey)
                }
            }


            // -- Face Image -- //
            val dg2In = service.getInputStream(PassportService.EF_DG2)
            val dg2File = DG2File(dg2In)

            val faceInfos = dg2File.faceInfos
            val allFaceImageInfos: MutableList<FaceImageInfo> = ArrayList()
            for (faceInfo in faceInfos) {
                allFaceImageInfos.addAll(faceInfo.faceImageInfos)
            }

            if (allFaceImageInfos.isNotEmpty()) {
                val faceImageInfo = allFaceImageInfos.iterator().next()
                val image: Image = ImageUtil.getImage(context, faceImageInfo)

                // Bitmap image'i doğrudan atama
                image.bitmapImage?.let {
                    personDetails.faceImage = it
                }

                // Base64 image'i atama
                personDetails.faceImageBase64 = image.base64Image ?: ""  // Boş string ile fallback yapabilirsiniz
            }





            /*   val dg2In = service.getInputStream(PassportService.EF_DG2)
               val dg2File = DG2File(dg2In)

               val faceInfos: List<FaceInfo> = dg2File.faceInfos
               val allFaceImageInfos = mutableListOf<FaceImageInfo>()

               for (faceInfo in faceInfos) {
                   allFaceImageInfos.addAll(faceInfo.faceImageInfos)
               }*/


            // -- Personal Details -- //
            val dg1In = service.getInputStream(PassportService.EF_DG1)
            val dg1File = DG1File(dg1In)

            val mrzInfo = dg1File.mrzInfo
            personDetails.apply {
                name = mrzInfo.secondaryIdentifier.replace("<", " ").trim()
                surname = mrzInfo.primaryIdentifier.replace("<", " ").trim()
                personalNumber = mrzInfo.personalNumber
                gender = mrzInfo.gender?.toString()
                birthDate = convertFromMrzDate(mrzInfo.dateOfBirth)
                expiryDate = convertFromMrzDate(mrzInfo.dateOfExpiry)
                serialNumber = mrzInfo.documentNumber
                nationality = mrzInfo.nationality
                issuerAuthority = mrzInfo.issuingState
            }



            docType = if (mrzInfo.documentCode == "I") DocType.ID_CARD else DocType.PASSPORT

            // -- Additional Details (if exist) -- //
            try {
                val dg11In = service.getInputStream(PassportService.EF_DG11)
                val dg11File = DG11File(dg11In)

                if (dg11File.length > 0) {
                    additionalPersonDetails.apply {
                        custodyInformation = dg11File.custodyInformation
                        nameOfHolder = dg11File.nameOfHolder
                        fullDateOfBirth = dg11File.fullDateOfBirth
                        otherNames = dg11File.otherNames
                        otherValidTDNumbers = dg11File.otherValidTDNumbers
                        permanentAddress = dg11File.permanentAddress
                        personalNumber = dg11File.personalNumber
                        personalSummary = dg11File.personalSummary
                        placeOfBirth = dg11File.placeOfBirth
                        profession = dg11File.profession
                        proofOfCitizenship = dg11File.proofOfCitizenship
                        tag = dg11File.tag
                        tagPresenceList = dg11File.tagPresenceList
                        telephone = dg11File.telephone
                        title = dg11File.title
                    }
                }
            } catch (e: Exception) {
                // Handle exception
            }

            // -- Document Public Key -- //
            try {
                val dg15In = service.getInputStream(PassportService.EF_DG15)
                val dg15File = DG15File(dg15In)
                val publicKey = dg15File.publicKey
                eDocument.docPublicKey = publicKey
            } catch (e: Exception) {
                // Handle exception
            }

            eDocument.apply {
                docType = docType
                personDetails = personDetails
                additionalPersonDetails = additionalPersonDetails
            }

            // Callback çağrısı (arrière planda güvenli bir şekilde)
            //callback.onPersonDetailsRetrieved(personDetails)
            // Tüm veriler alındıktan sonra callback çağrılır
            callback.onPersonDetailsRetrieved(personDetails)


            null // Başarıyla tamamlandığında null döndür
        } catch (e: Exception) {
            // Hata durumunda callback'i çağır
            callback.onError(e)
            e
        }
    }

    private fun convertFromMrzDate(mrzDate: String): String {
        val date = stringToDate(mrzDate, SimpleDateFormat("yyMMdd"))
        return dateToString(date, SimpleDateFormat("dd.MM.yyyy"))
    }

    private fun stringToDate(dateString: String, format: SimpleDateFormat): Date {
        return format.parse(dateString) ?: throw IllegalArgumentException("Invalid date format")
    }

    private fun dateToString(date: Date, format: SimpleDateFormat): String {
        return format.format(date)
    }

}
