package com.google.firebase.codelab.nthuchat

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker




class AddActivityMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_map)


        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapAddActivity) as SupportMapFragment
        mapFragment.getMapAsync(this)

        title = "24.794657, 120.993240"

//        supportActionBar!!.setDisplayHomeAsUpEnabled(true)//back btn

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.getItemId()) {
            R.id.doneMap -> {
                val intent = intent
                val bundle = Bundle()
                bundle.putString("lat", title.toString())
                intent.putExtras(bundle)
                setResult(Activity.RESULT_OK, intent)
                finish()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val nthu = LatLng(24.794657, 120.993240)
        mMap.addMarker(MarkerOptions().position(nthu).title(getString(R.string.drag)).draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_2)))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(nthu))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(nthu, 16.5F))



        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                Log.e("mapDrag", "DragStart : " + marker.position)
                val lat = String.format("%.5f", marker.position.latitude)
                val long = String.format("%.5f", marker.position.longitude)
                title = "$lat, $long"
            }

            override fun onMarkerDrag(marker: Marker) {
                Log.e("mapDrag", "Drag : " + marker.position)
                val lat = String.format("%.5f", marker.position.latitude)
                val long = String.format("%.5f", marker.position.longitude)
                title = "$lat, $long"
            }

            override fun onMarkerDragEnd(marker: Marker) {
                Log.e("mapDrag", "DragEnd : " + marker.position)
                val lat = String.format("%.5f", marker.position.latitude)
                val long = String.format("%.5f", marker.position.longitude)
                title = "$lat, $long"
            }
        })

    }
}
