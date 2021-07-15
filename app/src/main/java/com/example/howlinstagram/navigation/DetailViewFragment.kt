package com.example.howlinstagram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
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

        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
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

            viewholer.detailviewitem_profile_textview.text = contentDTOs!![p1].userId

            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholer.detailviewitem_imageview_content)

            viewholer.detailviewitem_explain_textview.text = contentDTOs!![p1].explain

            viewholer.detailviewitem_favoritecounter_textview.text = "Likes "+ contentDTOs!![p1].favoriteCount

            Glide.with(p0.itemView.context).load(contentDTOs!![p1].imageUrl).into(viewholer.detailviewitem_profile_image)

            viewholer.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(p1)
            }

            if(contentDTOs!![p1].favorites.containsKey(uid)){
                viewholer.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholer.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
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
                }

                transaction.set(tsDoc, contentDTO)
            }
        }

    }
}