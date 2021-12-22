package com.example.search_map.tracker

enum class TrackEvents(val eventName: String) {
    GOOGLE_API_CONNECTION_FAILED("Connection Failed"),
    ON_LOAD_LOCATION_PICKER("Location Picker"),
    ON_SEARCH_LOCATIONS("Click on search for locations"),
    ON_LOCALIZED_ME("Click on localize me"),
    ON_LOCALIZED_BY_POI("Long click on map"),
    SIMPLE_ON_LOCALIZE_BY_POI("Click on map"),
}