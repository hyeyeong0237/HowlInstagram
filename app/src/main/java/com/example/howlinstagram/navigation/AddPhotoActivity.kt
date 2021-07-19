package com.example.howlinstagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_add_photo.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.item_detail.view.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var storage : FirebaseStorage?= null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        CropImage.activity()
            .setAspectRatio(1,1)
            .start(this);

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                val result = CropImage.getActivityResult(data)
                photoUri = result.uri
                addphoto_image.setImageURI(photoUri)
            }else{
                finish()
            }
        }
    }

    fun contentUpload(){
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var user_name = ""


        firestore?.collection("Users")?.document(auth?.currentUser?.uid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user_name = task.result?.get("username").toString()
                }
            }

        //call back
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()
                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.userName = user_name
                contentDTO.explain = addphoto_edit_explain.text.toString()
                contentDTO.timestamp = System.currentTimeMillis()
                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}