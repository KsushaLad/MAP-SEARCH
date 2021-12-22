package com.example.search_map

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.GeoApiContext
import com.example.search_map.geocoder.AndroidGeocoderDataSource
import com.example.search_map.geocoder.GeocoderPresenter
import com.example.search_map.geocoder.GeocoderRepository
import com.example.search_map.geocoder.GeocoderViewInterface
import com.example.search_map.geocoder.GoogleGeocoderDataSource
import com.example.search_map.geocoder.api.AddressBuilder
import com.example.search_map.geocoder.api.NetworkClient
import com.example.search_map.geocoder.places.GooglePlacesDataSource
import com.example.search_map.geocoder.timezone.GoogleTimeZoneDataSource
import com.example.search_map.permissions.PermissionUtils
import com.example.search_map.tracker.TrackEvents
import com.example.search_map.utils.ReactiveLocationProvider
import java.util.Locale
import java.util.TimeZone

const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val TRANSITION_BUNDLE = "transition_bundle"
const val LAYOUTS_TO_HIDE = "layouts_to_hide"
const val SEARCH_ZONE = "search_zone"
const val SEARCH_ZONE_DEFAULT_LOCALE = "search_zone_default_locale"
const val BACK_PRESSED_RETURN_OK = "back_pressed_return_ok"
const val ENABLE_SATELLITE_VIEW = "enable_satellite_view"
const val ENABLE_LOCATION_PERMISSION_REQUEST = "enable_location_permission_request"
const val ENABLE_GOOGLE_TIME_ZONE = "enable_google_time_zone"
const val POIS_LIST = "pois_list"
const val MAP_STYLE = "map_style"
const val UNNAMED_ROAD_VISIBILITY = "unnamed_road_visibility"
const val WITH_LEGACY_LAYOUT = "with_legacy_layout"
private const val GEOLOC_API_KEY = "geoloc_api_key"
private const val PLACES_API_KEY = "places_api_key"
private const val LOCATION_KEY = "location_key"
private const val LAST_LOCATION_QUERY = "last_location_query"
private const val OPTIONS_HIDE_STREET = "street"
private const val OPTIONS_HIDE_CITY = "city"
private const val OPTIONS_HIDE_ZIPCODE = "zipcode"
private const val UNNAMED_ROAD_WITH_COMMA = "Unnamed Road, "
private const val UNNAMED_ROAD_WITH_HYPHEN = "Unnamed Road - "
private const val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
private const val DEFAULT_ZOOM = 16
private const val WIDER_ZOOM = 6
private const val MIN_CHARACTERS = 2
private const val DEBOUNCE_TIME = 400


class LocationPickerActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener,
    GoogleMap.OnMapLongClickListener,
    GeocoderViewInterface,
    GoogleMap.OnMapClickListener {

    private var map: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private var currentLocation: Location? = null
    private var currentPoi: Poi? = null
    private var geocoderPresenter: GeocoderPresenter? = null
    private var adapter: ArrayAdapter<String>? = null
    private var searchView: EditText? = null
    private var street: TextView? = null
    private var coordinates: TextView? = null
    private var longitude: TextView? = null
    private var latitude: TextView? = null
    private var city: TextView? = null
    private var zipCode: TextView? = null
    private var locationInfoLayout: FrameLayout? = null
    private var progressBar: ProgressBar? = null
    private var listResult: ListView? = null
    private var searchResultsList: RecyclerView? = null
    private var searchAdapter: RecyclerView.Adapter<*>? = null
    private lateinit var linearLayoutManager: RecyclerView.LayoutManager
    private var clearSearchButton: ImageView? = null
    private var searchOption: MenuItem? = null
    private var searchEditLayout: LinearLayout? = null
    private var searchFrameLayout: FrameLayout? = null
    private val locationList = ArrayList<Address>()
    private var locationNameList: MutableList<String> = ArrayList()
    private var hasWiderZoom = false
    private val bundle = Bundle()
    private var selectedAddress: Address? = null
    private var isLocationInformedFromBundle = false
    private var isStreetVisible = true
    private var isCityVisible = true
    private var isZipCodeVisible = true
    private var shouldReturnOkOnBackPressed = false
    private var enableSatelliteView = true
    private var enableLocationPermissionRequest = true
    private var googlePlacesApiKey: String? = null
    private var isGoogleTimeZoneEnabled = false
    private var searchZone: String? = null
    private var isSearchZoneWithDefaultLocale = false
    private var poisList: List<Poi>? = null
    private var currentMarker: Marker? = null
    private var textWatcher: TextWatcher? = null
    private var googleGeocoderDataSource: GoogleGeocoderDataSource? = null
    private var isUnnamedRoadVisible = true
    private var mapStyle: Int? = null
    private var isLegacyLayoutEnabled = false
    private var isSearchLayoutShown = false
    private lateinit var timeZone: TimeZone

    private val searchTextWatcher: TextWatcher //наблюдатель за изменением текста
        get() = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
                if ("" == charSequence.toString()) {
                    if (isLegacyLayoutEnabled) {
                        adapter?.let {
                            it.clear()
                            it.notifyDataSetChanged()
                        }
                    } else {
                        searchAdapter?.notifyDataSetChanged()
                    }
                    showLocationInfoLayout()
                    clearSearchButton?.visibility = View.INVISIBLE
                } else {
                    if (charSequence.length > MIN_CHARACTERS) {
                        retrieveLocationWithDebounceTimeFrom(charSequence.toString())
                    }
                    clearSearchButton?.visibility = View.VISIBLE
                    searchOption?.setIcon(R.drawable.ic_search)
                    searchOption?.isVisible = true
                }
            }
            override fun afterTextChanged(editable: Editable) {
            }
        }

    private val defaultZoom: Int //увеличивание по умолчанию
        get() {
            return if (hasWiderZoom) {
                WIDER_ZOOM //более широкий зум
            } else {
                DEFAULT_ZOOM //по умолчанию
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateValuesFromBundle(savedInstanceState)
        setContentView(R.layout.activity_search)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setUpMainVariables()
        setUpResultsList()
        checkLocationPermission()
        setUpSearchView()
        setUpMapIfNeeded()
        setUpFloatingButtons()
        buildGoogleApiClient()
        track(TrackEvents.ON_LOAD_LOCATION_PICKER)
    }

    private fun checkLocationPermission() { //проверка разрешение на расположение
        if (enableLocationPermissionRequest && //включение запроса разрешений на определение местоположений
            PermissionUtils.shouldRequestLocationStoragePermission(applicationContext)) {
            PermissionUtils.requestLocationPermission(this)
        }
    }

    private fun track(event: TrackEvents) { //отслеживание событий
        LocationPicker.getTracker().onEventTracked(event)
    }

    private fun setUpMainVariables() { //инициализация используемых переменных
        var placesDataSource: GooglePlacesDataSource? = null //источник данных мест
        if (!Places.isInitialized() && !googlePlacesApiKey.isNullOrEmpty()) {
            googlePlacesApiKey?.let {
                Places.initialize(applicationContext, it)
            }
            placesDataSource = GooglePlacesDataSource(Places.createClient(this))
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        if (googleGeocoderDataSource == null) {
            googleGeocoderDataSource = GoogleGeocoderDataSource(NetworkClient(), AddressBuilder())
        }
        val geocoderRepository = GeocoderRepository(AndroidGeocoderDataSource(geocoder), googleGeocoderDataSource!!)
        val timeZoneDataSource = GoogleTimeZoneDataSource(GeoApiContext.Builder().apiKey(GoogleTimeZoneDataSource.getApiKey(this)).build())
        geocoderPresenter = GeocoderPresenter(ReactiveLocationProvider(applicationContext), geocoderRepository, placesDataSource, timeZoneDataSource)
        geocoderPresenter?.setUI(this)
        progressBar = findViewById(R.id.loading_progress_bar)
        progressBar?.visibility = View.GONE
        locationInfoLayout = findViewById(R.id.location_info)
        longitude = findViewById(R.id.longitude)
        latitude = findViewById(R.id.latitude)
        street = findViewById(R.id.street)
        coordinates = findViewById(R.id.coordinates)
        city = findViewById(R.id.city)
        zipCode = findViewById(R.id.zipCode)
        clearSearchButton = findViewById(R.id.clear_search_image)
        clearSearchButton?.setOnClickListener {
            searchView?.setText("")
        }
        locationNameList = ArrayList()
        searchEditLayout = findViewById(R.id.search_touch_zone)
        searchFrameLayout = findViewById(R.id.search_frame_layout)
    }

    private fun setUpResultsList() { //список результатов поиска
            linearLayoutManager = LinearLayoutManager(this)
            searchAdapter = LocationSearchAdapter(locationNameList, object : LocationSearchAdapter.SearchItemClickListener {
                    override fun onItemClick(position: Int) {
                        if (locationList[position].hasLatitude() && locationList[position].hasLongitude()) {
                            setNewLocation(locationList[position])
                            changeListResultVisibility(View.GONE)
                            closeKeyboard()
                            hideSearchLayout()
                        }
                    }
                }
            )
            searchResultsList = findViewById<RecyclerView>(R.id.search_result_list).apply {
                setHasFixedSize(true)
                layoutManager = linearLayoutManager
                adapter = searchAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
    }

    private fun setUpSearchView() { //настройка представление поиска
        searchView = findViewById(R.id.search)
        searchView?.setOnEditorActionListener { v, actionId, _ ->
            var handled = false //обработка
            if (actionId == EditorInfo.IME_ACTION_SEARCH && v.text.toString().isNotEmpty()) {
                retrieveLocationFrom(v.text.toString())
                closeKeyboard()
                handled = true
            }
            handled
        }
        textWatcher = searchTextWatcher
        searchView?.addTextChangedListener(textWatcher)
        if (!isLegacyLayoutEnabled) {
            searchView?.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    showSearchLayout()
                }
            }
        }
    }

    private fun showSearchLayout() { //открытие макета поиска
        searchFrameLayout?.setBackgroundResource(R.color.app_white)
        searchEditLayout?.setBackgroundResource(R.drawable.search_text_with_border_background)
        searchResultsList?.visibility = View.VISIBLE
        isSearchLayoutShown = true
    }

    private fun hideSearchLayout() { //закрытие макета поиска
        searchFrameLayout?.setBackgroundResource(android.R.color.transparent)
        searchEditLayout?.setBackgroundResource(R.drawable.search_text_background)
        searchResultsList?.visibility = View.GONE
        searchView?.clearFocus()
        isSearchLayoutShown = false
    }

    private fun setUpFloatingButtons() {
        val button_my_location = findViewById<FloatingActionButton>(R.id.btnMyLocation)
        button_my_location.setOnClickListener {
            checkLocationPermission()
            geocoderPresenter?.getLastKnownLocation()
            track(TrackEvents.ON_LOCALIZED_ME)
        }
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) { //обновление значений
        val transitionBundle = intent.extras
        transitionBundle?.let {
            getTransitionBundleParams(it) //получение параметров для обновления значений
        }
        savedInstanceState?.let {
            getSavedInstanceParams(it) //получение параметров сохраненного экземпляра
        }
        updateAddressLayoutVisibility()
        if (!googlePlacesApiKey.isNullOrEmpty()) {
            geocoderPresenter?.enableGooglePlaces()
        }
    }

    private fun setUpMapIfNeeded() { //настройка карты по необходимости
        if (map == null) {
            (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) { //результат по запросу разрешений
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtils.isLocationPermissionGranted(applicationContext)) {
            geocoderPresenter?.getLastKnownLocation() //если предоставилось разрешение то получить последнюю локацию
        }
    }

    override fun onStart() {
        super.onStart()
        googleApiClient?.connect()
        geocoderPresenter?.setUI(this)
    }

    override fun onStop() {
        googleApiClient?.let {
            if (it.isConnected) {
                it.disconnect()
            }
        }
        geocoderPresenter?.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        setUpMapIfNeeded()
    }

    override fun onDestroy() {
        textWatcher?.let {
            searchView?.removeTextChangedListener(it)
        }
        googleApiClient?.unregisterConnectionCallbacks(this)
        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (map == null) {
            map = googleMap
            setDefaultMapSettings()
            setCurrentPositionLocation()
        }
    }

    override fun onConnected(savedBundle: Bundle?) {
        if (currentLocation == null) {
            geocoderPresenter?.getLastKnownLocation()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        googleApiClient?.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult.hasResolution()) { //если есть разрешение
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST)
            } catch (e: IntentSender.SendIntentException) {
                track(TrackEvents.GOOGLE_API_CONNECTION_FAILED)
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        currentLocation?.let {
            savedInstanceState.putParcelable(LOCATION_KEY, it)
        }
        searchView?.let {
            savedInstanceState.putString(LAST_LOCATION_QUERY, it.text.toString())
        }
        if (bundle.containsKey(TRANSITION_BUNDLE)) {
            savedInstanceState.putBundle(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE))
        }
        poisList?.let {
            savedInstanceState.putParcelableArrayList(POIS_LIST, ArrayList(it))
        }
        savedInstanceState.putBoolean(ENABLE_SATELLITE_VIEW, enableSatelliteView)
        savedInstanceState.putBoolean(ENABLE_LOCATION_PERMISSION_REQUEST, enableLocationPermissionRequest)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastQuery = savedInstanceState.getString(LAST_LOCATION_QUERY, "")
        if ("" != lastQuery) {
            retrieveLocationFrom(lastQuery)
        }
        currentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        if (currentLocation != null) {
            setCurrentPositionLocation()
        }
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        }
        if (savedInstanceState.containsKey(POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(POIS_LIST)
        }
        if (savedInstanceState.containsKey(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = savedInstanceState.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (savedInstanceState.containsKey(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        currentPoi = null
        setNewPosition(latLng)
        track(TrackEvents.ON_LOCALIZED_BY_POI)
    }

    override fun onMapClick(latLng: LatLng) {
        currentPoi = null
        setNewPosition(latLng)
        track(TrackEvents.SIMPLE_ON_LOCALIZE_BY_POI)
    }

    private fun setNewPosition(latLng: LatLng) { //новая локация
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.network_resource))
        }
        currentLocation?.latitude = latLng.latitude
        currentLocation?.longitude = latLng.longitude
        setCurrentPositionLocation()
    }

    override fun willLoadLocation() { //загрузка местоположения
        progressBar?.visibility = View.VISIBLE
        changeListResultVisibility(View.GONE)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun showLocations(addresses: List<Address>) { //показ локаций
        fillLocationList(addresses) //заполнение списка с локациями
        if (addresses.isEmpty()) {
            Toast.makeText(applicationContext, R.string.no_search_results, Toast.LENGTH_LONG).show()
        } else {
            updateLocationNameList(addresses)
            if (hasWiderZoom) {
                searchView?.setText("")
            }
            if (addresses.size == 1) {
                setNewLocation(addresses[0])
            }
            if (isLegacyLayoutEnabled) {
                adapter?.notifyDataSetChanged()
            } else {
                searchAdapter?.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun showDebouncedLocations(addresses: List<Address>) { //показ отклоненных местоположений
        fillLocationList(addresses)
        if (addresses.isNotEmpty()) {
            updateLocationNameList(addresses)
        } else {
            setNoSearchResultsOnList()
        }
        if (isLegacyLayoutEnabled) {
            adapter?.notifyDataSetChanged()
        } else {
            searchAdapter?.notifyDataSetChanged()
        }
    }

    private fun setNoSearchResultsOnList() { //"нет параметров в списке"
        val noResultsAddress = Address(Locale.getDefault())
        locationList.add(noResultsAddress)
        locationNameList.clear()
        locationNameList.add(getString(R.string.no_search_results))
    }

    override fun didLoadLocation() { //загрузка
        progressBar?.visibility = View.GONE
        changeListResultVisibility(if (locationList.size >= 1) View.VISIBLE else View.GONE)
        if (locationList.size == 1) {
            changeLocationInfoLayoutVisibility(View.VISIBLE)
        } else {
            changeLocationInfoLayoutVisibility(View.GONE)
        }
        track(TrackEvents.ON_SEARCH_LOCATIONS)
    }

    private fun changeListResultVisibility(visibility: Int) { //изменение видимости результатов списка
        if (isLegacyLayoutEnabled) {
            listResult?.visibility = visibility
        } else {
            searchResultsList?.visibility = visibility
        }
    }

    private fun changeLocationInfoLayoutVisibility(visibility: Int) { //изменение видимости макета сведений о местоположении
        locationInfoLayout?.visibility = visibility
    }

    private fun showCoordinatesLayout() { //показ макет с координатами
        longitude?.visibility = View.VISIBLE
        latitude?.visibility = View.VISIBLE
        coordinates?.visibility = View.VISIBLE
        street?.visibility = View.GONE
        city?.visibility = View.GONE
        zipCode?.visibility = View.GONE
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun showAddressLayout() { //показ макета с адресом
        longitude?.visibility = View.GONE
        latitude?.visibility = View.GONE
        coordinates?.visibility = View.GONE
        if (isStreetVisible) {
            street?.visibility = View.VISIBLE
        }
        if (isCityVisible) {
            city?.visibility = View.VISIBLE
        }
        if (isZipCodeVisible) {
            zipCode?.visibility = View.VISIBLE
        }
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun updateAddressLayoutVisibility() { //обновление видимости макета с долготой и широтой
        street?.visibility = if (isStreetVisible) View.VISIBLE else View.INVISIBLE
        city?.visibility = if (isCityVisible) View.VISIBLE else View.INVISIBLE
        zipCode?.visibility = if (isZipCodeVisible) View.VISIBLE else View.INVISIBLE
        longitude?.visibility = View.VISIBLE
        latitude?.visibility = View.VISIBLE
        coordinates?.visibility = View.VISIBLE
    }

    override fun showLoadLocationError() { //показать ошибку о загрузке локации
        progressBar?.visibility = View.GONE
        changeListResultVisibility(View.GONE)
        Toast.makeText(this, R.string.load_location_error, Toast.LENGTH_LONG).show()
    }

    override fun willGetLocationInfo(latLng: LatLng) { //получение информации о местоположении
        changeLocationInfoLayoutVisibility(View.VISIBLE)
        resetLocationAddress()
        setCoordinatesInfo(latLng)
    }

    override fun showLastLocation(location: Location) { //показ последнего местоположения
        currentLocation = location
        didGetLastLocation()
    }

    override fun didGetLastLocation() { //получил ли последнее местоположение
        if (currentLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show()
                return
            }
            setUpMapIfNeeded()
        }
        setUpDefaultMapLocation()
    }

    override fun showLocationInfo(address: Pair<Address, TimeZone?>) { //показать информацию о местоположении
        selectedAddress = address.first
        address.second?.let {
            timeZone = it
        }
        selectedAddress?.let {
            setLocationInfo(it)
        }
    }

    private fun setLocationEmpty() { //установить местоположение "Пусто"
        this.street?.text = ""
        this.city?.text = ""
        this.zipCode?.text = ""
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    override fun didGetLocationInfo() { //получение информации о местоположении
        showLocationInfoLayout()
    }

    override fun showGetLocationInfoError() { //показ о получении информации с ошибкой
        setLocationEmpty()
    }

    private fun showLocationInfoLayout() { //показ макета  информации о местоположении
        changeLocationInfoLayoutVisibility(View.VISIBLE)
    }

    private fun getSavedInstanceParams(savedInstanceState: Bundle) { //получение параметров сохраненного экземпляра
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE))
        } else {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            currentLocation = savedInstanceState.getParcelable(LOCATION_KEY)
        }
        setUpDefaultMapLocation()
        if (savedInstanceState.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(savedInstanceState)
        }
        if (savedInstanceState.keySet().contains(GEOLOC_API_KEY)) {
            googleGeocoderDataSource = GoogleGeocoderDataSource(NetworkClient(), AddressBuilder())
            googleGeocoderDataSource?.setApiKey(savedInstanceState.getString(GEOLOC_API_KEY, ""))
        }
        if (savedInstanceState.keySet().contains(PLACES_API_KEY)) {
            googlePlacesApiKey = savedInstanceState.getString(PLACES_API_KEY, "")
        }
        if (savedInstanceState.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = savedInstanceState.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE)) {
            searchZone = savedInstanceState.getString(SEARCH_ZONE)
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE_DEFAULT_LOCALE)) {
            isSearchZoneWithDefaultLocale = savedInstanceState.getBoolean(SEARCH_ZONE_DEFAULT_LOCALE, false)
        }
        if (savedInstanceState.keySet().contains(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = savedInstanceState.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (savedInstanceState.keySet().contains(POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(POIS_LIST)
        }
        if (savedInstanceState.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (savedInstanceState.keySet().contains(UNNAMED_ROAD_VISIBILITY)) {
            isUnnamedRoadVisible = savedInstanceState.getBoolean(UNNAMED_ROAD_VISIBILITY, true)
        }
        if (savedInstanceState.keySet().contains(MAP_STYLE)) {
            mapStyle = savedInstanceState.getInt(MAP_STYLE)
        }
        if (savedInstanceState.keySet().contains(WITH_LEGACY_LAYOUT)) {
            isLegacyLayoutEnabled = savedInstanceState.getBoolean(WITH_LEGACY_LAYOUT, false)
        }
    }

    private fun getTransitionBundleParams(transitionBundle: Bundle) { //получение параметров пакета переходов
        bundle.putBundle(TRANSITION_BUNDLE, transitionBundle)
        if (transitionBundle.keySet().contains(LATITUDE) && transitionBundle.keySet()
                .contains(LONGITUDE)) {
            setLocationFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(LAYOUTS_TO_HIDE)) {
            setLayoutVisibilityFromBundle(transitionBundle)
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE)) {
            searchZone = transitionBundle.getString(SEARCH_ZONE)
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE_DEFAULT_LOCALE)) {
            isSearchZoneWithDefaultLocale = transitionBundle.getBoolean(SEARCH_ZONE_DEFAULT_LOCALE, false)
        }
        if (transitionBundle.keySet().contains(BACK_PRESSED_RETURN_OK)) {
            shouldReturnOkOnBackPressed = transitionBundle.getBoolean(BACK_PRESSED_RETURN_OK)
        }
        if (transitionBundle.keySet().contains(ENABLE_SATELLITE_VIEW)) {
            enableSatelliteView = transitionBundle.getBoolean(ENABLE_SATELLITE_VIEW)
        }
        if (transitionBundle.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = transitionBundle.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST)
        }
        if (transitionBundle.keySet().contains(POIS_LIST)) {
            poisList = transitionBundle.getParcelableArrayList(POIS_LIST)
        }
        if (transitionBundle.keySet().contains(GEOLOC_API_KEY)) {

            googleGeocoderDataSource?.setApiKey(transitionBundle.getString(GEOLOC_API_KEY, ""))
        }
        if (transitionBundle.keySet().contains(PLACES_API_KEY)) {
            googlePlacesApiKey = transitionBundle.getString(PLACES_API_KEY, "")
        }
        if (transitionBundle.keySet().contains(ENABLE_GOOGLE_TIME_ZONE)) {
            isGoogleTimeZoneEnabled = transitionBundle.getBoolean(ENABLE_GOOGLE_TIME_ZONE, false)
        }
        if (transitionBundle.keySet().contains(UNNAMED_ROAD_VISIBILITY)) {
            isUnnamedRoadVisible = transitionBundle.getBoolean(UNNAMED_ROAD_VISIBILITY, true)
        }
        if (transitionBundle.keySet().contains(MAP_STYLE)) {
            mapStyle = transitionBundle.getInt(MAP_STYLE)
        }
        if (transitionBundle.keySet().contains(WITH_LEGACY_LAYOUT)) {
            isLegacyLayoutEnabled = transitionBundle.getBoolean(WITH_LEGACY_LAYOUT, false)
        }
    }

    private fun setLayoutVisibilityFromBundle(transitionBundle: Bundle) { //настройка видимости макета из пакета
        val options = transitionBundle.getString(LAYOUTS_TO_HIDE)
        if (options != null && options.contains(OPTIONS_HIDE_STREET)) {
            isStreetVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_CITY)) {
            isCityVisible = false
        }
        if (options != null && options.contains(OPTIONS_HIDE_ZIPCODE)) {
            isZipCodeVisible = false
        }
    }

    private fun setLocationFromBundle(transitionBundle: Bundle) { //установить местоположение из пакета
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.network_resource))
        }
        currentLocation?.latitude = transitionBundle.getDouble(LATITUDE)
        currentLocation?.longitude = transitionBundle.getDouble(LONGITUDE)
        setCurrentPositionLocation()
        isLocationInformedFromBundle = true
    }

    private fun setCoordinatesInfo(latLng: LatLng) { //установка информации о координатах
        this.latitude?.text = String.format("%s: %s", getString(R.string.latitude), latLng.latitude)
        this.longitude?.text = String.format("%s: %s", getString(R.string.longitude), latLng.longitude)
        showCoordinatesLayout()
    }

    private fun resetLocationAddress() { //сброс адреса местоположения
        street?.text = ""
        city?.text = ""
        zipCode?.text = ""
    }

    private fun setLocationInfo(address: Address) { //установка сведений о местоположении
        street?.let {
            val formattedAddress = getFormattedAddress(address)
            if (isUnnamedRoadVisible) {
                it.text = formattedAddress
            } else {
                it.text = removeUnnamedRoad(formattedAddress)
            }
        }
        city?.text = if (isStreetEqualsCity(address)) "" else address.locality
        zipCode?.text = address.postalCode
        showAddressLayout()
    }

    private fun getFormattedAddress(address: Address): String { //получение форматированного адреса
        return if (!address.thoroughfare.isNullOrEmpty() && !address.subThoroughfare.isNullOrEmpty()) {
            getString(R.string.formatted_address, address.thoroughfare, address.subThoroughfare)
        } else {
            if (address.subThoroughfare.isNullOrEmpty() && !address.thoroughfare.isNullOrEmpty()) {
                address.thoroughfare
            } else if (address.thoroughfare.isNullOrEmpty() && !address.subThoroughfare.isNullOrEmpty()) {
                address.subThoroughfare
            } else {
                address.getAddressLine(0)
            }
        }
    }

    private fun isStreetEqualsCity(address: Address): Boolean { //улица равна городу
        return address.getAddressLine(0) == address.locality
    }

    private fun setNewMapMarker(latLng: LatLng) { //установка нового маркера на карту
        if (map != null) {
            currentMarker?.remove()
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(defaultZoom.toFloat()).build()
            hasWiderZoom = false
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            currentMarker = addMarker(latLng)
        }
    }

    private fun retrieveLocationFrom(query: String) { //восстановление местоположения из строки
        if (searchZone != null && searchZone!!.isNotEmpty()) {
            retrieveLocationFromDefaultZone(query)
        }
        else if (isSearchZoneWithDefaultLocale) {
            retrieveLocationFromDefaultZone(query)
        } else {
            geocoderPresenter?.getFromLocationName(query)
        }
    }

    private fun retrieveLocationWithDebounceTimeFrom(query: String) { //получение местоположения с времени со строки
            geocoderPresenter?.getDebouncedFromLocationName(query, DEBOUNCE_TIME)
    }

    private fun retrieveLocationFromDefaultZone(query: String) { //получение местоположения из зоны по умолчанию
        geocoderPresenter?.getFromLocationName(query)
    }

    private fun updateLocationNameList(addresses: List<Address>) { //обновление списка имен местоположений
        locationNameList.clear()
        for (address in addresses) {
            if (address.featureName == null) {
                locationNameList.add(getString(R.string.unknown_location))
            } else {
                locationNameList.add(getFullAddressString(address))
            }
        }
    }

    private fun getFullAddressString(address: Address): String { //получение полной строки адреса
        var fullAddress = ""
        address.featureName?.let {
            fullAddress += it
        }
        if (address.subLocality != null && address.subLocality.isNotEmpty()) {
            fullAddress += ", " + address.subLocality
        }
        if (address.locality != null && address.locality.isNotEmpty()) {
            fullAddress += ", " + address.locality
        }
        if (address.countryName != null && address.countryName.isNotEmpty()) {
            fullAddress += ", " + address.countryName
        }
        return fullAddress
    }

    private fun setDefaultMapSettings() { //установка настройек карты по умолчанию
        map?.let {
            it.mapType = MAP_TYPE_NORMAL
            it.setOnMapLongClickListener(this)
            it.setOnMapClickListener(this)
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.isMyLocationButtonEnabled = true
            it.uiSettings.isMapToolbarEnabled = false
        }
    }

    private fun setUpDefaultMapLocation() { //Настройка местоположения на карте по умолчанию
        if (currentLocation != null) {
            setCurrentPositionLocation()
        } else {
            searchView = findViewById(R.id.search)
            retrieveLocationFrom(Locale.getDefault().displayCountry)
            hasWiderZoom = true
        }
    }

    private fun setCurrentPositionLocation() { //установка текущего местоположения
        currentLocation?.let {
            setNewMapMarker(LatLng(it.latitude, it.longitude))
            geocoderPresenter?.getInfoFromLocation(LatLng(it.latitude,
                it.longitude))
        }
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        val googleApiClientBuilder = GoogleApiClient.Builder(this).addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
        googleApiClient = googleApiClientBuilder.build()
        googleApiClient?.connect()
    }

    private fun addMarker(latLng: LatLng): Marker? { //добавление маркера
        map?.let {
            return it.addMarker(MarkerOptions().position(latLng).draggable(true))
        }
        return null
    }

    private fun setNewLocation(address: Address) { //установка нового местоположения
        this.selectedAddress = address
        if (currentLocation == null) {
            currentLocation = Location(getString(R.string.network_resource))
        }
        currentLocation?.latitude = address.latitude
        currentLocation?.longitude = address.longitude
        setNewMapMarker(LatLng(address.latitude, address.longitude))
        setLocationInfo(address)
        searchView?.setText("")
    }

    private fun fillLocationList(addresses: List<Address>) { //заполнение списка о местоположении
        locationList.clear()
        locationList.addAll(addresses)
    }

    private fun closeKeyboard() { //закрытие клавиатуры
        val view = this.currentFocus
        view?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun removeUnnamedRoad(str: String): String { //удаление безименной дороги
        return str.replace(UNNAMED_ROAD_WITH_COMMA, "")
            .replace(UNNAMED_ROAD_WITH_HYPHEN, "")
    }
}