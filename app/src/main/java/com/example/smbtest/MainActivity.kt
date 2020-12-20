package com.example.smbtest

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val extras = intent.extras
        val user: String
        if(extras != null) {
            user = extras.getString("user_mail").toString()
            collectionPath = "Users/$user/Products"
        } else {
            collectionPath = "Products"
        }

        auth = FirebaseAuth.getInstance()

        db = FirebaseFirestore.getInstance()
        collection = db.collection(collectionPath)
        val query = collection.orderBy("name")
        val options =
                FirestoreRecyclerOptions.Builder<Product>().setQuery(query, Product::class.java).build()

        adapter = RecyclerAdapter(db, collectionPath, options)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.add_item).setOnClickListener { view ->
            addProduct()
            Snackbar.make(view, "Product being added...", Snackbar.LENGTH_LONG)
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addProduct() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Product")
        val view: View =
                LayoutInflater.from(this@MainActivity).inflate(R.layout.product_dialog, null, false)
        builder.setView(view)
        val enterProductName: EditText = view.findViewById(R.id.enterProductName)
        val enterProductPrice: EditText = view.findViewById(R.id.enterProductPrice)
        builder.setPositiveButton("Add") { _, _ ->
            val name: String = enterProductName.text.toString().trim()
            val price: Float? = enterProductPrice.text.toString().toFloatOrNull()
            if (name.isNotEmpty() && price != null && price >= 0.0f) {
                val productDataMap = hashMapOf("name" to name, "price" to price)
                // Products/$user
                db.collection(collectionPath)
                        .document(name)
                        .set(productDataMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "Document added with ID: $name")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this@MainActivity, "Invalid data!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(
                "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        builder.show()
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