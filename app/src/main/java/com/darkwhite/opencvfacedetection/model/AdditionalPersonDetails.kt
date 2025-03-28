package com.darkwhite.opencvfacedetection.model

data class AdditionalPersonDetails(
    var custodyInformation: String? = null,
    var fullDateOfBirth: String? = null,
    var nameOfHolder: String? = null,
    var otherNames: List<String> = emptyList(),
    var otherValidTDNumbers: List<String> = emptyList(),
    var permanentAddress: List<String> = emptyList(),
    var personalNumber: String? = null,
    var personalSummary: String? = null,
    var placeOfBirth: List<String> = emptyList(),
    var profession: String? = null,
    var proofOfCitizenship: ByteArray? = null,
    var tag: Int = 0,
    var tagPresenceList: List<Int> = emptyList(),
    var telephone: String? = null,
    var title: String? = null
)

