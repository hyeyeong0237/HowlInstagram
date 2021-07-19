package com.example.howlinstagram.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.EditProfileActivity
import com.example.howlinstagram.LoginActivity
import com.example.howlinstagram.MainActivity
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.AlarmDTO
import com.example.howlinstagram.navigation.model.ContentDTO
import com.example.howlinstagram.navigation.model.FollowDTO
import com.example.howlinstagram.navigation.model.UserDTO
import com.example.howlinstagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.fullname
import kotlinx.android.synthetic.main.fragment_user.username
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(){
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    var user_name : String? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        var mainactivity = (activity as MainActivity)
        mainactivity.my_toolbar.visibility = View.GONE

        if(uid == currentUserUid){
            fragmentView?.account_btn_follow_signout?.text = "EDIT PROFILE"
            fragmentView?.toolbar_btn_back?.visibility = View.GONE
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                startActivity(Intent(activity, EditProfileActivity::class.java))
            }
        }else{
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            fragmentView?.toolbar_btn_back?.visibility = View.VISIBLE
            fragmentView?.toolbar_btn_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }

        }

        fragmentView?.options?.setOnClickListener {
            activity?.finish()
            startActivity(Intent(activity, LoginActivity::class.java))
            auth?.signOut()
        }

        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(requireActivity(), 3)


        getUserInfo()
        getFollowerAndFollowing()

        return fragmentView
    }

    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()

            }
            if(followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text = context?.getString(R.string.follow_cancel)
                }else{
                    if(uid != currentUserUid){
                        fragmentView?.account_btn_follow_signout?.text = context?.getString(R.string.follow)
                    }
                }
            }
        }
    }
    fun requestFollow(){

        FirebaseFirestore.getInstance().collection("Users")?.document(auth?.currentUser?.uid!!)
            ?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful){
                    user_name= task.result?.get("username").toString()

                }
            }
        //save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction{ transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.following[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if(followDTO.following.containsKey(uid)){ //it remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount-1
                followDTO?.following?.remove(uid)
            }else{
                // It add following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount+1
                followDTO?.following[uid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction

        }
        //save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!, user_name)
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid!!)){
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!, user_name)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }

    }

    fun followerAlarm(destinationUid: String, user_name: String?){

        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.userName = user_name
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = user_name + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "HYInstagram", message)
    }

    fun getUserInfo(){

        if(isAdded){
            firestore?.collection("Users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                var userDTO = documentSnapshot.toObject(UserDTO::class.java)
                username?.text = userDTO?.username
                fullname?.text = userDTO?.fullname
                Glide.with(this).load(userDTO?.imageurl).apply(RequestOptions().circleCrop())
                    .into(fragmentView?.account_iv_profile!!)

            }
        }

    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
       var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageView = ImageView(p0.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageView)
        }
        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView){

        }

        override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
           var imageView = (p0 as CustomViewHolder).imageView
            Glide.with(p0.itemView.context).load(contentDTOs[p1].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}