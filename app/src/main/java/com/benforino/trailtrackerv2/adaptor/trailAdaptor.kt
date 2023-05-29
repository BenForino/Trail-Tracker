package com.benforino.trailtrackerv2.adaptor

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.benforino.trailtrackerv2.R
import com.benforino.trailtrackerv2.database.Trail
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pixelcarrot.base64image.Base64Image
class trailAdaptor(private val trailsList:ArrayList<Trail>):
    RecyclerView.Adapter<trailAdaptor.MyViewHolder>() {
    private var db= Firebase.firestore

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val trailImg: ShapeableImageView = itemView.findViewById(R.id.trailImg)
        val tvHeading: TextView = itemView.findViewById(R.id.distanceText)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_trail,
        parent,false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return trailsList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = trailsList[position]
        fetchDecodeImages(currentItem.id,holder,position)

        holder.tvHeading.text = currentItem.name
    }
    private fun fetchDecodeImages(imgID: String, holder: MyViewHolder, position: Int){
        val images = db.collection("Trail_Images")
        images.document(imgID)
            .get()
            .addOnSuccessListener {
                val imgStr = it.get("img").toString()
                if(imgStr.isNotEmpty()){
                    Base64Image.decode(imgStr) {it1 ->
                        if (it1 != null) {
                            holder.trailImg.setImageBitmap(it1)
                        }
                    }
                }
            }
    }
}