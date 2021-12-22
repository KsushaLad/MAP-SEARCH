package com.example.search_map.utility

import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.reactivex.rxjava3.core.ObservableEmitter

abstract class BaseLocationObservableOnSubscribe<T> protected constructor(ctx: Context?) : //базовое местоположение при подписке
    BaseObservableOnSubscribe<T>(ctx!!, LocationServices.API) {
     override fun onGoogleApiClientReady(
        context: Context?,
        googleApiClient: GoogleApiClient?,
        emitter: ObservableEmitter<in T>?
    ) {
        onLocationProviderClientReady(
            LocationServices.getFusedLocationProviderClient(context!!),
            emitter
        )
    }

    protected abstract fun onLocationProviderClientReady(
        locationProviderClient: FusedLocationProviderClient?,
        emitter: ObservableEmitter<in T>?
    )
}