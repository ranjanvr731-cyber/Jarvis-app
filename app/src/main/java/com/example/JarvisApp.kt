package com.example

import android.app.Application
import com.example.data.AppDatabase

class JarvisApp : Application() {

    // Lazy initialization of database
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: JarvisApp
            private set
    }
}
