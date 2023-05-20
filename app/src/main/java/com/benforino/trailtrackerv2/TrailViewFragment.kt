package com.benforino.trailtrackerv2

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benforino.trailtrackerv2.adaptor.trailAdaptor
import com.benforino.trailtrackerv2.database.Trail
import com.benforino.trailtrackerv2.databinding.FragmentRecordBinding
import com.benforino.trailtrackerv2.databinding.FragmentTrailViewBinding
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pixelcarrot.base64image.Base64Image


class TrailViewFragment : Fragment() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var db= Firebase.firestore
    private var _binding: FragmentTrailViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var trailRecycler: RecyclerView;
    private var trailArray = ArrayList<Trail>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        getLatestImage(LatLng(51.7296836,-2.2036068))

        _binding = FragmentTrailViewBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun getLatestImage(position: LatLng){
        val trails = db.collection("Trails")
        val latLongBounds = calculateLatLngBounds(position)
        trails.whereLessThanOrEqualTo("finalLat", latLongBounds.second.latitude)
            .whereGreaterThanOrEqualTo("finalLat",latLongBounds.first.latitude)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if ( document.get("finalLon").toString().toDouble() >= latLongBounds.first.longitude
                        && document.get("finalLon").toString().toDouble() <= latLongBounds.second.longitude){
                        Log.d("Testing", "DocumentSnapshot data: ${document.data}")
                    }
                }
                getImages(documents)
            }
            .addOnFailureListener { exception ->
                Log.w("Testing", "Error getting documents: ", exception)
            }
    }
    private fun getImages(documents:QuerySnapshot){
        var trailArray = arrayListOf<Trail>()
        var idList = mutableListOf<String>()
        trailRecycler = binding.trailRecycler
        trailRecycler.layoutManager = LinearLayoutManager(requireContext())
        for (document in documents) {
            idList.add(document.id)
        }
        val images = db.collection("Trail_Images")
        images.whereIn("id",idList)
            .get()
            .addOnSuccessListener { documents1 ->
                for (document1 in documents1) {
                    val imgStr = document1.get("img").toString()
                    for(document in documents){
                        if(document.id == document1.id){
                            val trail = Trail(document1.id,document.get("distance").toString().toFloat(),imgStr)
                            trailArray.add(trail)
                            break
                        }
                    }

                }
                trailRecycler.adapter = trailAdaptor(trailArray)


            }
            .addOnFailureListener { exception ->
            Log.w("Testing", "Error getting documents: ", exception)
        }

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