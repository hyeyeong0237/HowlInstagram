package com.example.howlinstagram

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import java.text.SimpleDateFormat
import java.util.*


class AddStoryActivity : AppCompatActivity() {
    var storage : FirebaseStorage?= null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? =null
    var user_name : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        CropImage.activity()
            .setAspectRatio(9,16)
            .start(this);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                val result = CropImage.getActivityResult(data)
                 var photoUri = result.uri
                 contentUpload(photoUri)

            }else{
                finish()
            }
        }
    }

    fun contentUpload(photoUri : Uri){
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "Story_" + timestamp + "_.png"



        firestore?.collection("Users")?.document(auth?.currentUser?.uid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    this.user_name = task.result?.get("username").toString()
                }
            }

        //call back
        var storageRef = storage?.reference?.child("Story")?.child(imageFileName)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->

                var timeend = System.currentTimeMillis() + 86400000
                var timestart = System.currentTimeMillis()

                val hashMap: HashMap<String, Any> = HashMap()
                hashMap["imageurl"] = uri.toString()
                hashMap["timestart"] = timestart
                hashMap["timeend"] = timeend
                hashMap["userid"] = auth?.currentUser?.uid!!

                val hashMap2: HashMap<String, Any> = HashMap()
                hashMap2["userid"] = auth?.currentUser?.uid!!
                firestore?.collection("Story")?.document(auth?.currentUser?.uid!!)?.set(hashMap2)
                firestore?.collection("Story")?.document(auth?.currentUser?.uid!!)?.collection("userStories")?.document()?.set(hashMap)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

}