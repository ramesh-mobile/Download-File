package com.sr.dummyapidemo.data.di.module

import com.avatar.inpsection.data.network.RetrofitInterface
import com.sr.savefileproject.BuildConfig
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by ramesh on 17-10-2021
 */
@Module
class RetroModule {
    val BASE_URL = "https://dummy.restapiexample.com/api/v1/"

    @Singleton
    @Provides
    fun getHttpClient() : OkHttpClient{
        return OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(
                (if (BuildConfig.DEBUG) HttpLoggingInterceptor().setLevel(
                    HttpLoggingInterceptor.Level.NONE
                ) else HttpLoggingInterceptor())
            )
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.MINUTES).build()
    }

    @Singleton
    @Provides
    fun getRetrofitInstance(okHttpClient: OkHttpClient): Retrofit{
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            //.addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Singleton
    @Provides
    fun getRetrofitInsterfaceInstance(retrofit: Retrofit) : RetrofitInterface{
        return retrofit.create(RetrofitInterface::class.java)
    }
}