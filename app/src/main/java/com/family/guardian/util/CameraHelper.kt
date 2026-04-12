package com.family.guardian.util

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Minimal Camera2 helper for SOS photo capture.
 * Captures a single JPEG from either front or rear camera.
 * Blocking call -- must be invoked from a background thread.
 */
class CameraHelper(private val context: Context) {

    companion object {
        private const val TAG = "CameraHelper"
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private const val TIMEOUT_SEC = 8L
    }

    fun capturePhoto(front: Boolean): File? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val facing = if (front)
            CameraCharacteristics.LENS_FACING_FRONT
        else
            CameraCharacteristics.LENS_FACING_BACK

        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == facing
        } ?: run {
            Log.w(TAG, "No ${if (front) "front" else "rear"} camera found")
            return null
        }

        val thread = HandlerThread("CamCapture").apply { start() }
        val handler = Handler(thread.looper)

        val imageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.JPEG, 1)
        val latchOpen = CountDownLatch(1)
        val latchCapture = CountDownLatch(1)

        var cameraDevice: CameraDevice? = null
        var result: File? = null

        // Set up image callback
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val dir = File(context.cacheDir, "sos_photos")
                dir.mkdirs()
                val file = File(dir, "sos_${if (front) "front" else "rear"}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { it.write(bytes) }
                result = file
                Log.d(TAG, "Photo saved: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save photo: ${e.message}")
            } finally {
                image.close()
                latchCapture.countDown()
            }
        }, handler)

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    latchOpen.countDown()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    latchOpen.countDown()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera open error: $error")
                    camera.close()
                    latchOpen.countDown()
                }
            }, handler)

            if (!latchOpen.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                Log.e(TAG, "Camera open timed out")
                thread.quitSafely()
                return null
            }

            val cam = cameraDevice ?: run {
                thread.quitSafely()
                return null
            }

            val latchSession = CountDownLatch(1)
            var captureSession: CameraCaptureSession? = null

            @Suppress("DEPRECATION")
            cam.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        latchSession.countDown()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Session config failed")
                        latchSession.countDown()
                    }
                },
                handler
            )

            if (!latchSession.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                Log.e(TAG, "Session config timed out")
                cam.close()
                thread.quitSafely()
                return null
            }

            val session = captureSession ?: run {
                cam.close()
                thread.quitSafely()
                return null
            }

            val captureRequest = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }.build()

            session.capture(captureRequest, null, handler)

            latchCapture.await(TIMEOUT_SEC, TimeUnit.SECONDS)

            session.close()
            cam.close()

        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Camera error: ${e.message}")
        } finally {
            imageReader.close()
            thread.quitSafely()
        }

        return result
    }
}
