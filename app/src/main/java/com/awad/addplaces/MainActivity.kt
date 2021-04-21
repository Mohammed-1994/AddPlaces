package com.awad.addplaces

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awad.addplaces.databinding.ActivityMainBinding
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


private const val TAG = "MainActivity myTag"
const val RESULT_IMAGE = 1

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), UploadCallbacks {


    @Inject
    lateinit var fireStore: FirebaseFirestore

    var storageRef = FirebaseStorage.getInstance().reference

    private lateinit var binding: ActivityMainBinding
    private var serviceOptions = ArrayList<String>()
    private var healthAndSafety = ArrayList<String>()
    private var mainFeatures = ArrayList<String>()
    private var accessibility = ArrayList<String>()
    private var eatOptions = ArrayList<String>()
    private var services = ArrayList<String>()
    private var payment = ArrayList<String>()
    private var amenities = ArrayList<String>()
    private var thePublic = ArrayList<String>()
    private var atmosphere = ArrayList<String>()
    private var planning = ArrayList<String>()
    private var images: ArrayList<String>? = null

    var name: String? = null
    var description: String? = null
    var location: String? = null
    var address: String? = null
    var phone: String? = null
    var city: String? = null
    var type: String? = null

    private var data: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveButton.setOnClickListener {
            getInputs()
            val lat = location?.substring(0, location!!.indexOf(','))!!
            val lon = location?.substring(location!!.indexOf(',') + 1, location!!.length - 1)!!

            val geoPoint = GeoPoint(lat.toDouble(), lon.toDouble())

            startActivity(intent)

            binding.progressBar.visibility = VISIBLE

            val mainInfo = hashMapOf(
                "name" to name,
                "description" to description,
                "location" to geoPoint,
                "address" to address,
                "phone" to phone,
                "city" to city,
                "type" to type,
            )
            val info = hashMapOf(

                "main info" to mainInfo,
                "service options" to serviceOptions,
                "main features" to mainFeatures,
                "accessibility" to accessibility,
                "eat options" to eatOptions,
                "services" to services,
                "payment" to payment,
                "amenities" to amenities,
                "public" to thePublic,
                "atmosphere" to atmosphere,
                "planning" to planning,
                "healthAndSafety" to healthAndSafety

            )

            fireStore.collection("cities").document(city!!).collection(type!!)
                .add(info)
                .addOnCompleteListener {
                    if (it.isSuccessful)
                        uploadMetaData(it.result)
                    else
                        Log.e(TAG, "onCreate: Error", it.exception)
                }

        }

        binding.uploadImage.setOnClickListener {
            data = null
            images = ArrayList()
            Log.d(TAG, "onCreate: ${images?.size}")
            checkForPermissions()
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_IMAGE)

        }


    }


    private fun getInputs() {
        name = binding.details.nameEditText.text.toString()
        description = binding.details.descriptionEditText.text.toString()
        location = binding.details.locationEditText.text.toString()
        address = binding.details.addressEditText.text.toString()
        phone = binding.details.phoneEditText.text.toString()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: ${images?.size}")
        Log.d(TAG, "onActivityResult: ")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "onActivityResult: RESULT_OK")
            this.data = data

        }
    }

    private fun uploadImage(imageUri: Uri, ref: DocumentReference?) {
        binding.progressCircular.visibility = VISIBLE
        Log.d(TAG, "uploadImage: ${images?.size}")
        val imageRef =
            storageRef.child(city!!).child(type!!).child(ref?.id!!)
                .child("images/${imageUri.lastPathSegment}")

        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Log.e(TAG, "onActivityResult: Error", it)

        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
            Log.d(TAG, "onActivityResult: success")
        }.addOnProgressListener {
            val total = it.totalByteCount.toDouble()
            val transferred = it.bytesTransferred.toDouble()
            val progress = (transferred / total) * 100
            Log.d(
                TAG,
                "uploadImage: transferred $transferred, total = $total, progress = $progress"
            )



            binding.progressCircular.progress = (progress.toInt())

        }
        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            binding.progressCircular.visibility = GONE

            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d(TAG, "onActivityResult: uri = $downloadUri")
                Log.d(TAG, "uploadImage: got uri :  ${images?.size}")

                images?.add(downloadUri.toString())
                uploadImageUris(ref)


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

    override fun uploadImageUris(ref: DocumentReference?) {
//        val images = hashMapOf(
//            "images" to images
//        )

        Log.d(TAG, "uploadImageUris:  ${images?.size} ")
        ref?.update("images", images)
            ?.addOnCompleteListener {
                if (!it.isSuccessful)
                    Log.e(TAG, "uploadImageUris: Error", it.exception)
            }
    }

    override fun uploadMetaData(ref: DocumentReference?) {

        val metadata = hashMapOf(
            "city" to city,
            "type" to type
        )

        ref!!.collection("meta data")
            .add(metadata)
            .addOnCompleteListener {
                binding.progressBar.visibility = GONE
                if (!it.isSuccessful)
                    Log.e(TAG, "reaRef: Error", it.exception)
                else
                    uploadImage(ref)
            }
    }

    override fun uploadImage(ref: DocumentReference?) {

        if (data?.clipData != null) {
            val totalItems = data?.clipData!!.itemCount
            Log.d(TAG, "onActivityResult: clip data != null,  size = $totalItems")
            (0 until totalItems).forEach { i ->
                Log.d(TAG, "onActivityResult: foreach")
                val imageUri = data?.clipData!!.getItemAt(i).uri
                uploadImage(imageUri, ref)

            }

        } else if (data != null && data?.clipData == null) {
            Log.d(TAG, "onActivityResult: data.data != null")
            uploadImage(data?.data!!, ref)
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

    fun services(view: View) {
        view as CheckBox
        val text = view.text
        if (services.contains(text.toString()))
            services.remove(text.toString())
        else
            services.add(text.toString())
    }

    fun accessibilityClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (accessibility.contains(text.toString()))
            accessibility.remove(text.toString())
        else
            accessibility.add(text.toString())
    }

    fun eatOptionsClicked(view: View) {
        view as CheckBox
        val text = view.text

        if (eatOptions.contains(text.toString()))
            eatOptions.remove(text.toString())
        else
            eatOptions.add(text.toString())


    }


    fun amenitiesClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (amenities.contains(text.toString()))
            amenities.remove(text.toString())
        else
            amenities.add(text.toString())
    }

    fun atmosphereClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (atmosphere.contains(text.toString()))
            atmosphere.remove(text.toString())
        else
            atmosphere.add(text.toString())
    }

    fun thePublicClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (thePublic.contains(text.toString()))
            thePublic.remove(text.toString())
        else
            thePublic.add(text.toString())
    }

    fun planningClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (planning.contains(text.toString()))
            planning.remove(text.toString())
        else
            planning.add(text.toString())
    }

    fun paymentClicked(view: View) {
        view as CheckBox
        val text = view.text
        if (payment.contains(text.toString()))
            payment.remove(text.toString())
        else
            payment.add(text.toString())
    }

    fun onCitiesRadioClicked(view: View) {
        view as RadioButton
        city = view.text.toString()
    }

    fun onTypesRadioClicked(view: View) {
        view as RadioButton
        type = view.text.toString()
    }
}


interface UploadCallbacks {
    fun uploadMetaData(ref: DocumentReference?)
    fun uploadImage(ref: DocumentReference?)
    fun uploadImageUris(ref: DocumentReference?)
}