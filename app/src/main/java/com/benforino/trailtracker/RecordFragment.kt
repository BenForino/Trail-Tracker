package com.benforino.trailtracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.benforino.trailtracker.databinding.FragmentRecordBinding
import com.benforino.trailtracker.misc.Constants.ACTION_PAUSE_SERVICE
import com.benforino.trailtracker.misc.Constants.ACTION_START_OR_RESUME_SERVICE
import com.benforino.trailtracker.misc.Constants.ACTION_STOP_SERVICE
import com.benforino.trailtracker.services.Polyline
import com.benforino.trailtracker.services.TrackService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class RecordFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var _binding: FragmentRecordBinding? = null
    private var map: GoogleMap? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var tracking = false
    private var waypoints = mutableListOf<Polyline>()

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

    private fun updateCamLocation(){
        if(waypoints.isNotEmpty() && waypoints.last().isNotEmpty()){
            Log.d("trackservice","${waypoints.last().last()}")
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    waypoints.last().last(),
                    15f
                )
            )
        }
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
            finishRecording()
        }

        startObservers()
    }

    private fun finishRecording() {
        sendServiceCommand(ACTION_STOP_SERVICE)
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