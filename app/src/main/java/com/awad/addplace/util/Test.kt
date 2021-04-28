package com.awad.addplace.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import javax.inject.Inject

private const val TAG = "Test myTag"

class Test : SaveReference {

    @Inject
    lateinit var fireStore: FirebaseFirestore
    private fun addData() {
        val docData = hashMapOf(
            "stringExample" to "Hello world!",
            "booleanExample" to true,
            "numberExample" to 3.14159265,
            "dateExample" to Timestamp(Date()),
            "listExample" to arrayListOf(1, 2, 3),
            "nullExample" to null,
            "Ref" to fireStore


        )

        val nestedData = hashMapOf(
            "a" to 5,
            "b" to true
        )

        docData["objectExample"] = nestedData


        fireStore.collection("data")
            .add(nestedData)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully written!")
                saveRef(it, it.id)
                Log.d(TAG, "success: id =  ${it.id}")

            }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

    }

    override fun saveRef(ref: DocumentReference, id: String) {
        Log.d(TAG, "saveRef: id =  $id")
        val nestedData = hashMapOf(
            "reg" to ref,

            )
        fireStore.collection("new created").document(id).set(nestedData)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    var newRef = fireStore.collection("new created").document(id)
                    getRefData(newRef)
                    Log.d(TAG, "saveRef: success")
                } else
                    Log.e(TAG, "saveRef: Error", it.exception)
            }
    }

    override fun getRefData(newRef: DocumentReference) {
        newRef.get().addOnCompleteListener {
            if (it.isSuccessful) {
                val ref: DocumentReference = it.result?.get("reg") as DocumentReference
                readLastRef(ref)
            }

        }
    }

    override fun readLastRef(last: DocumentReference) {
        last.get().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "readLastRef: ${it.result?.get("a")}")
                Log.d(TAG, "readLastRef: ${it.result?.get("b")}")
            }
        }
    }
}

interface SaveReference {
    fun saveRef(ref: DocumentReference, id: String)
    fun getRefData(newRef: DocumentReference)
    fun readLastRef(last: DocumentReference)
}

