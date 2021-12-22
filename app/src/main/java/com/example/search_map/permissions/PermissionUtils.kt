package com.example.search_map.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    private fun shouldRequestPermission(context: Context, permission: String): Boolean { //запрашивание разрешения
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED
    }

    fun isLocationPermissionGranted(context: Context): Boolean { //разрешили ли
        return ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(activity: Activity, permission: String, requestCode: Int) { //запрашивание разрешения
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    fun shouldRequestLocationStoragePermission(context: Context): Boolean { //запрос на разрешение на хранение местоположения
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldRequestPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun requestLocationPermission(activity: Activity) { //запрос на определение местоположения
        requestPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION, 0)
    }
}