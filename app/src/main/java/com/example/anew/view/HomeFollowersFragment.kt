package com.example.anew.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.anew.R
import com.example.anew.adapter.HomePostsAdapter
import com.example.anew.adapter.OnClickListenerCatchData
import com.example.anew.databinding.FragmentHomeFollowersBinding
import com.example.anew.viewmodel.HomeFollowersViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFollowersFragment : Fragment(R.layout.fragment_home_followers) {

    @Inject
    internal lateinit var auth: FirebaseAuth
    @Inject
    internal lateinit var db: FirebaseFirestore
    @Inject
    lateinit var glide: RequestManager
    private lateinit var adapter: HomePostsAdapter
    private lateinit var binding: FragmentHomeFollowersBinding
    private lateinit var viewModel: HomeFollowersViewModel
    private var followedUserList: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeFollowersBinding.bind(view)
        viewModel = ViewModelProvider(this)[HomeFollowersViewModel::class.java]

        setupBottomNavigationView()
        setupButtonClick()

       auth.currentUser?.let {
           CoroutineScope(Dispatchers.IO).launch {
               viewModel.fetchUserData(auth.uid!!)
           }
           viewModel.userData.observe(viewLifecycleOwner){
               followedUserList = it.details?.listFollow!!
               glide.load(it.details?.profileImg)
                   .placeholder(R.mipmap.ic_none_img)
                   .error(R.mipmap.ic_none_img)
                   .diskCacheStrategy(DiskCacheStrategy.ALL)
                   .centerCrop()
                   .into(binding.circleImage)

               CoroutineScope(Dispatchers.IO).launch {
                   viewModel.fetchPosts(followedUserList)
               }
           }


           adapter = HomePostsAdapter(emptyList(), object : OnClickListenerCatchData {
               override fun onProfileImageClick(senderId: String) {
                   if(senderId == auth.uid){
                       findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
                   }else{
                       findNavController().navigate(R.id.action_homeFragment_to_profileViewerFragment,
                           bundleOf("senderId" to senderId)
                       )
                   }

               }
           }, object : OnClickListenerCatchData {
               override fun onProfileImageClick(senderId: String) {
                   CoroutineScope(Dispatchers.IO).launch { viewModel.checkLike(senderId) }
                   viewModel.checkLike.observe(viewLifecycleOwner){
                       if(it){
                           viewModel.disLike(senderId)
                           CoroutineScope(Dispatchers.Main).launch {
                               viewModel.refreshPostData(followedUserList)
                               viewModel.postsData.observe(viewLifecycleOwner){
                                   adapter.setData(it)
                               }
                           }
                       }else{
                           viewModel.like(senderId)
                           CoroutineScope(Dispatchers.Main).launch {
                               viewModel.refreshPostData(followedUserList)
                               viewModel.postsData.observe(viewLifecycleOwner){
                                   adapter.setData(it)
                               }
                           }
                       }
                   }
               }

           }, object : OnClickListenerCatchData {
               override fun onProfileImageClick(senderId: String) {
                   findNavController().navigate(R.id.action_homeFragment_to_postViewerFragment,
                       bundleOf("postID" to senderId)
                   )
               }
           })
       }

        viewModel.postsData.observe(viewLifecycleOwner){
            adapter.setData(it)
            val layoutManager = LinearLayoutManager(requireContext())
            binding.homeRecycler.layoutManager = layoutManager
            binding.homeRecycler.adapter = adapter
        }
    }

    private fun setupBottomNavigationView(){
        binding.bottomNavigationView.setOnNavigationItemReselectedListener {

            when(it.itemId){

                R.id.ic_action_search -> {
                    Log.e("search","click")
                    findNavController().navigate(R.id.action_homeFollowersFragment_to_searchFragment)
                    false
                }
                R.id.ic_action_notification -> {
                    Log.e("notification","click")
                    findNavController().navigate(R.id.action_homeFollowersFragment_to_notificationsFragment)
                    false
                }
                R.id.ic_action_inbox -> {
                    Log.e("inbox","click")
                    findNavController().navigate(R.id.action_homeFollowersFragment_to_inboxFragment)
                    false
                }

                else -> false
            }
        }
        val menuItem = binding.bottomNavigationView.menu.getItem(0)
        menuItem.isChecked = true
    }

    private fun setupButtonClick(){
        binding.circleImage.setOnClickListener {
            findNavController().navigate(R.id.action_homeFollowersFragment_to_profileFragment)
        }
        binding.buttonAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_homeFollowersFragment_to_shareFragment)
        }
        binding.textViewEveryone.setOnClickListener {
            findNavController().navigate(R.id.action_homeFollowersFragment_to_homeFragment)
        }
    }
}

