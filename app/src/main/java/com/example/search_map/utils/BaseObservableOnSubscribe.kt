package com.example.search_map.utility

import android.content.Context
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Api.ApiOptions.NotRequiredOptions
import com.google.android.gms.common.api.GoogleApiClient
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*

abstract class BaseObservableOnSubscribe<T> @SafeVarargs protected constructor(
    private val ctx: Context,
    vararg services: Api<out NotRequiredOptions?>?
) :
    ObservableOnSubscribe<T> {
    private val services: List<Api<out NotRequiredOptions?>>

    @Throws(Exception::class)
    override fun subscribe(emitter: ObservableEmitter<T>) {
        val apiClient = createApiClient(emitter)
        try {
            apiClient.connect()
        } catch (ex: Throwable) {
            if (!emitter.isDisposed) {
                emitter.onError(ex)
            }
        }
        emitter.setDisposable(Disposable.fromAction {
            onDisposed()
            apiClient.disconnect()
        })
    }

    private fun createApiClient(emitter: ObservableEmitter<in T>): GoogleApiClient {
        val apiClientConnectionCallbacks: ApiClientConnectionCallbacks =
            ApiClientConnectionCallbacks(
                ctx, emitter
            )
        var apiClientBuilder = GoogleApiClient.Builder(ctx)
        for (service in services) {
            apiClientBuilder = apiClientBuilder.addApi(service)
        }
        apiClientBuilder = apiClientBuilder
            .addConnectionCallbacks(apiClientConnectionCallbacks)
            .addOnConnectionFailedListener(apiClientConnectionCallbacks)
        val apiClient = apiClientBuilder.build()
        apiClientConnectionCallbacks.setClient(apiClient)
        return apiClient
    }

    protected fun onDisposed() {}
    protected abstract fun onGoogleApiClientReady(
        context: Context?,
        googleApiClient: GoogleApiClient?,
        emitter: ObservableEmitter<in T>?
    )

    private inner class ApiClientConnectionCallbacks(
        private val context: Context,
        private val emitter: ObservableEmitter<in T>
    ) :
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        private var apiClient: GoogleApiClient? = null
        override fun onConnected(bundle: Bundle?) {
            try {
                onGoogleApiClientReady(context, apiClient, emitter)
            } catch (ex: Throwable) {
                if (!emitter.isDisposed) {
                    emitter.onError(ex)
                }
            }
        }

        override fun onConnectionSuspended(cause: Int) {
            if (!emitter.isDisposed) {
                emitter.onError(IllegalStateException())
            }
        }

        override fun onConnectionFailed(connectionResult: ConnectionResult) {
            if (!emitter.isDisposed) {
                emitter.onError(IllegalStateException("Error connecting to GoogleApiClient"))
            }
        }

        fun setClient(client: GoogleApiClient?) {
            apiClient = client
        }
    }

    init {
        this.services = Arrays.asList(*services) as List<Api<out NotRequiredOptions?>>
    }
}
