package me.royalaid.tumblur

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tumblr.jumblr.types.PhotoPost
import com.tumblr.jumblr.types.Post
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.google.gson.Gson
import android.util.Log
import android.widget.*
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import org.jetbrains.anko.find


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
    inner class ViewHolder(private val constraintLayout: RelativeLayout) : RecyclerView.ViewHolder(constraintLayout) {

        init {
            constraintLayout.setOnClickListener({ v ->
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
            })
        }

        fun loadUrl(url: String): Unit {
            val imgView: ImageView = constraintLayout.find(R.id.post_img)
            val progress: ProgressBar = constraintLayout.find(R.id.post_img_progress)
            imgView.loadUrl(url, progress)
        }
    }

    private inner class LoadingVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun ImageView.loadUrl(url: String, progressBar: ProgressBar) {

        GlideApp.with(context)
                .load(url)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(p0: Drawable?, p1: Any?, p2: com.bumptech.glide.request.target.Target<Drawable>?, p3: DataSource?, p4: Boolean): Boolean {
                        Log.d(TAG, "OnResourceReady")
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(this)
    }


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
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
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
