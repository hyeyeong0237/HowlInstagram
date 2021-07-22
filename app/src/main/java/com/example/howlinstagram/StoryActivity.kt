package com.example.howlinstagram

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.example.howlinstagram.navigation.DetailViewFragment
import com.example.howlinstagram.navigation.model.StoryDTO
import com.example.howlinstagram.navigation.model.UserDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import jp.shts.android.storiesprogressview.StoriesProgressView
import kotlinx.android.synthetic.main.activity_story.*
import kotlinx.android.synthetic.main.story_item.view.*


class StoryActivity : AppCompatActivity(), StoriesProgressView.StoriesListener{

    var counter = 0
    var pressTime : Long = 0L
    var limit: Long = 500L

    var images : ArrayList<String>? = null
    var storyids : ArrayList<String>? = null

    var userid : String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        r_seen.visibility = View.GONE
        story_delete.visibility = View.GONE

        userid = intent.getStringExtra("userid")

        if(userid.equals(FirebaseAuth.getInstance().currentUser?.uid)){
            r_seen.visibility = View.VISIBLE
            story_delete.visibility = View.VISIBLE
        }

        getStories(userid)
        userInfo(userid)

        val onTouchListner = object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val action = event?.action
                when(action){
                    MotionEvent.ACTION_DOWN ->{
                        pressTime = System.currentTimeMillis();
                        stories.pause();
                    }
                    MotionEvent.ACTION_UP ->{
                        stories.resume();
                        limit < System.currentTimeMillis()- pressTime;
                    }
                }
                return false
            }

        }

        reverse.setOnClickListener {
            stories.reverse()
        }
        reverse.setOnTouchListener(onTouchListner)
        skip.setOnClickListener {
            stories.skip()
        }
        skip.setOnTouchListener(onTouchListner)

        story_delete.setOnClickListener {
            FirebaseFirestore.getInstance().collection("Story").document(userid!!).collection("userStories").document(storyids?.get(counter)!!).delete().addOnCompleteListener {
                task ->
                if(task.isSuccessful){
                    Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

    }

    fun getStories(userid: String?) {
        images = arrayListOf()
        storyids = arrayListOf()

        FirebaseFirestore.getInstance().collection("Story").document(userid!!).collection("userStories").orderBy("timestart")?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if( documentSnapshot == null) return@addSnapshotListener
            images!!.clear()
            storyids!!.clear()
            for(snapshot in  documentSnapshot.documents){
                var storyDTO = snapshot.toObject(StoryDTO::class.java)
                var timecurrent = System.currentTimeMillis()
                if (timecurrent > storyDTO?.timestart!! && timecurrent < storyDTO?.timeend!!){
                    images?.add(storyDTO?.imageurl!!)
                    storyids?.add(snapshot.id)
                }
            }

            Log.d("imagestory", images?.size.toString())
            stories.setStoriesCount(images?.size!!)
            stories.setStoryDuration(5000L)
            stories.setStoriesListener(this)
            if(images?.size == 0){
                finish()
                startActivity(Intent(this, MainActivity::class.java))
            }else{
                Glide.with(applicationContext).load(images?.get(counter)).into(image)
            }
            stories.startStories(counter)

            addView(storyids?.get(counter)!!)
            seenNumber(storyids?.get(counter)!!)
        }

    }

    fun userInfo(userid: String?){

        FirebaseFirestore.getInstance()?.collection("Users")?.document(userid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            var userDTO = documentSnapshot.toObject(UserDTO::class.java)
            Glide.with(this).load(userDTO?.imageurl).apply(RequestOptions().circleCrop())
                .into(story_photo)
            story_username.setText(userDTO?.username)


        }

    }
    fun addView(storyid : String){
        var tsDoc = FirebaseFirestore.getInstance().collection("Story").document(userid!!).collection("userStories").document(storyid)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            var storyDTO = transaction.get(tsDoc).toObject(StoryDTO::class.java)
            var currentid = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("keys", storyDTO?.views?.keys.toString())
            storyDTO?.views?.set(currentid, true)

            transaction.set(tsDoc, storyDTO!!)
        }
    }
    fun seenNumber(storyid : String){
        var tsDoc = FirebaseFirestore.getInstance().collection("Story").document(userid!!).collection("userStories").document(storyid)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            var storyDTO = transaction.get(tsDoc).toObject(StoryDTO::class.java)
            seen_number.setText(storyDTO?.views?.size.toString())

            transaction.set(tsDoc, storyDTO!!)
        }
    }

    override fun onNext() {

        Glide.with(applicationContext).load(images?.get(++counter)).into(image)
        addView(storyids?.get(counter)!!)
        seenNumber(storyids?.get(counter)!!)

    }

    override fun onPrev() {
        if ((counter - 1) < 0) return;
        Glide.with(applicationContext).load(images?.get(--counter)).into(image)
        seenNumber(storyids?.get(counter)!!)
    }

    override fun onComplete() {
        finish()
    }

    override fun onDestroy() {
        stories.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        stories.pause()
        super.onPause()
    }

    override fun onResume() {
        stories.resume()
        super.onResume()
    }
}