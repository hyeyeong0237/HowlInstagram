package com.example.howlinstagram

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.howlinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    var auth  : FirebaseAuth? = null
    var pd : ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()


        txt_login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java ))
            finish()
        }

        register.setOnClickListener {

            var str_username = username.text.toString()
            var str_fullname = fullname.text.toString()
            var str_email = email.text.toString()
            var str_password = password.text.toString()

            if (TextUtils.isEmpty(str_username) || TextUtils.isEmpty(str_fullname) || TextUtils.isEmpty(str_email) || TextUtils.isEmpty(str_password)){
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            } else if(str_password.length < 6){
                Toast.makeText(this, "Password must have 6 characters!", Toast.LENGTH_SHORT).show();
            } else {
                pd = ProgressDialog(this)
                pd!!.setMessage("Please wait...")
                pd!!.show()
                register(str_username, str_fullname, str_email, str_password);
            }





        }


    }

    fun register(username: String, fullname: String, email: String, password: String) {
        auth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                var userID = auth?.currentUser?.uid
                var map = HashMap<String, Any>()
                map["id"] = userID.toString()
                map["username"] = username
                map["fullname"] = fullname
                map["email"] = email
                map["imageurl"] = "https://firebasestorage.googleapis.com/v0/b/instagramtest-fcbef.appspot.com/o/placeholder.png?alt=media&token=b09b809d-a5f8-499b-9563-5252262e9a49"
                FirebaseFirestore.getInstance().collection("Users").document(userID!!).set(map).addOnCompleteListener {
                    task ->
                    if(task.isSuccessful){
                        pd?.dismiss()
                        startActivity(Intent(this, MainActivity::class.java ))
                        finish()

                    }
                }
            }else{
                pd?.dismiss()
                Toast.makeText(this,"You can't register with this email or password", Toast.LENGTH_SHORT).show()
            }

        }



    }
}