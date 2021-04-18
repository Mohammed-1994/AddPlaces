package com.awad.addplaces

import android.Manifest
import android.R.attr
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awad.addplaces.databinding.ActivityMainBinding
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.net.URI
import javax.inject.Inject


private const val TAG = "MainActivity myTag"
const val RESULT_IMAGE = 1

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ReadSavedRef {


    @Inject
    lateinit var fireStore: FirebaseFirestore

    var storageRef = FirebaseStorage.getInstance().reference

    private lateinit var binding: ActivityMainBinding
    private var serviceOptions = ArrayList<String>()
    private var healthAndSafety = ArrayList<String>()
    private var mainFeatures = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {

            val info = hashMapOf(
                "serviceOptions" to serviceOptions,
                "healthAndSafety" to healthAndSafety
            )

            fireStore.collection("cities").document("Rafah").collection("res")
                .add(info)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        reaRef(it.result)
                    else
                        Log.e(TAG, "onCreate: Error", it.exception)
                }

        }

        binding.uploadImage.setOnClickListener {
            checkForPermissions()
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_IMAGE)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: ")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "onActivityResult: RESULT_OK")
            if (data.clipData != null) {
                val totalItems = data.clipData!!.itemCount
                Log.d(TAG, "onActivityResult: clip data != null,  size = $totalItems")
                (0 until totalItems).forEach { i ->
                    Log.d(TAG, "onActivityResult: foreach")
                    val imageUri = data.clipData!!.getItemAt(i).uri
//                    uploadImage(imageUri)

                }

            }
            if (data.data != null && data.clipData == null) {
                Log.d(TAG, "onActivityResult: data.data != null")
                uploadImage(data.data!!)
            }
        }
    }

    private fun uploadImage(imageUri: Uri) {

        val riversRef = storageRef.child("images/${imageUri.lastPathSegment}")

        val uploadTask = riversRef.putFile(imageUri)

        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Log.e(TAG, "onActivityResult: Error", it)

        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
            Log.d(TAG, "onActivityResult: success")
        }
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            riversRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d(TAG, "onActivityResult: uri = $downloadUri")
            } else {
                // Handle failures
                // ...
            }
        }
    }

    fun healthAndSafetyClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (healthAndSafety.contains(text.toString()))
            healthAndSafety.remove(text.toString())
        else
            healthAndSafety.add(text.toString())
    }

    fun serviceOptionsClicked(view: View) {

        view as CheckBox
        val text = view.text
        if (serviceOptions.contains(text.toString()))
            serviceOptions.remove(text.toString())
        else
            serviceOptions.add(text.toString())

    }

    fun mainFeatures(view: View) {

        view as CheckBox
        val text = view.text
        if (mainFeatures.contains(text.toString()))
            mainFeatures.remove(text.toString())
        else
            mainFeatures.add(text.toString())

    }

    override fun reaRef(ref: DocumentReference?) {
        Log.d(TAG, "reaRef: ${ref?.id}")
        ref?.get()?.addOnCompleteListener {
            if (it.isSuccessful) {
                var serviceOptions = it.result?.get("serviceOptions") as ArrayList<*>
                Log.d(TAG, "reaRef: size = ${serviceOptions.size}")
                Log.d(TAG, "reaRef: $serviceOptions")
            } else
                Log.e(TAG, "reaRef: Error", it.exception)
        }
    }

    private fun checkForPermissions() {
        val permissions =
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        for (s in permissions) {
            this.requestPermissions(s)
        }
    }

    private fun requestPermissions(permission: String) {
        if (
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )    // You can use the API that requires the permission.

        else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.

            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(permission), 1
            )
        }
    }
}


interface ReadSavedRef {
    fun reaRef(ref: DocumentReference?)
}