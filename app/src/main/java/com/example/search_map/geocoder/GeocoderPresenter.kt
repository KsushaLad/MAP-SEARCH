package com.example.search_map.geocoder

import android.annotation.SuppressLint
import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.example.search_map.geocoder.places.GooglePlacesDataSource
import com.example.search_map.geocoder.timezone.GoogleTimeZoneDataSource
import com.example.search_map.utils.ReactiveLocationProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.TimeZone
import kotlin.collections.ArrayList

private const val RETRY_COUNT = 3


class GeocoderPresenter @JvmOverloads constructor(
    private val locationProvider: ReactiveLocationProvider,
    private val geocoderRepository: GeocoderRepository,
    private val googlePlacesDataSource: GooglePlacesDataSource? = null,
    private val googleTimeZoneDataSource: GoogleTimeZoneDataSource? = null,
    private val scheduler: Scheduler = AndroidSchedulers.mainThread()
) {

    private var view: GeocoderViewInterface? = null
    private val nullView = GeocoderViewInterface.NullView()
    private val compositeDisposable = CompositeDisposable()
    private var isGooglePlacesEnabled = false

    init {
        this.view = nullView
    }

    fun setUI(geocoderViewInterface: GeocoderViewInterface) {
        this.view = geocoderViewInterface
    }

    fun stop() {
        this.view = nullView
        compositeDisposable.clear()
    }

    fun getLastKnownLocation() { //получение последнего доступного местоположения
        @SuppressLint("MissingPermission")
        val disposable = locationProvider.getLastKnownLocation()
            .retry(RETRY_COUNT.toLong())
            .subscribe({ view?.showLastLocation(it!!) },
                { view?.didGetLastLocation() })
        compositeDisposable.add(disposable)
    }

    fun getFromLocationName(query: String) {
        view?.willLoadLocation()
        val disposable = geocoderRepository.getFromLocationName(query)
            .observeOn(scheduler)
            .subscribe({ view?.showLocations(it) },
                { view?.showLoadLocationError() },
                { view?.didLoadLocation() })
        compositeDisposable.add(disposable)
    }

    fun getDebouncedFromLocationName(query: String, debounceTime: Int) { //получение подсказки от имени местоположения
        view?.willLoadLocation()
        val disposable = geocoderRepository.getFromLocationName(query)
            .debounce(debounceTime.toLong(), TimeUnit.MILLISECONDS, Schedulers.io())
            .observeOn(scheduler)
            .subscribe({ view?.showDebouncedLocations(it) },
                { view?.showLoadLocationError() },
                { view?.didLoadLocation() })
        compositeDisposable.add(disposable)
    }

    fun getInfoFromLocation(latLng: LatLng) { //получение информации из местоположения
        view?.willGetLocationInfo(latLng)
        val disposable = geocoderRepository.getFromLocation(latLng)
            .observeOn(scheduler)
            .retry(RETRY_COUNT.toLong())
            .filter { addresses -> addresses.isNotEmpty() }
            .map { addresses -> addresses[0] }
            .flatMap { address -> returnTimeZone(address) }
            .subscribe({ pair: Pair<Address, TimeZone?> -> view?.showLocationInfo(pair) },
                { view?.showGetLocationInfoError() },
                { view?.didGetLocationInfo() })
        compositeDisposable.add(disposable)
    }

    private fun returnTimeZone(address: Address): ObservableSource<out Pair<Address, TimeZone?>>? { //часовой пояс
        return Observable.just(
            Pair(address, googleTimeZoneDataSource?.getTimeZone(address.latitude, address.longitude))
        ).onErrorReturn { Pair(address, null) }
    }

    fun enableGooglePlaces() { //включение адресов
        this.isGooglePlacesEnabled = true
    }
}