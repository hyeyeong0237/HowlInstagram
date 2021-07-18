package com.example.howlinstagram.navigation.util

import com.example.howlinstagram.navigation.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class FcmPush {

    var JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAVhVTfJw:APA91bEv6mNE63O9-cxdmFttC0CiO3RZn-ngzibmwfjEBaUscYuBT7Cfg_l7fF5g8-jpWPs7NaRXoMN6KJBcQKPlJCSTx31iVzBpWGTfGVWndwPO6Yyv--0No1_N8XZ2ZkwMt-m9BFKc"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object{
        var instance = FcmPush()

    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }
    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()
                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO)!!)
                var request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key="+serverKey)
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        println(response.body?.string())
                    }

                })

            }

        }
    }
}