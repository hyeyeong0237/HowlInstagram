package com.example.howlinstagram.navigation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.AddStoryActivity
import com.example.howlinstagram.R
import com.example.howlinstagram.StoryActivity
import com.example.howlinstagram.navigation.model.*
import com.example.howlinstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.add_story_item.view.*
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.fullname
import kotlinx.android.synthetic.main.fragment_user.username
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*
import kotlinx.android.synthetic.main.story_item.view.*
import kotlinx.android.synthetic.main.story_item.view.story_photo


class DetailViewFragment : Fragment(){

    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var fragmentview : View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentview= LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        fragmentview?.detailviewfragment_recyclerview?.adapter = DetailViewRecyclerViewAdapter()
        fragmentview?.detailviewfragment_recyclerview?.layoutManager = LinearLayoutManager(activity)
        fragmentview?.recycler_view_story?.adapter = StoryRecyclerViewAdapter()
        fragmentview?.recycler_view_story?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)


        return fragmentview
    }


    inner class StoryRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var stories: ArrayList<String> = arrayListOf()



        init {
            firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                stories.clear()
                stories.add(uid!!)
                var countStory = 0
                var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
                for(followingId in followDTO?.following?.keys!!){

                    firestore?.collection("Story")?.document(followingId)?.collection("userStories")?.whereEqualTo("userid", followingId)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                        if(querySnapshot == null) return@addSnapshotListener
                        countStory = 0
                        for(snapshot in querySnapshot.documents){
                            var storyDTO = snapshot.toObject(StoryDTO::class.java)
                            if (storyDTO?.timestart!! < System.currentTimeMillis()  && System.currentTimeMillis() < storyDTO.timeend!!){
                                countStory++
                            }
                        }
                        if(countStory > 0 && !stories.contains(followingId)){
                            stories.add(followingId)
                        }

                    }
                }
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view: View

            if(viewType == 0){
                view = LayoutInflater.from(parent.context).inflate(R.layout.add_story_item, parent, false)

            }else{
                view = LayoutInflater.from(parent.context).inflate(R.layout.story_item, parent, false)
            }

            return RecycleCustomViewHolder(view)

        }
        inner class RecycleCustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            var storyUid = stories.get(position)

            userInfo(view, storyUid, position)

            if (holder.adapterPosition != 0) {
                seenStory(view, storyUid)
            }
            if (holder.adapterPosition == 0) {
                myStory(view.addstory_text,view.story_plus, false)
            }
            view.setOnClickListener {
                if(holder.adapterPosition == 0){
                    myStory(view.addstory_text, view.story_photo, true)
                }else{
                    val intent = Intent(context, StoryActivity::class.java)
                    intent.putExtra("userid", storyUid)
                    startActivity(intent)
                }
            }


        }

        override fun getItemCount(): Int {
            Log.d("story1", stories.size.toString())
            return stories.size
        }

        override fun getItemViewType(position: Int): Int {
           if(position == 0){
               return 0
           }
            return 1
        }

    }


    fun userInfo(view: View, userid : String, position: Int){


            firestore?.collection("Users")?.document(userid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                var userDTO = documentSnapshot.toObject(UserDTO::class.java)
                if(isAdded) {
                    Glide.with(this).load(userDTO?.imageurl).apply(RequestOptions().circleCrop())
                        .into(view.story_photo)
                    if (position != 0) {
                        Glide.with(this).load(userDTO?.imageurl)
                            .apply(RequestOptions().circleCrop())
                            .into(view.story_photo_seen)
                        view.story_username.setText(userDTO?.username)
                    }
                }

            }

    }

    fun seenStory(view: View, userid: String){
        firestore?.collection("Story")?.document(userid)?.collection("userStories")?.whereEqualTo("userid", userid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if(querySnapshot == null) return@addSnapshotListener
            var i = 0
            for(snapshot in querySnapshot.documents){
                var storyDTO = snapshot.toObject(StoryDTO::class.java)
                if (storyDTO?.views?.containsKey(uid) == false && System.currentTimeMillis() < storyDTO.timeend!!){
                    i++
                }
            }

            if( i > 0){
                view.story_photo.visibility = View.VISIBLE
                view.story_photo_seen.visibility = View.GONE

            }else{
                view.story_photo.visibility = View.GONE
                view.story_photo_seen.visibility = View.VISIBLE
            }

        }
    }
    fun myStory(add_Text : TextView, story_plus : ImageView , click : Boolean){
        firestore?.collection("Story")?.document(uid!!)?.collection("userStories")?.whereEqualTo("userid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if(querySnapshot == null) return@addSnapshotListener
            var count = 0
            var timecurrent = System.currentTimeMillis()
            for(snapshot in querySnapshot.documents){
                 var storyDTO = snapshot.toObject(StoryDTO::class.java)
                if (storyDTO != null) {
                    if (timecurrent < storyDTO.timeend!! && timecurrent > storyDTO.timestart!!){
                        count++
                    }
                    }
            }


            if(click){

                if(count>0){
                    val alertDialogBuilder = AlertDialog.Builder(context)
                    alertDialogBuilder.setNeutralButton("View Story"){ dialog, which -> //TODO: go to story
                            val intent = Intent(context, StoryActivity::class.java)
                             intent.putExtra("userid", uid)
                             startActivity(intent)
                             dialog.dismiss()
                        }
                    alertDialogBuilder.setPositiveButton("Add Story"){ dialog, which ->
                             var intent = Intent(context, AddStoryActivity::class.java)
                             startActivity(intent)
                            dialog.dismiss()
                        }

                    alertDialogBuilder.show()

                }else{
                    var intent = Intent(context, AddStoryActivity::class.java)
                    startActivity(intent)
                }

            }else{
                if(count > 0) {
                    add_Text.setText("My story")
                    story_plus.visibility = View.GONE
                }else{
                    add_Text.setText("Add story")
                    story_plus.visibility = View.VISIBLE
                }
            }
        }
    }




    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {


            firestore?.collection("images")?.orderBy("timestamp",
                Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
          var view = LayoutInflater.from(p0.context).inflate(R.layout.item_detail, p0, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
            var viewholer = (p0 as CustomViewHolder).itemView

            firestore?.collection("Users")?.document(contentDTOs[p1].uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                var userDTO = documentSnapshot.toObject(UserDTO::class.java)

                Glide.with(p0.itemView.context).load(userDTO?.imageurl).apply(RequestOptions().circleCrop()).into(viewholer.detailviewitem_profile_image)
                viewholer.detailviewitem_profile_textview.text = userDTO?.username
                viewholer.user_name.text = userDTO?.username
                }


            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholer.detailviewitem_imageview_content)

            viewholer.detailviewitem_explain_textview.text = contentDTOs!![p1].explain

            viewholer.detailviewitem_favoritecounter_textview.text = "좋아요 "+ contentDTOs!![p1].favoriteCount+"개"


            viewholer.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(p1)
            }


            if(contentDTOs!![p1].favorites.containsKey(uid)){
                viewholer.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholer.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }


            viewholer.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[p1].uid)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.addToBackStack(null)?.commit()
            }

            viewholer.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[p1])
                intent.putExtra("destinationUid", contentDTOs[p1].uid)
                startActivity(intent)
            }


        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        fun favoriteEvent(position: Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    contentDTO.favoriteCount = contentDTO?.favoriteCount -1
                    contentDTO?.favorites.remove(uid)
                }else{
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount +1
                    contentDTO?.favorites[uid!!] = true
                    FirebaseFirestore.getInstance().collection("Users")?.document(FirebaseAuth.getInstance().currentUser?.uid!!)
                        ?.get()?.addOnCompleteListener { task ->
                            if(task.isSuccessful){
                                var user_name = task.result?.get("username").toString()
                                favoriteAlarm(contentDTOs[position].uid!!, user_name)
                            }
                        }

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
}