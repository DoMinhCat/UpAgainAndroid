package com.example.upagain.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {
    private val sharedPreferences = context.getSharedPreferences("app_cookies", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cookieStore = HashMap<String, MutableList<SerializableCookie>>()

    init {
        loadCookiesFromPrefs()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val serializableCookies = cookies.map { SerializableCookie.fromCookie(it) }
        cookieStore[host] = serializableCookies.toMutableList()
        saveCookiesToPrefs()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: return emptyList()
        val validCookies = mutableListOf<Cookie>()
        val iterator = cookies.iterator()
        while (iterator.hasNext()) {
            val serializableCookie = iterator.next()
            val cookie = serializableCookie.toCookie()
            if (cookie.expiresAt < System.currentTimeMillis()) {
                iterator.remove() // Expired cookie
            } else {
                validCookies.add(cookie)
            }
        }
        if (validCookies.size != cookies.size) {
            saveCookiesToPrefs()
        }
        return validCookies
    }

    @Synchronized
    fun clear() {
        cookieStore.clear()
        sharedPreferences.edit().clear().apply()
    }

    private fun saveCookiesToPrefs() {
        val json = gson.toJson(cookieStore)
        sharedPreferences.edit().putString("cookies", json).apply()
    }

    private fun loadCookiesFromPrefs() {
        val json = sharedPreferences.getString("cookies", null) ?: return
        try {
            val type = object : TypeToken<HashMap<String, MutableList<SerializableCookie>>>() {}.type
            val loadedStore: HashMap<String, MutableList<SerializableCookie>> = gson.fromJson(json, type)
            cookieStore.putAll(loadedStore)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private class SerializableCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean
    ) {
        fun toCookie(): Cookie {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)
            if (hostOnly) {
                builder.hostOnlyDomain(domain)
            } else {
                builder.domain(domain)
            }
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            return builder.build()
        }

        companion object {
            fun fromCookie(cookie: Cookie): SerializableCookie {
                return SerializableCookie(
                    name = cookie.name,
                    value = cookie.value,
                    expiresAt = cookie.expiresAt,
                    domain = cookie.domain,
                    path = cookie.path,
                    secure = cookie.secure,
                    httpOnly = cookie.httpOnly,
                    hostOnly = cookie.hostOnly
                )
            }
        }
    }
}
