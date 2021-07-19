package com.example.howlinstagram.navigation.model

data class ContentDTO(var explain : String? = null,
                    var imageUrl : String? = null,
                    var uid: String? = null,
                    var userId : String? = null,
                      var userName : String? = null,
                    var timestamp : Long? = null,
                    var favoriteCount : Int = 0,
                    var favorites : MutableMap<String, Boolean> = HashMap()){
    data class Comment(var uid: String? = null,
                       var userId: String? = null,
                       var userName : String? = null,
                       var comment : String? = null,
                       var timestamp : Long? = null)
}