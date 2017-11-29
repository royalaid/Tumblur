package me.royalaid.tumblur

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import co.metalab.asyncawait.async
import kotlinx.android.synthetic.main.activity_scrolling.*
import me.royalaid.tumblur.MainActivity.Singleton.tumblr
import com.tumblr.jumblr.types.Post
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.anko.custom.async


class ScrollingActivity : AppCompatActivity() {

    private val dash = mutableListOf<Post>()

    class OnScrollListener(private val layoutManager: GridLayoutManager, private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, private val dataList: MutableList<Post>) : RecyclerView.OnScrollListener() {
        var previousTotal = 0
        var loading = true
        val visibleThreshold = 10
        var firstVisibleItem = 0
        var visibleItemCount = 0
        var totalItemCount = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            visibleItemCount = recyclerView.childCount
            totalItemCount = layoutManager.itemCount
            firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false
                    previousTotal = totalItemCount
                }
            }

            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                loading = true
                recyclerView.setHasTransientState(true)
                    async(CommonPool){
                        val initialSize = dataList.size

                        val lowestId = dataList.minBy { post -> post.likedTimestamp }?.likedTimestamp
                        val nextDash = tumblr.nextLikes(lowestId?:System.currentTimeMillis() / 1000)
                        async(UI){
                            dataList.addAll(nextDash.await())
                            val updatedSize = dataList.size
                            recyclerView.post { adapter.notifyItemRangeInserted(initialSize, updatedSize) }
                            val filter = dataList.groupBy { post -> post.id }
                                    .filter { (k, v) -> v.size > 1 }
                            recyclerView.setHasTransientState(false)
                        }.await()

                    }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar)

        val mRecyclerView:RecyclerView = findViewById(R.id.photo_recycler)

        runBlocking{
            dash.addAll(tumblr.nextLikes(0).await())
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true)
        val layoutManager  = GridLayoutManager(this, 3)

        mRecyclerView.layoutManager = layoutManager

        val mAdapter = MyAdapter(dash)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.addOnScrollListener(OnScrollListener(layoutManager, mAdapter, dash))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
