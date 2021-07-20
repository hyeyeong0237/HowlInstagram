package com.example.howlinstagram.navigation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlinstagram.MainActivity
import com.example.howlinstagram.R
import com.example.howlinstagram.navigation.model.UserDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.android.synthetic.main.item_user.view.*

class SearchFragment : Fragment() {

    var firestore : FirebaseFirestore? = null
    var fragmentView : View? = null
    var UserDTOs: ArrayList<UserDTO> = arrayListOf()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var mainactivity = (activity as MainActivity)
        mainactivity.my_toolbar.visibility = View.GONE
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_search, container, false)
        firestore = FirebaseFirestore.getInstance()
        fragmentView?.search_recycler_view?.adapter= SearchFragmentRecyclerViewAdapter(UserDTOs)
        fragmentView?.search_recycler_view?.layoutManager = LinearLayoutManager(context)
        readUsers()
        fragmentView?.search?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
        fragmentView?.close?.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        return fragmentView
    }
    fun searchUsers(s: String){
        firestore?.collection("Users")?.orderBy("username")?.startAt(s)?.endAt(s+"\uf8ff")
            ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener
                UserDTOs.clear()
                for (snapshot in querySnapshot.documents) {
                    UserDTOs.add(snapshot.toObject(UserDTO::class.java)!!)
                }
                fragmentView?.search_recycler_view?.removeAllViews()
                SearchFragmentRecyclerViewAdapter(UserDTOs).notifyDataSetChanged()

            }


    }
    fun readUsers(){

        firestore?.collection("Users")
            ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (querySnapshot == null) return@addSnapshotListener
                UserDTOs.clear()
                for (snapshot in querySnapshot.documents) {
                    UserDTOs.add(snapshot.toObject(UserDTO::class.java)!!)
                }

                fragmentView?.search_recycler_view?.removeAllViews()
                SearchFragmentRecyclerViewAdapter(UserDTOs).notifyDataSetChanged()
            }

    }

    inner class SearchFragmentRecyclerViewAdapter(var userDTOs: ArrayList<UserDTO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(p0.context).inflate(R.layout.item_user, p0, false)
            return SearchCustomViewHolder(view)
        }

        private inner class SearchCustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.search_username.text = userDTOs[position].username
            view.search_fullname.text = userDTOs[position].fullname
            Glide.with(holder.itemView.context).load(userDTOs[position].imageurl)
                .apply(RequestOptions().circleCrop()).into(view.search_imageview)

            view.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", userDTOs[position].id)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.main_content, fragment)?.addToBackStack(null)?.commit()
            }
        }


        override fun getItemCount(): Int {
            return userDTOs.size
        }

    }


}