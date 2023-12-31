package com.example.anew.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.anew.R
import com.example.anew.adapter.HomePostsAdapter
import com.example.anew.adapter.OnClickListenerCatchData
import com.example.anew.databinding.FragmentProfileViewerBinding
import com.example.anew.viewmodel.PostViewerViewModel
import com.example.anew.viewmodel.ProfileViewerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ProfileViewerFragment : Fragment(R.layout.fragment_profile_viewer) {

    companion object {
        fun newInstance(senderId: String) : ProfileViewerFragment {
            val fragment = ProfileViewerFragment()
            val args = Bundle()
            args.putString("senderId", senderId)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    lateinit var auth: FirebaseAuth
    @Inject
    lateinit var db: FirebaseFirestore
    @Inject
    lateinit var glide: RequestManager
    private lateinit var viewModel: ProfileViewerViewModel
    private lateinit var binding : FragmentProfileViewerBinding
    private lateinit var adapter: HomePostsAdapter
    private lateinit var senderId : String
    private var inMainNav: Boolean? = null



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileViewerBinding.bind(view)
        viewModel = ViewModelProvider(this)[ProfileViewerViewModel::class.java]



        senderId = requireArguments().getString("senderId").toString()
        inMainNav = requireArguments().getBoolean("inMainNav")

        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchUserData(senderId)
            viewModel.fetchPosts(senderId)
            viewModel.checkFollowButton(senderId)

        }

        binding.imageViewSendMsg.setOnClickListener {
            if(inMainNav == true){
                findNavController().navigate(R.id.action_profileViewerFragment_to_chatFragment2,
                    bundleOf("senderId" to senderId))
            }else{
                findNavController().navigate(R.id.action_profileViewerFragment2_to_chatFragment3,
                    bundleOf("senderId" to senderId))
            }
           /* if(){
                findNavController().navigate(R.id.action_profileViewerFragment_to_chatFragment2, bundleOf("senderId" to senderId))
            }
            else{
                findNavController().navigate(R.id.action_profileViewerFragment2_to_chatFragment3)
            }*/
           /* viewModel.inMainNav.observe(viewLifecycleOwner){
                if (it){

                }else{

                    )
                }
            }*/
        }

        CoroutineScope(Dispatchers.Main).launch{
            getUserData()
            getPostData()
            viewModel.buttonCheck.observe(viewLifecycleOwner){
                checkFollowingForButton(it)
            }

        }


        adapter = HomePostsAdapter(emptyList(), object : OnClickListenerCatchData{
            override fun onProfileImageClick(senderId: String) {

            }
        },object : OnClickListenerCatchData{
            override fun onProfileImageClick(senderId: String) {

            }

        }, object : OnClickListenerCatchData{
            override fun onProfileImageClick(senderId: String) {
                findNavController().navigate(R.id.action_profileViewerFragment_to_postViewerFragment,
                    bundleOf("postID" to senderId))
            }
        })

        binding.buttonFollow.setOnClickListener {
            viewModel.checkIfFollowing(senderId)
            viewModel.checkFollowing.observe(viewLifecycleOwner){
                if (it){
                    followButton()
                    CoroutineScope(Dispatchers.Main).launch { viewModel.unfollowUser(senderId) }
                    viewModel.checkUserUpdate.observe(viewLifecycleOwner){
                        if(it){
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.refreshUserData(senderId)
                                viewModel.userData.observe(viewLifecycleOwner) { user ->
                                    binding.textViewFollowed.text = user.details?.followed.toString()
                                    binding.textViewFollowers.text = user.details?.followers.toString()
                                }
                            }
                        }else{
                            Log.d("checkUserUpdate", "false")
                        }
                    }
                }else{
                    CoroutineScope(Dispatchers.Main).launch { viewModel.followUser(senderId) }
                    unfollowButton()
                    viewModel.checkUserUpdate.observe(viewLifecycleOwner){
                        if(it){
                            CoroutineScope(Dispatchers.Main).launch {
                                viewModel.refreshUserData(senderId)
                                viewModel.userData.observe(viewLifecycleOwner) { user ->
                                    binding.textViewFollowed.text = user.details?.followed.toString()
                                    binding.textViewFollowers.text = user.details?.followers.toString()
                                }
                            }
                        }else{
                            Log.d("checkUserUpdate", "false")
                        }
                    }
                }
            }
        }

    }
    private fun getPostData(){
        viewModel.postsData.observe(viewLifecycleOwner){
            adapter.setData(it)
            adapter.notifyDataSetChanged()

            val layoutManager = LinearLayoutManager(requireContext())
            binding.profileRecycler.layoutManager = layoutManager
            binding.profileRecycler.adapter = adapter
        }
    }


    private  fun checkFollowingForButton(follow: Boolean){
            if(follow){
                unfollowButton()
            }
            else{
                followButton()
            }
    }

    private fun unfollowButton(){
        binding.buttonFollow.setBackgroundResource(R.drawable.active_bt_bg)
        binding.buttonFollow.text = "Unfollow"
        binding.buttonFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }
    private fun followButton(){
        binding.buttonFollow.setBackgroundResource(R.drawable.bt_login_bg)
        binding.buttonFollow.text = "Follow"
        binding.buttonFollow.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
    }

    private  fun getUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.userData.observe(viewLifecycleOwner) { user ->
                binding.textViewUsername.text = user?.username
                binding.textViewName.text = user.details?.name
                binding.textViewBio.text = user.details?.bio
                binding.textViewFollowed.text = user.details?.followed.toString()
                binding.textViewFollowers.text = user.details?.followers.toString()

                glide.load(user.details?.profileImg)
                    .placeholder(R.mipmap.ic_none_img)
                    .error(R.mipmap.ic_launcher)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.circleImageView)
            }
        }
    }

}