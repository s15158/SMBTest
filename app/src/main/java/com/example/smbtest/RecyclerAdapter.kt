package com.example.smbtest

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import checkData
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class RecyclerAdapter(
    private val database: FirebaseFirestore,
    private val collectionPath: String,
    options: FirestoreRecyclerOptions<Store>
) :
    FirestoreRecyclerAdapter<Store, RecyclerAdapter.ProductHolder>(options) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductHolder {
        val inflatedView = parent.inflate(R.layout.recyclerview_product_row, false)
        return ProductHolder(database, collectionPath, inflatedView, this)
    }

    class ProductHolder(
        private val db: FirebaseFirestore,
        private val cp: String,
        v: View,
        private val adapter: RecyclerAdapter,
    ) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var mNameView: TextView? = null
        private var mDescriptionView: TextView? = null
        private var mRadiusView: TextView? = null
        private var mLatitudeView: TextView? = null
        private var mLongitudeView: TextView? = null
        private var store: Store? = null

        init {
            mNameView = itemView.findViewById(R.id.storeName)
            mDescriptionView = itemView.findViewById(R.id.storeDescription)
            mRadiusView = itemView.findViewById(R.id.storeRadius)
            mLatitudeView = itemView.findViewById(R.id.storeLatitude)
            mLongitudeView = itemView.findViewById(R.id.storeLongitude)
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val popupMenu = PopupMenu(v.context, v)
            popupMenu.menuInflater.inflate(R.menu.pop_up_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.item_update -> {
                        val builder = AlertDialog.Builder(v.context)
                        val view: View = LayoutInflater.from(v.context)
                            .inflate(R.layout.store_dialog, null, false)
                        builder.setTitle("Edit Item")
                        val editStoreName: EditText = view.findViewById(R.id.enterStoreName)
                        val editStoreDescription: EditText = view.findViewById(R.id.enterStoreDescription)
                        val editStoreRadius: EditText = view.findViewById(R.id.enterStoreRadius)
                        editStoreName.setText(mNameView?.text)
                        editStoreDescription.setText(mDescriptionView?.text)
                        editStoreRadius.setText(mRadiusView?.text)

                        builder.setView(view)
                        builder.setPositiveButton(
                            "Edit"
                        ) { _, _ ->
                            if (checkData(
                                    editStoreName.text.toString(),
                                    editStoreDescription.text.toString(),
                                    editStoreRadius.text.toString().toIntOrNull()
                                ) && !getStore(editStoreName.text.toString()).isSuccessful
                            ) {
                                delStore(mNameView?.text as String)
                                db.collection(cp)
                                    .document(editStoreName.text.toString())
                                    .set(
                                        hashMapOf(
                                            "name" to editStoreName.text.toString(),
                                            "description" to editStoreDescription.text.toString(),
                                            "radius" to editStoreRadius.text.toString().toInt()
                                        )
                                    )
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Document successfully edited.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error editing document", e)
                                    }
                                adapter.notifyDataSetChanged()
                                Toast.makeText(
                                    v.context,
                                    "Store edited",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    v.context,
                                    "Invalid data",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        builder.setNegativeButton(
                            "Cancel"
                        ) { dialog, _ -> dialog.dismiss() }
                        builder.show()
                    }
                    R.id.item_del -> {
                        db.collection(cp)
                            .document(mNameView?.text as String)
                            .delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "Document successfully deleted.")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error deleting document.", e)
                            }
                        adapter.notifyDataSetChanged()
                        Toast.makeText(v.context, "Store deleted", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                true
            }

            popupMenu.show()
        }

        private fun getStore(name: String): Task<DocumentSnapshot> {
            return db.collection(cp).document(name).get()
        }

        private fun delStore(name: String) {
            db.collection(cp)
                .document(name)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Document successfully deleted.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error deleting document.", e)
                }
        }

        fun bindProduct(store: Store) {
            this.store = store
            mNameView?.text = store.name
            mDescriptionView?.text = store.description
            mRadiusView?.text = store.radius.toString()
            mLatitudeView?.text = store.latitude.toString()
            mLongitudeView?.text = store.longitude.toString()
        }
    }

    override fun onBindViewHolder(holder: ProductHolder, position: Int, model: Store) {
        holder.bindProduct(model)
    }

    companion object {
        const val TAG = "RECYCLER"
    }
}