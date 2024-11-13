package com.example.rentify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RentifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Any additional setup if needed
    }
}
