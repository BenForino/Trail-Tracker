package com.benforino.trailtrackerv2

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.benforino.trailtrackerv2.databinding.FragmentRecordBinding
import com.benforino.trailtrackerv2.misc.Constants.ACTION_PAUSE_SERVICE
import com.benforino.trailtrackerv2.misc.Constants.ACTION_START_OR_RESUME_SERVICE
import com.benforino.trailtrackerv2.misc.Constants.ACTION_STOP_SERVICE
import com.benforino.trailtrackerv2.services.Polyline
import com.benforino.trailtrackerv2.services.TrackService
import com.benforino.trailtrackerv2.viewmodel.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class RecordFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val viewModel: ViewModel by viewModels()
    private var _binding: FragmentRecordBinding? = null
    private var map: GoogleMap? = null
    val fireStoreDatabase = FirebaseFirestore.getInstance()
    private val binding get() = _binding!!
    private var tracking = false
    private var waypoints = mutableListOf<Polyline>()
    private var db=Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun appendLatestWaypoint() {
        if(waypoints.isNotEmpty()){
            if(waypoints.last().size >= 2){
                val penultimateWaypoint = waypoints.last()[waypoints.last().size - 2]
                val finalWaypoint = waypoints.last().last()
                val options = PolylineOptions()
                    .color(Color.BLUE)
                    .width(6f)
                    .add(penultimateWaypoint)
                    .add(finalWaypoint)
                map?.addPolyline(options)
            }
        }
    }

    private fun polyLineSetup(){
        for(pl in waypoints){
            val options = PolylineOptions()
                .color(Color.BLUE)
                .width(9f)
                .addAll(pl)
            map?.addPolyline(options)
        }
    }

    private fun updateCamLocation(finalZoom: Boolean = false){
        if(!finalZoom) {
            if (waypoints.isNotEmpty() && waypoints.last().isNotEmpty()) {
                Log.d("trackservice", "${waypoints.last().last()}")
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        waypoints.last().last(),
                        15f
                    )
                )
            }
        }else{
            val mapBounds = LatLngBounds.builder()
            for(waypoint in waypoints){
                for(point in waypoint){
                    mapBounds.include(point)
                }
            }
            map?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    mapBounds.build(),
                    binding.mapView.width,
                    binding.mapView.height,
                    (binding.mapView.height * 0.05f).toInt()
                )
            )
        }
    }
    private fun finishRide(){
        map?.snapshot { bmp ->
            var distance = 0f
            for(waypoint in waypoints){
                distance += calcRideDistance(waypoint)
            }
            val timeStamp = Calendar.getInstance().timeInMillis
           saveToFirestore(distance, timeStamp,bmp,waypoints)
           // viewModel.insertTrail(trail)
        }
        sendServiceCommand(ACTION_STOP_SERVICE)
    }

    private fun saveToFirestore(distance:Float, timeStamp:Long, img:Bitmap? = null, wayPoint:MutableList<Polyline>){
        val trailMap = hashMapOf(
            "distance" to distance,
            "timeStamp" to timeStamp,
        )
        db.collection("Trails").document("Trail").set(trailMap)
            .addOnSuccessListener {
                Log.d("firebase","Saved to firestore")
            }
            .addOnFailureListener {
                Log.d("firebase", "Failed to save to firestore")
            }

    }

    private fun calcRideDistance(polyline: Polyline):Float{
        var totalDistance = 0f
        for(i in 0..polyline.size -2 ){
            val p1 = polyline[i]
            val p2 = polyline[i+1]
            val result = FloatArray(1)
            Location.distanceBetween(
                p1.latitude,
                p1.longitude,
                p2.latitude,
                p2.longitude,
                result
            )
            totalDistance += result[0]
        }
        return totalDistance
    }

    private fun trackingWatch(tracking: Boolean){
        this.tracking = tracking
        if(!tracking){
            binding.startRecording.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.record_button_teal)
            binding.stopRecording.visibility = View.INVISIBLE
        }else{
            binding.startRecording.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.record_button_red)
            binding.stopRecording.visibility = View.VISIBLE
        }
    }
    private fun toggleTracking(){
        if(tracking) {
            sendServiceCommand(ACTION_PAUSE_SERVICE)
        }else{
            sendServiceCommand(ACTION_START_OR_RESUME_SERVICE)
        }
    }
    private fun startObservers() {
        TrackService.tracking.observe(viewLifecycleOwner, Observer {
            trackingWatch(it)
        })

        TrackService.waypoints.observe(viewLifecycleOwner, Observer {
            waypoints = it
            appendLatestWaypoint()
            updateCamLocation()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startRecording.setOnClickListener {
        }
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync {
            map = it;
            polyLineSetup()
        }
        binding.startRecording.setOnClickListener {
            toggleTracking()
        }
        binding.stopRecording.setOnClickListener{
            finishRide()
        }

        startObservers()
    }


    private fun sendServiceCommand(action: String) =
        Intent(requireContext(), TrackService::class.java).also{
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }


}