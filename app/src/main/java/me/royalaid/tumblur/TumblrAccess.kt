package me.royalaid.tumblur

import com.tumblr.jumblr.JumblrClient
import com.tumblr.jumblr.types.Post
import kotlinx.coroutines.experimental.*
import me.royalaid.tumblur.TumblrAccess.Singleton.client
import uy.klutter.core.collections.toImmutable

/**
 * Created by gwmai on 11/25/2017.
 */

class TumblrAccess(consumerKey: String, consumerSecret: String,
                   token: String, secret: String) {
    object Singleton {
        val requestTokenUrl = "https://www.tumblr.com/oauth/request_token"
        val accessTokenUrl = "https://www.tumblr.com/oauth/access_token"
        var authUrl = "https://www.tumblr.com/oauth/authorize"

        lateinit var client: JumblrClient

        private val oauthCallbackScheme = "tumblrapp"
        private val oauthCallbackHost = "tumblrapp.com"
        val oauthCallbackUrl = "$oauthCallbackScheme://$oauthCallbackHost/ok"

    }
    init {
        Singleton.client = JumblrClient(consumerKey, consumerSecret)
        setToken(token, secret)
    }

    private fun setToken(token: String, secret: String): Unit {
        client.setToken(token, secret)
    }

    fun nextDash(id: Long): Deferred<List<Post>> {
        return async(CommonPool) {
            return@async client.userDashboard(mapOf("before_id" to id)).toImmutable()
        }
    }

    fun nextLikes(timestamp: Long): Deferred<List<Post>> {
        return async(CommonPool) {
            return@async client.userLikes(mapOf("before" to timestamp)).toImmutable()
        }
    }
}