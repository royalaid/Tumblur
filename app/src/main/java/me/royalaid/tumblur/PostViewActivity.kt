package me.royalaid.tumblur

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.google.gson.Gson
import com.tumblr.jumblr.types.Post
import kotlinx.android.synthetic.main.activity_post_view.*
import pl.droidsonroids.gif.GifImageView

class PostViewActivity : AppCompatActivity() {
    private val gson = Gson()
    private lateinit var post:Post

    private fun GifImageView.loadUrl(url: String) {
        GlideApp.with(context).load(url).into(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_view)
        setSupportActionBar(toolbar)

        val url = intent.getStringExtra("original_size_url")
        val postImageView: GifImageView = findViewById(R.id.post_image_view)
        if (url.isNotEmpty()){
            postImageView.loadUrl(url)
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
