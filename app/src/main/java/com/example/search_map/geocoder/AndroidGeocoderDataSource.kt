package com.example.search_map.geocoder

import android.location.Address
import android.location.Geocoder

import com.google.android.gms.maps.model.LatLng
import java.io.IOException

import io.reactivex.rxjava3.core.Observable

private const val MAX_RESULTS = 5

class AndroidGeocoderDataSource(private val geocoder: Geocoder) : GeocoderInteractorDataSource {

    override fun getFromLocationName(query: String): Observable<List<Address>> { //получение из имени местоположения
        return Observable.create { emitter ->
            try {
                emitter.onNext(geocoder.getFromLocationName(query, MAX_RESULTS))
                emitter.onComplete()
            } catch (e: IOException) {
                emitter.tryOnError(e)
            }
        }
    }

    override fun getFromLocationName(query: String, lowerLeft: LatLng, upperRight: LatLng): Observable<List<Address>> { //получение из имени местоположения
        return Observable.create { emitter ->
            try {
                emitter.onNext(geocoder.getFromLocationName(query, MAX_RESULTS, lowerLeft.latitude,
                    lowerLeft.longitude, upperRight.latitude, upperRight.longitude))
                emitter.onComplete()
            } catch (e: IOException) {
                emitter.tryOnError(e)
            }
        }
    }

    override fun getFromLocation(latitude: Double, longitude: Double): Observable<List<Address>> { //получения из местоположения
        return Observable.create { emitter ->
            try {
                emitter.onNext(geocoder.getFromLocation(latitude, longitude, MAX_RESULTS))
                emitter.onComplete()
            } catch (e: IOException) {
                emitter.tryOnError(e)
            }
        }
    }
}
