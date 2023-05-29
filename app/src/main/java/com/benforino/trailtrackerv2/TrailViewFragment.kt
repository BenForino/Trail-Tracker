package com.benforino.trailtrackerv2

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benforino.trailtrackerv2.adaptor.trailAdaptor
import com.benforino.trailtrackerv2.database.Trail
import com.benforino.trailtrackerv2.databinding.FragmentTrailViewBinding
import com.benforino.trailtrackerv2.services.TrackService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pixelcarrot.base64image.Base64Image
import java.io.IOException
import java.util.Locale
import java.util.UUID


class TrailViewFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var db= Firebase.firestore
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var _binding: FragmentTrailViewBinding? = null
    private lateinit var geocoder: Geocoder
    private val binding get() = _binding!!
    private lateinit var trailRecycler: RecyclerView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = Firebase.auth
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        geocoder = Geocoder(requireContext(), Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTrailViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.locationAuto.setOnClickListener{
            startRecyclerView()
        }
        binding.locationManual.setOnClickListener{
            getLatLngFromStr()
        }
    }
    private fun startRecyclerView(){
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
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,CancellationTokenSource().token)
            .addOnSuccessListener {
                getTrails(it)
            }
    }
    private fun getTrails(position: Location){
        val trailArray = arrayListOf<Trail>()
        val trails = db.collection("Trails")
        val latLongBounds = calculateLatLngBounds(position)
        trailRecycler = binding.trailRecycler
        trailRecycler.layoutManager = LinearLayoutManager(requireContext())
        trails.whereLessThanOrEqualTo("finalLat", latLongBounds.second.latitude)
            .whereGreaterThanOrEqualTo("finalLat",latLongBounds.first.latitude)
            .get()
            .addOnSuccessListener { documents ->
                if(!documents.isEmpty) {
                    for (document in documents) {
                        if (document.get("finalLon").toString()
                                .toDouble() >= latLongBounds.first.longitude
                            && document.get("finalLon").toString()
                                .toDouble() <= latLongBounds.second.longitude
                        ) {
                            var name = "unnamed_trail"
                            if (document.get("name") !== null) {
                                name = document.get("name")!!.toString()
                            }
                            trailArray.add(Trail(document.id,
                                document.get("distance").toString().toFloat(),
                                name))
                        }
                    }
                    trailRecycler.adapter = trailAdaptor(trailArray)
                }else{
                    Toast.makeText(requireContext(), "No Trails found in this location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Testing", "Error getting documents: ", exception)
            }
    }
    private fun getTrails(position: LatLng){
        val trailArray = arrayListOf<Trail>()
        val trails = db.collection("Trails")
        val latLongBounds = calculateLatLngBounds(position)
        trailRecycler = binding.trailRecycler
        trailRecycler.layoutManager = LinearLayoutManager(requireContext())
        trails.whereLessThanOrEqualTo("finalLat", latLongBounds.second.latitude)
            .whereGreaterThanOrEqualTo("finalLat",latLongBounds.first.latitude)
            .get()
            .addOnSuccessListener { documents ->
                if(!documents.isEmpty) {
                    for (document in documents) {
                        if (document.get("finalLon").toString()
                                .toDouble() >= latLongBounds.first.longitude
                            && document.get("finalLon").toString()
                                .toDouble() <= latLongBounds.second.longitude
                        ) {
                            var name = "unnamed_trail"
                            if (document.get("name") !== null) {
                                name = document.get("name")!!.toString()
                            }
                            trailArray.add(Trail(document.id,
                                document.get("distance").toString().toFloat(),
                                name))
                        }
                    }
                    trailRecycler.adapter = trailAdaptor(trailArray)
                }else{
                    Toast.makeText(requireContext(), "No Trails found in this location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Testing", "Error getting documents: ", exception)
            }
    }

    @SuppressLint("InflateParams")
    private fun getLatLngFromStr(){
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.enter_address_layout,null)
        val editText = dialogLayout.findViewById<EditText>(R.id.address)
        with(builder){
            setTitle("Enter address to find trails")
            setPositiveButton("Save"){dialog, which ->
                val address = editText.text.toString()
                if(address.isEmpty()){
                    Toast.makeText(requireContext(), "No input found, try again", Toast.LENGTH_SHORT).show()
                }else{
                    val loc = geocoder.getFromLocationName(address,1)
                    if (loc != null) {
                        if(loc.isNotEmpty()){
                            val coordinates = LatLng(loc.first().latitude,loc.first().longitude)
                            getTrails(coordinates)
                        }else{
                            Toast.makeText(requireContext(), "Address not found, try again", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            setView(dialogLayout)
            show()
        }

    }
    private fun calculateLatLngBounds(latLng: Location, distanceKM:Int = 10):Pair<LatLng,LatLng>{
        val latMin = latLng.latitude - 0.045
        val latMax = latLng.latitude + 0.045
        val longMin = latLng.longitude - (0.045 / Math.cos(latLng.latitude*Math.PI/180))
        val longMax = latLng.longitude + (0.045 / Math.cos(latLng.latitude*Math.PI/180))
        val latLonMin = LatLng(latMin,longMin)
        val latLonMax = LatLng(latMax,longMax)
        return Pair(latLonMin,latLonMax)
    }
    private fun calculateLatLngBounds(latLng: LatLng, distanceKM:Int = 10):Pair<LatLng,LatLng>{
        val latMin = latLng.latitude - 0.045
        val latMax = latLng.latitude + 0.045
        val longMin = latLng.longitude - (0.045 / Math.cos(latLng.latitude*Math.PI/180))
        val longMax = latLng.longitude + (0.045 / Math.cos(latLng.latitude*Math.PI/180))
        val latLonMin = LatLng(latMin,longMin)
        val latLonMax = LatLng(latMax,longMax)
        return Pair(latLonMin,latLonMax)
    }

}