package com.example.howlinstagram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.AlarmDTO
import com.example.howlinstagram.navigation.model.ContentDTO
import com.example.howlinstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null
    var user_name : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)


        FirebaseFirestore.getInstance().collection("Users")?.document(FirebaseAuth.getInstance().currentUser?.uid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user_name = task.result?.get("username").toString()
                    Glide.with(this).load(task.result?.get("imageurl").toString()).apply(RequestOptions().circleCrop()).into(send_profile)

                }
            }

        FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user_Name.text = task.result?.get("userName").toString()
                    explain_textview.text = task.result?.get("explain").toString()

                }
            }

        FirebaseFirestore.getInstance().collection("Users").document(destinationUid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    Glide.with(this).load(task.result?.get("imageurl").toString()).apply(RequestOptions().circleCrop()).into(imageview_profile)

                }
            }



        comment_btn_send?.setOnClickListener {
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.userName = user_name
            comment.comment = comment_edit_message.text.toString()
            comment.timestamp = System.currentTimeMillis()


            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, comment_edit_message.text.toString(), comment.userName)
            comment_edit_message.setText("")
        }
    }

    fun commentAlarm(destinationUid: String, message: String, user_name: String?){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.userName = user_name
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var msg = user_name + " "+ getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid, "HYInstagram", msg)

    }

    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null) return@addSnapshotListener

                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent, false)
            return CustomViewHolder(view)
        }
        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.commentviewitem_textview_comment.text = comments[position].comment
            view.commentviewitem_textview_profile.text = comments[position].userName

            FirebaseFirestore.getInstance()
                .collection("Users")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        var url = task.result?.get("imageurl")
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.commentviewitem_imageview_profile)
                    }
                }
        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }



}