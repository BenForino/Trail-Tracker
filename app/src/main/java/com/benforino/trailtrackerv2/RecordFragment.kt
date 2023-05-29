package com.benforino.trailtrackerv2

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.benforino.trailtrackerv2.databinding.FragmentRecordBinding
import com.benforino.trailtrackerv2.misc.Constants.ACTION_PAUSE_SERVICE
import com.benforino.trailtrackerv2.misc.Constants.ACTION_START_OR_RESUME_SERVICE
import com.benforino.trailtrackerv2.misc.Constants.ACTION_STOP_SERVICE
import com.benforino.trailtrackerv2.services.Polyline
import com.benforino.trailtrackerv2.services.TrackService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pixelcarrot.base64image.Base64Image
import java.util.Calendar
import java.util.UUID


class RecordFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var firebaseAuth: FirebaseAuth
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
        firebaseAuth = Firebase.auth;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.requireContext())
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
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
            val finalLat = waypoints.last().last().latitude
            val finalLon = waypoints.last().last().longitude
            for(waypoint in waypoints){
                distance += calcRideDistance(waypoint)
            }

            val timeStamp = Calendar.getInstance().timeInMillis
           saveToFirestore(distance, timeStamp,bmp,waypoints,finalLat,finalLon)
           // viewModel.insertTrail(trail)
            sendServiceCommand(ACTION_STOP_SERVICE)
        }

    }

    private fun saveToFirestore(distance:Float, timeStamp:Long, img:Bitmap? = null, wayPoint:MutableList<Polyline>, finalLat:Double,finalLon:Double){
        val user = FirebaseAuth.getInstance().currentUser
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.name_trail_layout,null)
        val editText = dialogLayout.findViewById<EditText>(R.id.trailName)
        with(builder){
            setTitle("Enter a name for this new trail")
            setPositiveButton("Save"){dialog, which ->
                var name = editText.text.toString()
                if(name.isEmpty()){
                    name = "unnamed_trail"
                }
                user?.let {
                    Base64Image.encode(img) { base64 ->
                        base64?.let {
                            val randomID = UUID.randomUUID().toString()
                            val trailMap = hashMapOf(
                                "name" to name,
                                "distance" to distance,
                                "timeStamp" to timeStamp,
                                "userID" to user.uid,
                                "finalLat" to finalLat,
                                "finalLon" to finalLon
                            )
                            val imgMap = hashMapOf(
                                "img" to it,
                                "id" to randomID
                            )

                            db.collection("Trails").document(randomID).set(trailMap)
                                .addOnSuccessListener {
                                    Log.d("firebase","Saved to firestore")
                                    db.collection("Trail_Images").document(randomID).set(imgMap)
                                }
                                .addOnFailureListener {
                                    Log.d("firebase", "Failed to save to firestore")
                                }
                        }
                    }
                }
            }
            setView(dialogLayout)
            show()
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
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token)
            .addOnSuccessListener {
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude,it.longitude),
                        15f
                    )
                )
            }

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