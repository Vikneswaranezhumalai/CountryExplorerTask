package com.txstate.countryexplorertask.network

import com.google.gson.Gson
import com.txstate.countryexplorertask.model.Country
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
class CountryRepository {
    suspend fun getCountries(): List<Country> {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://gist.githubusercontent.com/peymano-wmt/32dcb892b06648910ddd40406e37fdab/raw/db25946fd77c5873b0303b858e861ce724e0dcd0/countries.json")
                    .build()
                val response = client.newCall(request).execute()
                response.use {
                    val responseBody = it.body?.string()   // <-- Note the parentheses
                    println("Response: $responseBody")
                    if (!responseBody.isNullOrBlank()) {
                        val gson = Gson()
                        gson.fromJson(responseBody, Array<Country>::class.java).toList()
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}


