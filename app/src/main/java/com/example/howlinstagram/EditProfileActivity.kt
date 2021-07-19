package com.example.howlinstagram

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.navigation.model.UserDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.fullname
import kotlinx.android.synthetic.main.fragment_user.username
import kotlinx.android.synthetic.main.fragment_user.view.*



class EditProfileActivity : AppCompatActivity() {

    var firestore : FirebaseFirestore? = null
    var auth : FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        updateUI()


        close.setOnClickListener {
            finish()
        }

        save.setOnClickListener {
            saveUserInfo(fullname.text.toString(), username.text.toString())
            finish()
        }

        tv_change.setOnClickListener {
            CropImage.activity().setAspectRatio(1,1)
                .setCropShape(CropImageView.CropShape.OVAL)
                .start(this)
        }

        image_profile.setOnClickListener {
            CropImage.activity().setAspectRatio(1,1)
                .setCropShape(CropImageView.CropShape.OVAL)
                .start(this)
        }


    }

    fun updateUI(){
        firestore?.collection("Users")?.document(auth?.currentUser?.uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var userDTO = documentSnapshot.toObject(UserDTO::class.java)
            username?.text = userDTO?.username
            fullname?.text = userDTO?.fullname
            Glide.with(this).load(userDTO?.imageurl).apply(RequestOptions().circleCrop())
                .into(image_profile)

        }


    }

    private fun saveUserInfo(fullname: String, username: String) {

        var map = HashMap<String, Any>()
        map["username"] = username
        map["fullname"] = fullname
        FirebaseFirestore.getInstance().collection("Users").document(auth?.currentUser?.uid!!).update(map).addOnCompleteListener {
            Toast.makeText(this, "Successfully updated!", Toast.LENGTH_SHORT).show()
        }

    }

    fun uploadImage(profileuri: Uri){

        val pd = ProgressDialog(this)
        pd.setMessage("Uploading")
        pd.show()

        var uid = FirebaseAuth.getInstance().currentUser?.uid
        var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
        storageRef.putFile(profileuri!!).continueWithTask { task: com.google.android.gms.tasks.Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl

        }.addOnSuccessListener { uri ->
            var map = HashMap<String, Any>()
            map["imageurl"] =uri.toString()
            FirebaseFirestore.getInstance().collection("Users").document(uid).update(map).addOnCompleteListener {
                pd.dismiss();
            }


        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK){
            val result = CropImage.getActivityResult(data)
            uploadImage(result.uri)

        }else{
            Toast.makeText(this, "Something gone wrong!", Toast.LENGTH_SHORT).show();
        }
    }
}