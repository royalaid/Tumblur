package me.royalaid.tumblur

import android.content.Intent
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tumblr.jumblr.types.PhotoPost
import com.tumblr.jumblr.types.Post
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.google.gson.Gson
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import org.jetbrains.anko.find
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import java.util.*


/**
 * Created by Mark Aiken on 11/25/2017.
 */

// Provide a suitable constructor (depends on the kind of dataset)
class MyAdapter (val mDataset: List<Post>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val gson = Gson()
    private val ITEM = 0
    private val LOADING = 1
    private val isLoadingAdded = false
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(constraintLayout: RelativeLayout) : RecyclerView.ViewHolder(constraintLayout) {
        val imgView: ImageView = constraintLayout.find(R.id.post_img)
        val progress: ProgressBar = constraintLayout.find(R.id.post_img_progress)
        val imageLoader = GlideImageLoader(imgView, progress)
        val sizeProvider = ViewPreloadSizeProvider<Any>(constraintLayout)
        val modelProvider = MyPreloadModelProvider()
        val preloader = RecyclerViewPreloader(Glide.with(constraintLayout),
                modelProvider, sizeProvider, 10 /*maxPreload*/);
        init {
            constraintLayout.setOnClickListener { v ->
                run {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val clickedDataItem = mDataset[pos]
                        val postActivity = Intent(v.context, PostViewActivity::class.java)
                        if(clickedDataItem is PhotoPost)
                            postActivity.putExtra("original_size_url", clickedDataItem.photos.first().originalSize.url)
                        else
                            postActivity.putExtra("original_size_url", "")
                        startActivity(v.context, postActivity, null)
                    }
                }
            }
        }

        inner class MyPreloadModelProvider : PreloadModelProvider<Any>{
            override fun getPreloadRequestBuilder(item: Any?): RequestBuilder<*>? {
                return imageLoader.getRequestBuilder(item as String)
            }

            override fun getPreloadItems(position: Int): List<*> {
                mDataset[position].let {
                    return if( it is PhotoPost && !it.photos.isEmpty())
                        Collections.singletonList(it.photos[0].originalSize.url)
                    else
                        Collections.emptyList<String>()
                }
            }

        }
        fun loadUrl(url: String): Unit {
            imageLoader.load(url)
        }
    }

    private inner class LoadingVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder  {
        // create a new view
        val inflater = LayoutInflater.from(parent.context)
        when (viewType){
            ITEM -> {
                val v = inflater
                        .inflate(R.layout.my_image_view, parent, false)
                val lp = v.layoutParams as GridLayoutManager.LayoutParams
                lp.width = parent.measuredWidth/ 3
                lp.height = lp.width
                v.layoutParams = lp
                return ViewHolder(v as RelativeLayout)
            }
            LOADING -> {
                val v2 = inflater.inflate(R.layout.item_progress, parent, false)
                return LoadingVH(v2)
            }
            else -> {
                val emptyImageView = inflater.inflate(R.layout.my_image_view, parent, false)
                return ViewHolder(emptyImageView as RelativeLayout)
            }

        }
    }// set the view's size, margins, paddings and layout parameters

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        when(holder){
            is MyAdapter.ViewHolder -> {
                mDataset[position].let {
                    if( it is PhotoPost && !it.photos.isEmpty())
                        holder.loadUrl(it.photos[0].originalSize.url)
                }

            }
        }


    }



    override fun getItemViewType(position: Int): Int =
            if (position == mDataset.size - 1 && isLoadingAdded) LOADING else ITEM

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int = mDataset.size


}
