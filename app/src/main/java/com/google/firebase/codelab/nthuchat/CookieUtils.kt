package com.google.firebase.codelab.nthuchat

import java.util.ArrayList

import cz.msebera.android.httpclient.cookie.Cookie
import com.loopj.android.http.*
import android.content.Context

object CookieUtils {

    private var cookies: List<Cookie>? = null

    /* 返回cookies列表 */
    fun getCookies(): List<Cookie>? {
        return if (cookies != null) cookies else ArrayList<Cookie>()
    }

    /* 设置cookies列表 */
    fun setCookies(cookies: List<Cookie>) {
        CookieUtils.cookies = cookies
    }

    /* 存储cookie */
    fun saveCookie(client: AsyncHttpClient?, context: Context) {
        val cookieStore = PersistentCookieStore(context)
        client?.setCookieStore(cookieStore)
    }

    /* 得到cookie */
    fun getCookie(context: Context): List<Cookie> {
        val cookieStore = PersistentCookieStore(context)
        return cookieStore.cookies
    }

    /* 清除cookie */
    fun clearCookie(context: Context) {
        val cookieStore = PersistentCookieStore(context)
        cookieStore.clear()
    }
}
