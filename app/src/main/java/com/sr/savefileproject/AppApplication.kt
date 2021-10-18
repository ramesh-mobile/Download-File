package com.sr.savefileproject

import android.app.Application

/**
 * Created by ramesh on 16-10-2021
 */
class AppApplication : Application(){

    companion object{
        val CHANNEL_ID = "projectChannelId"
        val CHANNEL_NAME = "projectChannelName"
    }

    override fun onCreate() {
        super.onCreate()
    }
}