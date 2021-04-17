package com.awad.addplaces

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private const val TAG = "MainActivity myTag"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    @Inject
    lateinit var firestore: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        firestore.collection("data").document("one")
            .update("stringExample", "hi world")
            .addOnCompleteListener { snapshot ->
                Log.d(TAG, "onCreate: completed")
                if (snapshot.isSuccessful)
                    Log.d(TAG, "onCreate: success ")
                else
                    Log.d(TAG, "onCreate: Error ", snapshot.exception)
            }

    }
}