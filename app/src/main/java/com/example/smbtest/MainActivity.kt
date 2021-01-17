package com.example.smbtest

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var collection: CollectionReference
    private lateinit var adapter: RecyclerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var collectionPath: String
    private lateinit var locationManager: LocationManager
    private lateinit var criteria: Criteria
    private lateinit var ll: LocationListener
    private lateinit var loc: Location
    private lateinit var geoClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        criteria = Criteria()
        criteria.isAltitudeRequired = false
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.powerRequirement = Criteria.NO_REQUIREMENT

        val notificationChannel =
            NotificationChannel("GEO", "GEOFENCE", NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.description = "Channel description"

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannel(notificationChannel)

        val provider = locationManager.getBestProvider(criteria, false)
        geoClient = LocationServices.getGeofencingClient(this)
        var loc = Location(provider)

        ll = LocationListener { location -> loc = location }

        val extras = intent.extras
        val user: String
        if (extras != null) {
            user = extras.getString("user_mail").toString()
            collectionPath = "Users/$user/Stores"
        } else {
            collectionPath = "Stores"
        }

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()
        collection = db.collection(collectionPath)
        val query = collection.orderBy("name")
        val options =
            FirestoreRecyclerOptions.Builder<Store>().setQuery(query, Store::class.java).build()

        adapter = RecyclerAdapter(db, collectionPath, options)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.add_item).setOnClickListener { view ->
            addStore()
            Snackbar.make(view, "Store being added...", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_map -> {
                val inMainAct = Intent(this, MapActivity::class.java)
                val myList = ArrayList<Store>()

                collection.get().addOnSuccessListener { result ->
                    for (document in result) {
                        myList.add(
                            Store(
                                document.data.get("name") as String?,
                                document.data.get("latitude") as Double,
                                document.data.get("longitude") as Double,
                                document.data.get("description") as String,
                                (document.data.get("radius") as Number).toFloat()
                            )
                        )
                        inMainAct.putParcelableArrayListExtra("storeList", myList)
                        Log.d(TAG, "${document.id} => ${document.data}")
                    }
                    startActivity(inMainAct)
                }.addOnFailureListener { exception ->
                    Log.d(TAG, "Error getting documents: ", exception)
                }

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addStore() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Store")
        val view: View =
            LayoutInflater.from(this@MainActivity).inflate(R.layout.store_dialog, null, false)
        builder.setView(view)
        val enterStoreName: EditText = view.findViewById(R.id.enterStoreName)
        val enterStoreDescription: EditText = view.findViewById(R.id.enterStoreDescription)
        val enterStoreRadius: EditText = view.findViewById(R.id.enterStoreRadius)
        builder.setPositiveButton("Add") { _, _ ->
            val name: String = enterStoreName.text.toString().trim()
            val description: String = enterStoreDescription.text.toString().trim()
            val radius: Float? = enterStoreRadius.text.toString().toFloatOrNull()
            val location = getCurrentLocation()
            Log.d(TAG, location?.latitude.toString())
            val latitude = location?.latitude
            val longitude = location?.longitude
            if (name.isNotEmpty() && description.isNotEmpty() && radius != null && radius > 0 && location != null) {
                val storeDataMap = hashMapOf(
                    "name" to name,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "description" to description,
                    "radius" to radius
                )
                db.collection(collectionPath)
                    .document(name)
                    .set(storeDataMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "Document added with ID: $name")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
                adapter.notifyDataSetChanged()
                createGeofence(name, latitude!!, longitude!!, radius)


            } else {
                Toast.makeText(this@MainActivity, "Invalid data!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun createGeofence(id: String, latitude: Double, longitude: Double, radius: Float) {
        val latLng = LatLng(latitude, longitude)
        val geo = Geofence.Builder().setRequestId(id)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setExpirationDuration(3600)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        geoClient.addGeofences(getGeofenceRequest(geo), getGeofencePendingIntent())
    }

    private fun getGeofenceRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence)
            .build()
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(): Location? {
        checkPermissions()
        val provider = locationManager.getBestProvider(criteria, false)
        locationManager.requestLocationUpdates(provider.toString(), 10000L, 1F, ll)
        return locationManager.getLastKnownLocation(provider.toString())
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val requestCode = 1

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(perms, requestCode)
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    companion object {
        const val TAG = "MAIN_ACTIVITY"
    }
}