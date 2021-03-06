package com.awad.addplace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awad.addplace.databinding.ActivityMainBinding
import com.awad.addplace.util.LocationModel
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryBounds
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import javax.inject.Inject


private const val TAG = "MainActivity myTag"
const val RESULT_IMAGE = 1

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), UploadCallbacks {


    private var count = 0


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
    private var info: HashMap<String, *>? = null
    private var locationHashMap: HashMap<String, Any>? = null

    var name: String? = null
    var description: String? = null
    var location: String? = null
    var address: String? = null
    var phone: String? = null
    var city: String? = null
    var type: String? = null
    var website: String? = null

    private var data: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    private fun queryLocations() {
        val center = GeoLocation(31.2593, 34.2695)
        val radius = 50 * 10000
        val bounds: List<GeoQueryBounds> = GeoFireUtils.getGeoHashQueryBounds(
            center,
            radius.toDouble()
        )
        Log.d(TAG, "queryLocations: bounds size ${bounds.size}")
        val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
        for (b in bounds) {

            val q = fireStore.collectionGroup("meta data")
//
//                .whereEqualTo("city", city)
                .whereEqualTo("type", type)

                .orderBy("geohash")
                .startAt(b.startHash)
                .endAt(b.endHash)


            tasks.add(q.get())
        }
        Log.d(TAG, "queryLocations: ${tasks.size}")
        // Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<LocationModel> = ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap.documents) {
                        val location = doc.toObject(LocationModel::class.java)
                        val lat = doc.get("lat")!! as Double
                        val lng = doc.get("lng")!! as Double

                        // We have to filter out a few false positives due to GeoHash
                        // accuracy, but most will match
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                        if (distanceInM <= radius) {
                            matchingDocs.add(location!!)
                        }
                    }
                }

                // matchingDocs contains the results
                // ...
                Log.d(TAG, "queryLocations: size ${matchingDocs.size}")
                Log.d(TAG, "queryLocations: size $matchingDocs")
                val intent = Intent(this, MapsActivity::class.java)
                intent.putParcelableArrayListExtra("locations", ArrayList(matchingDocs))
                startActivity(intent)

            }

    }

    private fun testQueries() {

        val q = fireStore.collectionGroup("meta data")
//
//                .whereEqualTo("city", city)
            .whereEqualTo("type", type)
//

        q.get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "testQueries: size = ${it.result.documents.size}")
                    for (d in it.result.documents) {
                        Log.d(TAG, "testQueries ::: type= ${d["type"]} ::: city= ${d["city"]}")
                    }
                } else
                    Log.e(TAG, "testQueries: Error", it.exception)
            }


    }

    //  save images to meta data
    private fun getDocumentRef() {
        fireStore.collection("cities").document(city!!).collection(type!!)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {

                    getMetaDataID(it.result.documents)
                } else
                    Log.e(TAG, "getDocumentRef: Error", it.exception)
            }


    }

    private fun getMetaDataID(documents: List<DocumentSnapshot>) {
        for (d in documents) {
            var imagesList = ArrayList<String>()
            if (d["images"] != null)
                imagesList = d["images"] as ArrayList<String>

            d.reference.collection("meta_data").get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {

                        val ref = it.result.documents[0].reference
                        uploadImagesListToMetaData(imagesList, ref)
                    } else
                        Log.e(TAG, "getMetaDataID: Error", it.exception)
                }
        }
    }

    private fun uploadImagesListToMetaData(
        imagesList: java.util.ArrayList<String>,
        ref: DocumentReference
    ) {

        ref.update("images", imagesList)
            .addOnCompleteListener {
                if (it.isSuccessful)
                    Toast.makeText(this, "uploaded", Toast.LENGTH_SHORT).show()
                else
                    Log.e(TAG, "uploadImagesListToMetaData: Error", it.exception)
            }
    }


    private fun getPlacesCount() {
        fireStore.collection("cities").document(city!!).collection(type!!)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful)
                    count = it.result.documents.size + 1
                getInputs()
            }
    }

    private fun saveLocation() {
        fireStore.collection("cities").document(city!!).collection(type!!)
            .add(info!!)
            .addOnFailureListener {
                Log.e(TAG, "saveLocation: Error", it)
            }
            .addOnCompleteListener {
                binding.progressBar.visibility = GONE
                if (it.isSuccessful)
                    uploadMetaData(it.result)
                else {
                    Log.e(TAG, "onCreate: Error", it.exception)
                }
            }
    }

    private fun getInputs() {
        Log.d(TAG, "getInputs: ")
        name = binding.details.nameEditText.text.toString()
        description = binding.details.descriptionEditText.text.toString()
        location = binding.details.locationEditText.text.toString()
        address = binding.details.addressEditText.text.toString()
        phone = binding.details.phoneEditText.text.toString()
        website = binding.details.websiteEditText.text.toString()
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
            "website" to website
        )
        images = ArrayList()
        info = hashMapOf(

            "place_number" to count,
            "main_info" to mainInfo,
            "service_options" to serviceOptions,
            "main_features" to mainFeatures,
            "accessibility" to accessibility,
            "eat_options" to eatOptions,
            "services" to services,
            "payment" to payment,
            "amenities" to amenities,
            "public" to thePublic,
            "atmosphere" to atmosphere,
            "planning" to planning,
            "health_and_safety" to healthAndSafety,
            "view_count" to 0,


            )

        saveLocation()
    }

    override fun saveLocation(ref: DocumentReference?) {
        val location = hashMapOf(
            "ref" to ref,
            "geoLocation" to locationHashMap

        )
        fireStore.collection("locations").add(location)
            .addOnCompleteListener {
                if (!it.isSuccessful)
                    Log.e(TAG, "saveLocation: Error", it.exception)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_IMAGE && resultCode == RESULT_OK && data != null) {
            this.data = data

        }
    }

    private fun uploadImage(imageUri: Uri, ref: DocumentReference?) {
        binding.progressCircular.visibility = VISIBLE
        val bmp = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos)


        val data: ByteArray = baos.toByteArray()
        val imageRef =
            storageRef.child(city!!).child(type!!).child(ref?.id!!).child(name!!)
                .child("images/${imageUri.lastPathSegment}")

        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads

        }.addOnSuccessListener {
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            // ...
        }.addOnProgressListener {
            val total = it.totalByteCount.toDouble()
            val transferred = it.bytesTransferred.toDouble()
            val progress = (transferred / total) * 100
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
                images?.add(downloadUri.toString())
                uploadImageUris(ref)


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


        ref?.update("images", images)
            ?.addOnCompleteListener {
                if (it.isSuccessful)
                    Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun uploadMetaData(ref: DocumentReference?) {
        val lat = location?.substring(0, location!!.indexOf(','))!!
        val lng = location?.substring(location!!.indexOf(',') + 1, location!!.length - 1)!!

        val hash =
            GeoFireUtils.getGeoHashForLocation(GeoLocation(lat.toDouble(), lng.toDouble()))
        val geoPoint = GeoPoint(lat.toDouble(), lng.toDouble())

        val metadata = hashMapOf(
            "city" to city,
            "name" to name,
            "address" to address,
            "type" to type,
            "geo_hash" to hash,
            "lat" to lat.toDouble(),
            "lng" to lng.toDouble(),
            "location" to geoPoint,
            "ref_id" to ref?.id

        )

        ref!!.collection("meta_data")
            .add(metadata)
            .addOnCompleteListener {
                binding.progressBar.visibility = GONE
                if (!it.isSuccessful)
                else {
                    uploadImage(ref)

                }

            }

    }

    private fun clearFields() {

        binding.details.addressEditText.setText("")
        binding.details.phoneEditText.setText("")
        binding.details.websiteEditText.setText("")
        binding.details.locationEditText.setText("")
        binding.details.nameEditText.setText("")
        binding.details.descriptionEditText.setText("")
    }

    override fun uploadImage(ref: DocumentReference?) {

        if (data?.clipData != null) {
            val totalItems = data?.clipData!!.itemCount
            (0 until totalItems).forEach { i ->
                val imageUri = data?.clipData!!.getItemAt(i).uri
                uploadImage(imageUri, ref)

            }

        } else if (data != null && data?.clipData == null) {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: ")
        return when (item.itemId) {
            R.id.save_menu_item -> {
                getPlacesCount()
                true
            }
            R.id.upload_image_menu_item -> {
                data = null
                images = null
                images = ArrayList()
                Log.d(TAG, "onCreate: ${images?.size}")
                checkForPermissions()
                val intent = Intent()
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_IMAGE)

                true
            }
            R.id.clear_inputs_menu_item -> {
                clearFields()
                true
            }

            else -> super.onOptionsItemSelected(item)

        }

    }
}


interface UploadCallbacks {
    fun uploadMetaData(ref: DocumentReference?)
    fun uploadImage(ref: DocumentReference?)
    fun uploadImageUris(ref: DocumentReference?)

    // tests for saving locations in separated node
    fun saveLocation(ref: DocumentReference?)
}