package com.example.search_map.geocoder.api

class NetworkException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)
}