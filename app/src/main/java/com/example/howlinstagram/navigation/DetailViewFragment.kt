package com.example.howlinstagram.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment(){

    var firestore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()
        var user_name : String? = null

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

            firestore?.collection("Users")?.document(contentDTOs[p1].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url = task.result?.get("imageurl")
                        Glide.with(p0.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(viewholer.detailviewitem_profile_image)
                    }
                }

            viewholer.detailviewitem_profile_textview.text = contentDTOs!![p1].userName

            viewholer.user_name.text = contentDTOs!![p1].userName

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
                bundle.putString("userName", contentDTOs[p1].userName)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
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
                                this.user_name = task.result?.get("username").toString()
                            }
                        }
                    favoriteAlarm(contentDTOs[position].uid!!, this.user_name)
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
            Log.d("myapp", alarmDTO.userName!!)
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = user_name + " " + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "HYInstagram", message)
        }

    }
}