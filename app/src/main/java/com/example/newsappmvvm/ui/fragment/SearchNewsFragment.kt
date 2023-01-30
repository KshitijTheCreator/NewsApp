package com.example.newsappmvvm.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsappmvvm.R
import com.example.newsappmvvm.adapters.NewsAdapter
import com.example.newsappmvvm.ui.NewsActivity
import com.example.newsappmvvm.ui.NewsViewModel
import com.example.newsappmvvm.utils.Constants
import com.example.newsappmvvm.utils.Constants.Companion.SEARCH_NEWS_TIME_DELAY
import com.example.newsappmvvm.utils.Resource
import kotlinx.android.synthetic.main.fragment_breaking_news.*
import kotlinx.android.synthetic.main.fragment_search_news.*
import kotlinx.android.synthetic.main.fragment_search_news.paginationProgressBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SearchNewsFragment : Fragment(R.layout.fragment_search_news) {
    lateinit var viewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    val TAG = "SearchNewsFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as NewsActivity).viewModel
        setupRecyclerView()

        newsAdapter.setOnitemClickListener {
            val bundle = Bundle().apply{
                putSerializable("article", it)
            }
            findNavController().navigate(
                R.id.action_searchNewsFragment2_to_articleFragment2,
                bundle
            )
        }

        //we want our search to have a delay in making request calls while we type
        var job: Job? = null
        etSearch.addTextChangedListener{ editable ->
            job?.cancel()
            job = MainScope().launch{
                delay(SEARCH_NEWS_TIME_DELAY)
                editable?.let{
                    if(editable.toString().isNotEmpty()) {
                        viewModel.searchNews(editable.toString())
                    }
                }
            }
        }

        viewModel.searchNews.observe(viewLifecycleOwner, Observer { response->
            when(response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE +2
                        isLastPage = viewModel.searchNewsPage == totalPages

                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let{message ->
                        Toast.makeText(activity, "An error occoured: $message", Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })
    }

    private fun setupRecyclerView(){
        newsAdapter = NewsAdapter()
        rvSearchNews.apply{
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@SearchNewsFragment.scrollListener)
        }
    }

    private fun hideProgressBar(){
        paginationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar(){
        paginationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    var isLoading: Boolean = false
    var isLastPage: Boolean = false
    var isScrolling: Boolean = false

    var scrollListener = object : RecyclerView.OnScrollListener(){
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) isScrolling = true
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            //There is no default mechanism for checking that we have scrolled to the bottom or not so we have to check as follows

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage: Boolean = !isLoading && isLastPage
            val isAtLastItem: Boolean = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotBeginning: Boolean = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible: Boolean = totalItemCount >= Constants.QUERY_PAGE_SIZE

            val shouldPaginate: Boolean = isNotLoadingAndNotLastPage && isAtLastItem && isNotBeginning && isTotalMoreThanVisible && isScrolling

            if(shouldPaginate){
                viewModel.searchNews(etSearch.text.toString())
                isScrolling = false
            }
        }
    }
}