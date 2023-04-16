package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.preference.DialogPreference
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.adapter.AdapterFeedList
import com.example.realtodoapp.databinding.DialogInterestProgressBinding
import com.example.realtodoapp.databinding.FragmentCommunityBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.example.realtodoapp.util.RetrofitUtil
import kotlin.streams.toList

class CommunityFragment: Fragment() {
    lateinit var fragmentCommunityBinding: FragmentCommunityBinding
    lateinit var dialogInterestProgressBinding: DialogInterestProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentCommunityBinding = FragmentCommunityBinding.inflate(layoutInflater)
        dialogInterestProgressBinding = DialogInterestProgressBinding.inflate(layoutInflater)

        var feedList = mutableListOf<FeedDto>()

        var feedRecyclerView = fragmentCommunityBinding.feedRecyclerView
        RetrofitUtil.getAllFeeds(
            successCallback = {
                feedList = it.toMutableList()
                var sortedFeedList = feedList.stream().
                    sorted(Comparator.comparing(FeedDto::feed_time).reversed()).toList() // 최신순 정렬
                var feedRecyclerViewAdapter = setFeedRecyclerView(feedRecyclerView, sortedFeedList)
            },
            failCallback = {
            }
        )


        val view = fragmentCommunityBinding.root
        return view
    }

    fun setFeedRecyclerView(recyclerView: RecyclerView, list:List<FeedDto>): AdapterFeedList {

        recyclerView.adapter = AdapterFeedList(requireContext(), requireActivity(), list, dialogInterestProgressBinding)
        val adapter = recyclerView.adapter as AdapterFeedList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }
}