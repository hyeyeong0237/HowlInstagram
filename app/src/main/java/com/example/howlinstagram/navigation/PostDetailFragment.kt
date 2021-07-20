package com.example.howlinstagram.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.AlarmDTO
import com.example.howlinstagram.navigation.model.ContentDTO
import com.example.howlinstagram.navigation.model.FollowDTO
import com.example.howlinstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_detail.view.*

class PostDetailFragment : Fragment() {
    var contentUid : String? = null
    var destinationUid : String? = null
    var fragmentView : View? = null
    var user_name : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        contentUid = arguments?.getString("contentUid")
        destinationUid = arguments?.getString("destinationUid")

        fragmentView = LayoutInflater.from(activity).inflate(R.layout.item_detail, container, false)



            FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
                ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    if (documentSnapshot == null) return@addSnapshotListener

                    var item = documentSnapshot.toObject(ContentDTO::class.java)
                    fragmentView?.detailviewitem_profile_textview?.text = item?.userName
                    fragmentView?.user_name?.text = item?.userName
                    fragmentView?.detailviewitem_explain_textview?.text = item?.explain
                    if(isAdded) {
                        Glide.with(this).load(item?.imageUrl)
                            .into(fragmentView?.detailviewitem_imageview_content!!)
                    }
                    fragmentView?.detailviewitem_favoritecounter_textview?.text =
                        "좋아요 " + item?.favoriteCount + "개"

                    if (item?.favorites?.containsKey(FirebaseAuth.getInstance().currentUser?.uid) == true) {
                        fragmentView?.detailviewitem_favorite_imageview?.setImageResource(R.drawable.ic_favorite)
                    } else {
                        fragmentView?.detailviewitem_favorite_imageview?.setImageResource(R.drawable.ic_favorite_border)
                    }

                    fragmentView?.detailviewitem_comment_imageview?.setOnClickListener { v ->
                        var intent = Intent(v.context, CommentActivity::class.java)
                        intent.putExtra("contentUid", documentSnapshot.id)
                        intent.putExtra("destinationUid", item?.uid)
                        startActivity(intent)
                    }
                    fragmentView?.detailviewitem_favorite_imageview?.setOnClickListener {
                        favoriteEvent()
                    }


                }

            FirebaseFirestore.getInstance().collection("Users")?.document(destinationUid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        var url = task.result?.get("imageurl")
                        Glide.with(this).load(url).apply(RequestOptions().circleCrop())
                            .into(fragmentView?.detailviewitem_profile_image!!)

                    }
                }





        return fragmentView
    }

    fun favoriteEvent(){
        var tsDoc = FirebaseFirestore.getInstance().collection("images")?.document(contentUid!!)

        FirebaseFirestore.getInstance()?.runTransaction { transaction ->

            var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

            if (contentDTO?.favorites?.containsKey(FirebaseAuth.getInstance().currentUser?.uid) == true) {
                contentDTO?.favoriteCount = contentDTO?.favoriteCount!! -1
                contentDTO?.favorites?.remove(FirebaseAuth.getInstance().currentUser?.uid)
            } else {
                contentDTO?.favoriteCount = contentDTO?.favoriteCount!!+1
                contentDTO?.favorites[FirebaseAuth.getInstance().currentUser?.uid!!] = true

                FirebaseFirestore.getInstance().collection("Users")
                    ?.document(FirebaseAuth.getInstance().currentUser?.uid!!)
                    ?.get()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            this.user_name = task.result?.get("username").toString()
                        }
                    }
                Log.d("myapp", this.user_name!!)
                favoriteAlarm(destinationUid!!, this.user_name)

            }
            transaction.set(tsDoc, contentDTO)
        }


    }

    fun favoriteAlarm(destinationUid: String, user_name: String?){

        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 0
        alarmDTO.userName = user_name
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = user_name + " " + getString(R.string.alarm_favorite)
        FcmPush.instance.sendMessage(destinationUid, "HYInstagram", message)
    }

}