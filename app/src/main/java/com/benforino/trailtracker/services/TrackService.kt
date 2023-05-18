package com.benforino.trailtracker.services
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.benforino.trailtracker.MainActivity
import com.benforino.trailtracker.R
import com.benforino.trailtracker.misc.Constants.ACTION_PAUSE_SERVICE
import com.benforino.trailtracker.misc.Constants.ACTION_START_OR_RESUME_SERVICE
import com.benforino.trailtracker.misc.Constants.ACTION_STOP_SERVICE
import com.benforino.trailtracker.misc.Constants.NOTIFICATION_CHANNEL_ID
import com.benforino.trailtracker.misc.Constants.NOTIFICATION_CHANNEL_NAME
import com.benforino.trailtracker.misc.Constants.NOTIFICATION_ID
import com.benforino.trailtracker.misc.Constants.SHOW_RECORDING_FRAGMENT
import com.benforino.trailtracker.misc.Constants.locationFastestInterval
import com.benforino.trailtracker.misc.Constants.locationUpdateInterval
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

class TrackService: LifecycleService() {

    private var firstFun = true
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        postInitVals()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        tracking.observe(this, Observer {
            checkLocationTracking(it)

        })
    }

    companion object {
        val tracking = MutableLiveData<Boolean>()
        val waypoints = MutableLiveData<Polylines>()
    }

    private fun postInitVals(){
        tracking.postValue(false)
        waypoints.postValue(mutableListOf())
    }
    private fun polyLineInit() = waypoints.value?.apply {
        add(mutableListOf())
        waypoints.postValue(this)
    } ?: waypoints.postValue(mutableListOf(mutableListOf()))

    private fun addWaypoint(location: Location?){
        location?.let {
            val loc = LatLng(location.latitude, location.longitude)
            waypoints.value?.apply {
                last().add(loc)
                waypoints.postValue(this)
            }
        }
    }

    private val locCallback = object : LocationCallback() {
        override fun onLocationResult(output: LocationResult) {
            super.onLocationResult(output)

            if(tracking.value!!){
                output.locations.let {
                    waypoints -> for(waypoint in waypoints){
                        addWaypoint(waypoint)
                        Log.d("trackservice","New Location ${waypoint.latitude}, ${waypoint.longitude}")
                }
                }
            }
        }
    }

    private fun checkLocationTracking(tracking: Boolean){
        if(tracking){
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationUpdateInterval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(locationFastestInterval)
                .build()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("trackservice","Invalid Permissions")
                return
            }
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locCallback,
                Looper.getMainLooper()
            )
        }else{
            fusedLocationProviderClient.removeLocationUpdates(locCallback)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(firstFun){
                        createForegroundService()
                        firstFun = false
                    }else{
                        createForegroundService()
                        Log.d("trackservice","Resumed Service")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    pause()
                }
                ACTION_STOP_SERVICE -> {
                    Log.d("trackservice","Stopped Service")
                }

                else -> {
                    Log.d("trackservice","Not Defined")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun pause(){
        tracking.postValue(false)
    }

    private fun getPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also{
            it.action = SHOW_RECORDING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
    )
    private fun createForegroundService() {
        polyLineInit()
        tracking.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        startNotificationService(notificationManager)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.radio_button_checked)
            .setContentTitle("Trail Tracker")
            .setContentText("00:00:00")
            .setContentIntent(getPendingIntent())
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }
    private fun startNotificationService(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
