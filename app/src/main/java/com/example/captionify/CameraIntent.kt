package com.example.captionify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class CameraIntent : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val REQUEST_IMAGE_CAPTURE = 1
    var storageRef:StorageReference?=null
    private var imageUri: Uri? = null
    private var mTts: TextToSpeech? = null
    var dbRef:DatabaseReference?=null
    private val TAG = "TextToSpeechDemo"

    private var myStr:String="";

    override fun onInit(status: Int) {
        // Your onInit code here
        if (status == TextToSpeech.SUCCESS) {
            val result = mTts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {

//implementation
            }
        } else {
            Log.e(TAG, "Initialization failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_intent)
        storageRef= FirebaseStorage.getInstance().reference.child("Frames")
        dbRef=FirebaseDatabase.getInstance().reference

        findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.GONE


        dbRef!!.child("captions").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                // You can get the data from the dataSnapshot
                if(dataSnapshot.hasChildren()){
                    for (children in dataSnapshot.children){
                        val stringValue = children.getValue(String::class.java)
                        myStr=stringValue!!
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                println("Error retrieving string value: ${error.message}")
                findViewById<TextView>(R.id.mytext).text="Something went Wrong"
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })


        // Add a listener to retrieve the string value

        mTts = TextToSpeech(this, this)



        findViewById<ImageView>(R.id.image).setOnClickListener{
            findViewById<TextView>(R.id.mytext).visibility=View.GONE
            findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.VISIBLE
            dispatchTakePictureIntent()
        }


    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }


    fun uploadBitmapToFirebaseStorage(bitmap: Bitmap) {

        speak("Image is processing")
        // Convert Bitmap to ByteArray
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream)
        val data = byteArrayOutputStream.toByteArray()

        // Get a reference to Firebase Storage
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        // Define the path to store the image
        val imagesRef: StorageReference = storageRef.child("MYFRAMES/${System.currentTimeMillis()}.jpg")

        // Upload the ByteArray to Firebase Storage
        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl

            // Ensure downloadUrl is not null before using it
            downloadUrl?.addOnSuccessListener { uri ->
                // Uri is the download URL
                val url = uri.toString()
                Log.d("URL", url)
                dbRef?.child("imgSource")?.setValue(url.toString())
                // Do something with the downloadUrl if needed (e.g., save it to a database)
            }?.addOnFailureListener {
                // Handle failure to retrieve the download URL
                it.printStackTrace()
            }

            // Do something with the downloadUrl if needed (e.g., save it to a database)
        }.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
            exception.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        findViewById<CircularProgressIndicator>(R.id.loader).visibility= View.VISIBLE
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            // Do something with the image URI
            // Assuming imageView is your ImageView
            if (imageUri != null) {
                Picasso.get().load(imageUri).into(findViewById<ImageView>(R.id.image))

            } else {
                // If it's not a file URI, try to retrieve the image from the intent's extras
                val imageBitmap = data?.extras?.get("data") as Bitmap?
                if (imageBitmap != null) {
                    // Display the image bitmap in the ImageView
                    findViewById<ImageView>(R.id.image).setImageBitmap(imageBitmap)
                    uploadBitmapToFirebaseStorage(imageBitmap)



                    dbRef!!.child("captions").addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.
                            // You can get the data from the dataSnapshot
                            if(dataSnapshot.hasChildren()){
                                for (children in dataSnapshot.children){
                                    val stringValue = children.getValue(String::class.java)

                                    if(myStr!=stringValue){
                                        findViewById<TextView>(R.id.mytext).text=stringValue!!
                                        findViewById<TextView>(R.id.mytext).visibility=View.VISIBLE
                                        speak(stringValue!!)
                                        myStr=stringValue!!
                                        findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.GONE
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Failed to read value
                            println("Error retrieving string value: ${error.message}")
                            findViewById<TextView>(R.id.mytext).text="Something went Wrong"
                            Log.w(TAG, "Failed to read value.", error.toException())
                        }
                    })


                }
            }
        }
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            mTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    override fun onDestroy() {
        if (mTts != null) {
            mTts!!.stop()
            mTts!!.shutdown()
        }
        super.onDestroy()
    }
}