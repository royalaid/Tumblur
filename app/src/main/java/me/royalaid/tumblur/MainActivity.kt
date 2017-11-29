package me.royalaid.tumblur

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import co.metalab.asyncawait.async
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import me.royalaid.tumblur.MainActivity.Singleton.auth
import me.royalaid.tumblur.MainActivity.Singleton.browser
import me.royalaid.tumblur.MainActivity.Singleton.browser2
import me.royalaid.tumblur.MainActivity.Singleton.consumer
import me.royalaid.tumblur.MainActivity.Singleton.provider
import me.royalaid.tumblur.MainActivity.Singleton.tumblr
import me.royalaid.tumblur.TumblrAccess.Singleton.accessTokenUrl
import me.royalaid.tumblur.TumblrAccess.Singleton.authUrl
import me.royalaid.tumblur.TumblrAccess.Singleton.oauthCallbackUrl
import me.royalaid.tumblur.TumblrAccess.Singleton.requestTokenUrl
import oauth.signpost.OAuth
import oauth.signpost.OAuthConsumer
import oauth.signpost.OAuthProvider
import oauth.signpost.exception.OAuthCommunicationException
import oauth.signpost.exception.OAuthExpectationFailedException
import oauth.signpost.exception.OAuthMessageSignerException
import oauth.signpost.exception.OAuthNotAuthorizedException
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer
import se.akerfeldt.okhttp.signpost.OkHttpOAuthProvider


class MainActivity : AppCompatActivity() {
    data class Result(val authUrl:String, val token:String, val tokenSecret:String)

    object Singleton {
        var auth = false
        var browser = false
        var browser2 = false
        lateinit var consumer: OkHttpOAuthConsumer
        var provider = OkHttpOAuthProvider(requestTokenUrl, accessTokenUrl, authUrl)

        lateinit var tumblr:TumblrAccess


    }

    private lateinit var pref: SharedPreferences

    private lateinit var token:String
    private lateinit var secret:String

    private lateinit var newIntent: Intent

    private lateinit var debug: String
    private lateinit var uriPath: String

    private var loggedIn = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val consumerKey = getString(R.string.consumer_key)
        val consumerSecret = getString(R.string.consumer_secret)

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        token = pref.getString("TUMBLR_OAUTH_TOKEN", "")
        secret = pref.getString("TUMBLR_OAUTH_TOKEN_SECRET", "")

        if(token != "" && secret != "")
        {
            auth = true
            loggedIn = true
            tumblr = TumblrAccess(consumerKey, consumerSecret, token,secret)
        }
        else
            setAuthURL()

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        Log.i("onResume","Top of resume")
        if (!auth) {
            if (browser){
                Log.i("onResume","In browser")
                browser2 = true
            }

            if (!browser) {
                Log.i("onResume","In not browser")
                browser = true
                newIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                startActivity(newIntent)
            }

            if (browser2) {
                Log.i("onResume","In browser2")
                val uri = intent.data
                uriPath = uri.toString()

                if (uriPath.startsWith(oauthCallbackUrl)) {
                    val verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER)
                    async {
                        val (authedToken, authedSecret) =
                        await {
                            provider.retrieveAccessToken(consumer, verifier)
                            return@await consumer.token to consumer.tokenSecret
                        }
                        val consumerKey = getString(R.string.consumer_key)
                        val consumerSecret = getString(R.string.consumer_secret)
                        tumblr = TumblrAccess(consumerKey, consumerSecret, authedToken, authedSecret)
                        token = authedToken
                        secret = authedSecret
                        val editor = pref.edit();
                        editor.putString("TUMBLR_OAUTH_TOKEN", token);
                        editor.putString("TUMBLR_OAUTH_TOKEN_SECRET", secret);
                        editor.apply()
                    }
                    Log.i("onResume","Collected verifier $verifier")

                    try {

                        auth = true
                        loggedIn = true

                    } catch (e: OAuthMessageSignerException) {
                        e.printStackTrace()
                    } catch (e: OAuthNotAuthorizedException) {
                        e.printStackTrace()
                    } catch (e: OAuthExpectationFailedException) {
                        e.printStackTrace()
                    } catch (e: OAuthCommunicationException) {
                        e.printStackTrace()
                    }

                }
            }
        } else{
            setContentView(R.layout.activity_main)
            debug = "Access Token: $token\n\nAccess Token Secret: $secret"

            loginorout.setOnClickListener { logInOrOut() }

            updateLoginStatus()
        }

        if(!auth && browser2)
            finish()
    }

    private fun setAuthURL() {
        if(token == "" && secret == "" && !auth && !browser){
            val mAuthTask = RequestTask()
            val (auth, token, secret) = mAuthTask.execute(provider to consumer).get()
            authUrl = auth
        }
    }

    private fun logInOrOut() {
        if (isAuthenticated())
            logout()
        else {
            browser2 = false
            browser = browser2
            auth = browser
            setAuthURL()
            browser = true
            newIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
            startActivity(newIntent)
        }
    }

    //Removes the shared preferences values and sets the current token and secret to null essentially logging the user out of Tumblr
    private fun logout() {
        val edit = pref.edit()
        edit.remove("TUMBLR_OAUTH_TOKEN")
        edit.remove("TUMBLR_OAUTH_TOKEN_SECRET")
        edit.remove("TUMBLR_BLOG_NAME")
        edit.apply()

        token = ""
        secret = ""

        val consumerKey = getString(R.string.consumer_key)
        val consumerSecret = getString(R.string.consumer_secret)

        consumer = OkHttpOAuthConsumer(consumerKey, consumerSecret)
        provider = OkHttpOAuthProvider(requestTokenUrl, accessTokenUrl, authUrl)

        debug = "Access Token: $token\n\nAccess Token Secret: $secret\n\n"

        loggedIn = false

        updateLoginStatus()
    }

    //Updates a TextView telling us whether or not we are logged into Tumblr
    private fun updateLoginStatus() {
        if (isAuthenticated())
            loginorout.text = getString(R.string.logoutText)
        else
            loginorout.text = getString(R.string.logInText)
    }

    //Returns whether or not the user is logged into Tumblr successfully
    private fun isAuthenticated(): Boolean {
        return loggedIn
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun onClicker(view: View){
        val scrolling = Intent(this, ScrollingActivity::class.java)
        startActivity(scrolling)
//        async {
//                        view.isEnabled
//            val userName = await { client.user().name }
//            val dash = await { client.userDashboard()}
//            Toast.makeText(this@MainActivity, userName, Toast.LENGTH_LONG).show()
//            val imageView = findViewById<ImageView>(R.id.imageView)
//            imageView.loadUrl((dash[0] as PhotoPost).photos[0].originalSize.url)
//        }

    }

    class RequestTask : AsyncTask<Pair<OAuthProvider, OAuthConsumer>, Void, Result>() {

        override fun doInBackground(vararg params: Pair<OAuthProvider, OAuthConsumer>): Result{
            val (provider: OAuthProvider, consumer: OAuthConsumer) = params[0]
            return try {
                val authUrl = provider.retrieveRequestToken(consumer, oauthCallbackUrl)
                Result(authUrl, consumer.token, consumer.tokenSecret)
            } catch (e: Exception) {
                Log.e("Exception", e.toString())
                Result("","","")
            }
        }

        override fun onPreExecute() {}

        override fun onPostExecute(result: Result) {
            Log.e("LoginUsingTumblrActivity", "onPostExecute")
            }
    }
}

