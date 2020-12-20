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
    options: FirestoreRecyclerOptions<Product>
) :
    FirestoreRecyclerAdapter<Product, RecyclerAdapter.ProductHolder>(options) {
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
        private var mPriceView: TextView? = null
        private var product: Product? = null

        init {
            mNameView = itemView.findViewById(R.id.productName)
            mPriceView = itemView.findViewById(R.id.productPrice)
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
                            .inflate(R.layout.product_dialog, null, false)
                        builder.setTitle("Edit Item")
                        val editProductName: EditText = view.findViewById(R.id.enterProductName)
                        val editProductPrice: EditText = view.findViewById(R.id.enterProductPrice)
                        editProductName.setText(mNameView?.text)
                        editProductPrice.setText(mPriceView?.text)

                        builder.setView(view)
                        builder.setPositiveButton(
                            "Edit"
                        ) { _, _ ->
                            if (checkData(
                                    editProductName.text.toString(),
                                    editProductPrice.text.toString().toFloatOrNull()
                                ) && getProduct(editProductName.text.toString()).isSuccessful
                            ) {
                                delProduct(mNameView?.text as String)
                                db.collection(cp)
                                    .document(editProductName.text.toString())
                                    .set(
                                        hashMapOf(
                                            "name" to editProductName.text.toString(),
                                            "price" to editProductPrice.text.toString().toFloat()
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
                                    "Product edited",
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
                        Toast.makeText(v.context, "Product deleted", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                true
            }

            popupMenu.show()
        }

        private fun getProduct(name: String): Task<DocumentSnapshot> {
            return db.collection(cp).document(name).get()
        }

        private fun delProduct(name: String) {
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

        fun bindProduct(product: Product) {
            this.product = product
            mNameView?.text = product.name
            mPriceView?.text = product.price.toString()
        }
    }

    override fun onBindViewHolder(holder: ProductHolder, position: Int, model: Product) {
        holder.bindProduct(model)
    }

    companion object {
        const val TAG = "RECYCLER"
    }
}