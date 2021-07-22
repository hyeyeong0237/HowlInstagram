package com.example.howlinstagram.navigation.model

import com.google.firebase.firestore.Blob

data class StoryDTO(
    var imageurl: String? = null,
    var timestart: Long? = null,
    var timeend: Long? = null,
    var userid: String? = null,
    var views: MutableMap<String?, Boolean?> = HashMap()
) {

}