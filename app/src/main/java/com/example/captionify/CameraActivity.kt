package com.example.captionify

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.checkSelfPermission
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
import java.io.IOException
import java.util.*

class CameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textureView: TextureView
    private var myCameraCaptureSession: CameraCaptureSession?=null
    private lateinit var stringCameraID: String
    private lateinit var cameraManager: CameraManager
    private var myCameraDevice: CameraDevice?=null
    private var imageReader: ImageReader? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder


    private lateinit var speechRecognizer: SpeechRecognizer

    private val REQUEST_IMAGE_CAPTURE = 1
    var storageRef:StorageReference?=null
    private var imageUri: Uri? = null
    private var mTts: TextToSpeech? = null
    var dbRef: DatabaseReference?=null
    private val TAG = "TextToSpeechDemo"

    private var myStr:String="";

    override fun onInit(status: Int) {
        // Your onInit code here
        if (status == TextToSpeech.SUCCESS) {
            val result = mTts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {

            }
        } else {
            Log.e(TAG, "Initialization failed")
        }
    }
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        speechRecognizer.startListening(intent)
        Toast.makeText(baseContext,"NOW LISTENING",Toast.LENGTH_SHORT).show()

        Log.d("SPEECH","NOW LISTENING")
    }

    private fun processSpeech(command: String) {
        Log.d("SPEECH",command)
        if (command.contains("capture", ignoreCase = true)) {
            // Perform action to capture image here
            Toast.makeText(this, "Image captured!", Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.mytext).visibility=View.GONE
            findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.VISIBLE
            buttonStopCamera()
            // Add your image capture logic here
        } else {
            Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show()
        }
    }



    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), PackageManager.PERMISSION_GRANTED)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        startCamera()

        storageRef= FirebaseStorage.getInstance().reference.child("Frames")
        dbRef= FirebaseDatabase.getInstance().reference

        findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.GONE

        textureView.setOnClickListener{
            buttonStartCamera()
        }
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





        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                CameraActivity.RECORD_AUDIO_PERMISSION_CODE
            )
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {

                Log.d("SPEECH","Ready")
            }

            override fun onBeginningOfSpeech() {

                Log.d("SPEECH","beginning")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d("SPEECH",rmsdB.toString())
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("SPEECH","buffer Recieved")
            }

            override fun onEndOfSpeech() {
                Log.d("SPEECH","End of speech")
            }

            override fun onError(error: Int) {
                Log.d("SPEECH",error.toString())
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { processSpeech(it) }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d("SPEECH",partialResults.toString())
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startListening()




    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            myCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            myCameraDevice?.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            myCameraDevice?.close()
            myCameraDevice = null
        }
    }

    private fun startCamera() {
        try {
            stringCameraID = cameraManager.cameraIdList[0]
            if (checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(stringCameraID, stateCallback, null)

        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
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

    fun buttonStartCamera() {
        val surfaceTexture = textureView.surfaceTexture
        val surface = Surface(surfaceTexture)
        try {
            captureRequestBuilder = myCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            val outputConfiguration = OutputConfiguration(surface)

            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(outputConfiguration),
                mainExecutor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = cameraCaptureSession
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO)
                        try {
                            myCameraCaptureSession!!.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                null
                            )
                        } catch (e: CameraAccessException) {
                            throw RuntimeException(e)
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        myCameraCaptureSession = null
                    }
                }
            )

            myCameraDevice!!.createCaptureSession(sessionConfiguration)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    fun buttonStopCamera() {
        try {
            myCameraCaptureSession!!.abortCaptures()
//            val imageBitmap = data?.extras?.get("data") as Bitmap?
            val bitmap = textureView.bitmap
            if (bitmap != null) {
                // Display the image bitmap in the ImageView
                uploadBitmapToFirebaseStorage(bitmap)
               //got bitmap


                dbRef!!.child("captions").addValueEventListener(object : ValueEventListener {
                    @SuppressLint("ResourceType")
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // This method is called once with the initial value and again
                        // whenever data at this location is updated.
                        // You can get the data from the dataSnapshot
                        if(dataSnapshot.hasChildren()){
                            for (children in dataSnapshot.children){
                                val stringValue = children.getValue(String::class.java)

                                if(myStr!=stringValue){
                                    val textview=findViewById<TextView>(R.id.mytext)

                                    textview.text=stringValue!!
                                    // Load the typing animation
//                                        val animatorSet = AnimatorInflater.loadAnimator(baseContext, R.anim.fade_in) as AnimatorSet
//
//                                        // Set the target TextView
//                                        animatorSet.setTarget(textview)
//
//                                        // Start the animation
//                                        animatorSet.start()
                                    textview.visibility=View.VISIBLE
                                    speak(stringValue!!)
                                    myStr=stringValue!!
                                    findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.GONE
                                    startListening()
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
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }

    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        findViewById<CircularProgressIndicator>(R.id.loader).visibility= View.VISIBLE
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//            imageUri = data?.data
//            // Do something with the image URI
//            // Assuming imageView is your ImageView
//            if (imageUri != null) {
//                Picasso.get().load(imageUri).into(findViewById<ImageView>(R.id.image))
//            } else {
//                // If it's not a file URI, try to retrieve the image from the intent's extras
//                val imageBitmap = data?.extras?.get("data") as Bitmap?
//                if (imageBitmap != null) {
//                    // Display the image bitmap in the ImageView
//                    findViewById<ImageView>(R.id.image).setImageBitmap(imageBitmap)
//                    uploadBitmapToFirebaseStorage(imageBitmap)
//
//                    dbRef!!.child("captions").addValueEventListener(object : ValueEventListener {
//                        @SuppressLint("ResourceType")
//                        override fun onDataChange(dataSnapshot: DataSnapshot) {
//                            // This method is called once with the initial value and again
//                            // whenever data at this location is updated.
//                            // You can get the data from the dataSnapshot
//                            if(dataSnapshot.hasChildren()){
//                                for (children in dataSnapshot.children){
//                                    val stringValue = children.getValue(String::class.java)
//
//                                    if(myStr!=stringValue){
//                                        val textview=findViewById<TextView>(R.id.mytext)
//
//                                        textview.text=stringValue!!
//                                        // Load the typing animation
////                                        val animatorSet = AnimatorInflater.loadAnimator(baseContext, R.anim.fade_in) as AnimatorSet
////
////                                        // Set the target TextView
////                                        animatorSet.setTarget(textview)
////
////                                        // Start the animation
////                                        animatorSet.start()
//                                        textview.visibility=View.VISIBLE
//                                        speak(stringValue!!)
//                                        myStr=stringValue!!
//                                        findViewById<CircularProgressIndicator>(R.id.loader).visibility=View.GONE
//                                    }
//                                }
//                            }
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//                            // Failed to read value
//                            println("Error retrieving string value: ${error.message}")
//                            findViewById<TextView>(R.id.mytext).text="Something went Wrong"
//                            Log.w(TAG, "Failed to read value.", error.toException())
//                        }
//                    })
//
//
//                }
//            }
//        }
//    }


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
        speechRecognizer.destroy()
    }
    // Don't forget to release the ImageReader when it's no longer needed

}


