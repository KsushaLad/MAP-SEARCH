package com.example.search_map.geocoder.timezone

import android.content.Context
import android.content.pm.PackageManager
import com.google.maps.GeoApiContext
import com.google.maps.TimeZoneApi
import com.google.maps.errors.ApiException
import com.google.maps.model.LatLng
import java.io.IOException
import java.util.TimeZone

class GoogleTimeZoneDataSource(private val geoApiContext: GeoApiContext) {

    companion object {
        fun getApiKey(context: Context): String? { //получение ключа API
            try {
                val appInfo = context.packageManager.getApplicationInfo( //информация о приложении
                    context.packageName, PackageManager.GET_META_DATA)
                if (appInfo.metaData != null) {
                    return appInfo.metaData.getString("com.google.android.geo.API_KEY")
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }

            return null
        }
    }

    fun getTimeZone(latitude: Double, longitude: Double): TimeZone? { //получение часового пояса
        try {
            return TimeZoneApi.getTimeZone(geoApiContext, LatLng(latitude, longitude)).await()
        } catch (ignored: ApiException) {
        } catch (ignored: InterruptedException) {
        } catch (ignored: IOException) {
        }
        return null
    }
}