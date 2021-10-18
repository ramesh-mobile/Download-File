package com.avatar.inpsection.data.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming


/**
 * Created by ramesh on 19-07-2021
 */
interface RetrofitInterface {
    @GET("{path}")
    @Streaming
    fun downloadFile(@Path(value = "path", encoded = true) user: String?): Call<ResponseBody?>?
}