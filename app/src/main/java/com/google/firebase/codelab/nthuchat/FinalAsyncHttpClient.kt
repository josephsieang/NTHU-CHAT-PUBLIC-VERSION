package com.google.firebase.codelab.nthuchat

import cz.msebera.android.httpclient.cookie.Cookie
import cz.msebera.android.httpclient.impl.client.BasicCookieStore

import com.loopj.android.http.AsyncHttpClient

class FinalAsyncHttpClient {

    var client: AsyncHttpClient? = null

    init {
        client = AsyncHttpClient()
        client?.setConnectTimeout(5)//5s超时
        if (CookieUtils.getCookies() != null) {//每次请求都要带上cookie
            val bcs = BasicCookieStore()
            bcs.addCookies(CookieUtils.getCookies()!!.toTypedArray())
            client?.setCookieStore(bcs)
        }
    }

    fun getAsyncHttpClient(): AsyncHttpClient? {
        return this.client
    }

}
