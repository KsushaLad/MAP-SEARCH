package com.example.search_map.utility

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import java.lang.Exception

 class LastKnownLocationObservableOnSubscribe  constructor(ctx: Context) : //последнее известное местоположение наблюдаемое при подписке
    BaseLocationObservableOnSubscribe<Location?>(ctx) {
    @SuppressLint("MissingPermission")
     override fun onLocationProviderClientReady(
        locationProviderClient: FusedLocationProviderClient?,
        emitter: ObservableEmitter<in Location?>?
    ) {
        locationProviderClient!!.lastLocation
            .addOnSuccessListener(OnSuccessListener { location ->
                if (emitter!!.isDisposed) return@OnSuccessListener
                if (location != null) {
                    emitter.onNext(location)
                }
                emitter.onComplete()
            })
            .addOnFailureListener(BaseFailureListener(emitter))
    }

    class BaseFailureListener<T>(private val emitter: ObservableEmitter<in T?>?) :
        OnFailureListener {
        override fun onFailure(exception: Exception) {
            if (emitter!!.isDisposed) return
            emitter.onError(exception)
            emitter.onComplete()
        }
    }

    companion object {
        fun createObservable(context: Context): @NonNull Observable<Location?> {

            return Observable.create(LastKnownLocationObservableOnSubscribe(context))
        }
    }
}

