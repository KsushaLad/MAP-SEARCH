package com.example.search_map

import com.example.search_map.tracker.LocationPickerTracker
import com.example.search_map.tracker.TrackEvents

object LocationPicker { //выбор местоположения

    private val EMPTY_TRACKER = EmptyLocationPickerTracker()

    private var tracker: LocationPickerTracker = EMPTY_TRACKER

    fun getTracker(): LocationPickerTracker {
        return tracker
    }

    class EmptyLocationPickerTracker : LocationPickerTracker {
        override fun onEventTracked(event: TrackEvents) { } //отслеживание событий
    }
}