package com.example.search_map.geocoder

import android.location.Address
import android.location.Location

import com.google.android.gms.maps.model.LatLng
import java.util.TimeZone

interface GeocoderViewInterface {
    fun willLoadLocation() //загрузка местоположения
    fun showLocations(addresses: List<Address>) //показ местоположений
    fun showDebouncedLocations(addresses: List<Address>) //показ отклоненных местоположений
    fun didLoadLocation() //загрузка
    fun showLoadLocationError() //показ ошибки загрузки
    fun showLastLocation(location: Location) //показ последнего местоположения
    fun didGetLastLocation() //получение последнего местоположения
    fun showLocationInfo(address: Pair<Address, TimeZone?>) //показ информации о местоположении
    fun willGetLocationInfo(latLng: LatLng) //получение информации о местоположении
    fun didGetLocationInfo() //получение информации о местоположении
    fun showGetLocationInfoError() //получение местоположения с информацией об ошибке

    class NullView : GeocoderViewInterface {
        override fun willLoadLocation() {}
        override fun showLocations(addresses: List<Address>) {}
        override fun showDebouncedLocations(addresses: List<Address>) {}
        override fun didLoadLocation() {}
        override fun showLoadLocationError() {}
        override fun showLastLocation(location: Location) {}
        override fun didGetLastLocation() {}
        override fun showLocationInfo(address: Pair<Address, TimeZone?>) {}
        override fun willGetLocationInfo(latLng: LatLng) {}
        override fun didGetLocationInfo() {}
        override fun showGetLocationInfoError() {}
    }
}