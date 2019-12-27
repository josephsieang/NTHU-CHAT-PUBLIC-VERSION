/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.nthuchat

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.facebook.stetho.Stetho
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.PersistentCookieStore
import com.loopj.android.http.RequestParams

import org.json.JSONObject

import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.cookie.Cookie

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.lang.String.valueOf


class SignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    companion object {

        private val TAG = "SignInActivity"
        private val RC_SIGN_IN = 9001
    }

    //var finalAsyncHttpClient: FinalAsyncHttpClient? = null
    var client: AsyncHttpClient? = null

    //private var mSignInButton1: Button? = null

    //private var mGoogleApiClient: GoogleApiClient? = null
    //private var mFirebaseUser: FirebaseUser? = null

    // Firebase instance variables
    private var mFirebaseAuth: FirebaseAuth? = null
    private var mIdView: EditText? = null
    private var mPasswordView: EditText? = null
    private var mFirebaseDB: FirebaseDatabase? = null
    //private var mFBdiv: DatabaseReference? = null
    var user: User? = null
    var dbinstance: AppDatabase? = null
    // var fire_div: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*if(!TextUtils.isEmpty(getCookieText())){
            Toast.makeText(SignInActivity.this,"Using Your Cookie",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            finish();
        }*/
        //Stetho.initializeWithDefaults(this);
        setContentView(R.layout.activity_sign_in)

        dbinstance = AppDatabase.getAppDatabase(applicationContext)
        if (dbinstance?.userDao()?.getUser() != null) {
            val targetUser: User? = dbinstance?.userDao()?.getUser()
            dbinstance?.userDao()?.delete(targetUser)
        }

        // Assign fields
        mIdView = findViewById<View>(R.id.Input_id) as EditText
        mIdView!!.setHintTextColor(Color.BLACK)
        mPasswordView = findViewById<View>(R.id.Input_pw) as EditText
        mPasswordView!!.setHintTextColor(Color.BLACK)
        var mSignInButton1: Button = findViewById<View>(R.id.sign_in_button_1) as Button

        // Set click listeners
        mSignInButton1.setOnClickListener(this)

        // Configure Google Sign In
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.default_web_client_id))
//                .requestEmail()
//                .build()
//        var mGoogleApiClient: GoogleApiClient = GoogleApiClient.Builder(this)
//                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
//                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
//                .build()

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseDB = FirebaseDatabase.getInstance()


    }

    public override fun onStart() {
        super.onStart()
        //Check if user is signed in (non-null) and update UI accordingly.
        val currentUser: FirebaseUser? = mFirebaseAuth?.getCurrentUser()
        //Toast.makeText(SignInActivity.this, "[signin.start]currentUser: "+currentUser, Toast.LENGTH_SHORT).show();
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        return true
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        // Reset errors.
        mIdView!!.error = null
        mPasswordView!!.error = null

        // Store values at the time of the login attempt.
        val Id = mIdView!!.text.toString()
        val password = mPasswordView!!.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView!!.error = getString(R.string.error_invalid_password)
            focusView = mPasswordView
            cancel = true
        }
        if (TextUtils.isEmpty(password)) {
            mPasswordView!!.error = getString(R.string.error_field_required)
            focusView = mPasswordView
            cancel = true
        }
        // Check for a valid email address.
        if (TextUtils.isEmpty(Id)) {
            mIdView!!.error = getString(R.string.error_field_required)
            focusView = mIdView
            cancel = true
        } else if (!isIdValid(Id)) {
            mIdView!!.error = getString(R.string.error_invalid_email)
            focusView = mIdView
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            login(Id, password)
        }
    }

    private fun isIdValid(Id: String): Boolean { return Id.length > 5 }

    private fun isPasswordValid(password: String): Boolean { return password.length > 0 }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    fun login(email: String, passwd: String) {
        val params = RequestParams()
        var finalAsyncHttpClient: FinalAsyncHttpClient = FinalAsyncHttpClient()
        client = finalAsyncHttpClient.getAsyncHttpClient()
        CookieUtils.saveCookie(client, this)
        val myCookieStore = PersistentCookieStore(this@SignInActivity)
        client?.setCookieStore(myCookieStore)
        params.put("account", email)
        params.put("password", passwd)
        params.put("secCode", "na")
        params.put("stay", "0")
        client?.post("http://lms.nthu.edu.tw/sys/lib/ajax/login_submit.php", params, object : AsyncHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<Header>, responseBody: ByteArray) {
                val result = String(responseBody)
                if (result != null) {
                    val test = result.substring(18, 22)
                    var fire_email = ""
                    var fire_passwd = ""
                    var fire_div = ""
                    var fire_name = ""

                    if (test == "true") {
                        try {
                            val jsonObj = JSONObject(result)
                            fire_email = jsonObj.getJSONObject("ret").getString("email")
                            fire_passwd = jsonObj.getJSONObject("ret").getString("name") + "_ilmschat"
                            fire_div = jsonObj.getJSONObject("ret").getString("divName")
                            fire_name = jsonObj.getJSONObject("ret").getString("name")
                            //Toast.makeText(LoginActivity.this,jsonObj.getString("ret"), Toast.LENGTH_SHORT).show();
                            //Toast.makeText(LoginActivity.this,jsonObj.getJSONObject("ret").getString("email"), Toast.LENGTH_SHORT).show();
                            //Log.d(TAG, jsonObj.getString("email"));
                            //Log.d(TAG, jsonObj.getString("status"));
                            createAccount(fire_email, fire_passwd)
                            signIn_ac(fire_email, fire_passwd, fire_div,fire_name, jsonObj)
                        } catch (e: Exception) {
                            Log.d(TAG, "Json login Firebase Fail")
                        }

                        //Toast.makeText(SignInActivity.this, "Login Access, cookie=" + getCookieText(), Toast.LENGTH_SHORT).show();
                        CookieUtils.setCookies(CookieUtils.getCookie(this@SignInActivity))
                        if (mFirebaseAuth!!.currentUser != null) {
                            var mFirebaseUser: FirebaseUser? = mFirebaseAuth!!.currentUser
                            //Toast.makeText(SignInActivity.this, "[login.auth]=" +mFirebaseAuth, Toast.LENGTH_SHORT).show();
                            //Toast.makeText(SignInActivity.this, "[login.user]=" +mFirebaseUser, Toast.LENGTH_SHORT).show();
                            startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        mPasswordView!!.error = getString(R.string.error_incorrect_password)
                        mPasswordView!!.requestFocus()
                    }
                } else {
                    Toast.makeText(this@SignInActivity, "Access Fail", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(statusCode: Int, headers: Array<Header>, responseBody: ByteArray, error: Throwable) {}
        })
    }

    private fun createAccount(email: String, password: String) {
        Log.d(TAG, "createAccount:$email")
        // [START create_user_with_email]
        mFirebaseAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        //Toast.makeText(SignInActivity.this, "Create Account Success", Toast.LENGTH_SHORT).show();
                    } else {
                        /*

                        JAVA version here!!!!
                        throw task.getException() ERROR!!!!

                        //Toast.makeText(SignInActivity.this, "Create Account Fail", Toast.LENGTH_SHORT).show();

                        try {
                            throw task.getException()
                        } catch (e: FirebaseAuthUserCollisionException) {
                            Log.w(TAG, "createUserWithEmail:failure CE", task.exception)
                        } catch (e: Exception) {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        }
                        */

                        //Kotlin version

                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        //Toast.makeText(this@CreateAccountActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        // [END create_user_with_email]
    }

    private fun getCookieText(): String {
        val myCookieStore = PersistentCookieStore(this@SignInActivity)
        val cookies = myCookieStore.cookies
        Log.d(TAG, "cookies.size() = " + cookies.size)
        CookieUtils.setCookies(cookies)
        for (cookie in cookies) {
            Log.d(TAG, cookie.name + " = " + cookie.value)
        }
        val sb = StringBuffer()
        for (i in cookies.indices) {
            val cookie = cookies[i]
            val cookieName = cookie.name
            val cookieValue = cookie.value
            if (!TextUtils.isEmpty(cookieName) && !TextUtils.isEmpty(cookieValue)) {
                sb.append("$cookieName=")
                sb.append("$cookieValue;")
            }
        }
        Log.e("cookie", sb.toString())
        return sb.toString()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.sign_in_button_1 -> attemptLogin()
        }
    }

    private fun signIn_ac(email: String, password: String, div: String, name: String, jsonObj: JSONObject) {
        Log.d(TAG, "signIn_ac:$email")
        Log.d(TAG, "mFirebaseAuth :" + mFirebaseAuth!!)
        // [START sign_in_with_email]
        mFirebaseAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task_sign ->
                    if (task_sign.isSuccessful) {
                        user = User()
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        var mFBdiv: DatabaseReference = mFirebaseDB!!.getReference("/users/" + mFirebaseAuth!!.currentUser!!.uid)
                        //Toast.makeText(SignInActivity.this, "Login Firebase Success.", Toast.LENGTH_SHORT).show();
                        val user = User()
                        mFBdiv.child("div").setValue(div)
                        mFBdiv.child("name").setValue(name)
                        user.Div = div
                        client?.get("http://lms.nthu.edu.tw/home.php", object : AsyncHttpResponseHandler() {
                            override fun onFailure(statusCode: Int, headers: Array<Header>,
                                                   responseBody: ByteArray, error: Throwable) {
                            }

                            override fun onSuccess(statusCode: Int, headers: Array<Header>, data: ByteArray) {
                                val json = String(data)
                                val document_unbox = Jsoup.parse(json)
                                analysecourse(document_unbox, user)
                                dbinstance?.userDao()?.insertAll(user)
                                startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                                finish()
                            }
                        })

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task_sign.exception)
                        Toast.makeText(this@SignInActivity, "SignIn failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        // [END sign_in_with_email]
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    fun analysecourse(document: Document, user: User) {
        val elements = document.select("div.mnuItem>a")
        var title_name = ""
        for (i in 0 until elements.size - 1) {
            var final_name = ""
            val title = elements[i].text().split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            //Log.d(TAG, title);
            for (m in title.indices) {
                if (title[m].matches("[A-Za-z0-9() &,-]*".toRegex())) {
                    title[m] = ""
                } else {
                    final_name += title[m]
                }
            }
            title_name += "$final_name#"
            //Toast.makeText(this, final_name, Toast.LENGTH_SHORT).show();
        }
        user.Classes = title_name
    }

}
