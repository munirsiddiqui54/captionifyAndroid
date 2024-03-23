package com.example.captionify

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
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
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.io.IOException
import java.util.*

class CameraActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private var myCameraCaptureSession: CameraCaptureSession?=null
    private lateinit var stringCameraID: String
    private lateinit var cameraManager: CameraManager
    private var myCameraDevice: CameraDevice?=null
    private var imageReader: ImageReader? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), PackageManager.PERMISSION_GRANTED)
        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        startCamera()
        findViewById<Button>(R.id.startbtn).setOnClickListener{
            buttonStartCamera()
        }
        findViewById<Button>(R.id.endbtn).setOnClickListener{
            buttonStopCamera()
        }
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
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

//    private fun captureImage() {
//        try {
//            // Create an ImageReader to capture the image
//            imageReader = ImageReader.newInstance(
//                textureView.width, textureView.height, ImageFormat.JPEG, 1
//            )
//
//            // Configure the capture request
//            val captureBuilder = myCameraDevice!!.createCaptureRequest(
//                CameraDevice.TEMPLATE_STILL_CAPTURE
//            ).apply {
//                addTarget(imageReader!!.surface)
//                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
//            }
//
//            // Capture the image
//            myCameraCaptureSession!!.capture(
//                captureBuilder.build(),
//                object : CameraCaptureSession.CaptureCallback() {
//                    override fun onCaptureCompleted(
//                        session: CameraCaptureSession,
//                        request: CaptureRequest,
//                        result: TotalCaptureResult
//                    ) {
//                        // Image captured successfully
//                        // You can process the captured image here if needed
//                        // For example, save it to storage
//                        // Afterwards, you might want to restart the preview
//                        startPreview()
//                        Toast.makeText(baseContext,"SUCCESS",Toast.LENGTH_LONG).show()
//                        Log.d("TAGG",imageReader.toString())
//                    }
//                },
//                null
//            )
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
//    }

    private fun startPreview() {
        // Resume the preview session
        // Add code to resume preview similar to buttonStartCamera function
    }

    // Don't forget to release the ImageReader when it's no longer needed
    override fun onPause() {
        super.onPause()
        imageReader?.close()
    }
}


