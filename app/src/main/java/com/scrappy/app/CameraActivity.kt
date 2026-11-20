package com.scrappy.app

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.scrappy.app.data.AiIdentificationCodec
import com.scrappy.app.data.RequestCancellation
import java.util.concurrent.Executors
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import java.io.File

class CameraActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var reviewImage: ImageView
    private lateinit var captureButton: Button
    private lateinit var retryCameraButton: Button
    private lateinit var retakeButton: Button
    private lateinit var identifyButton: Button
    private lateinit var statusView: TextView
    private lateinit var aiButton: Button
    private lateinit var cancelAnalysisButton: Button
    private val worker=Executors.newSingleThreadExecutor()
    private var generation=0
    private var analysis: String?=null
    private var busy=false
    private var imageCapture: ImageCapture? = null
    private var cameraPreview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraStarting=false
    private var lastPhoto: File? = null
    private var consentDialog: AlertDialog? = null
    private var activeRequest: RequestCancellation? = null

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else {
            status("Camera permission was not granted. Tap Retry camera to request it again, allow Camera in Android Settings, or use the identification helper.")
            captureButton.isEnabled = false
            retryCameraButton.visibility = View.VISIBLE
            identifyButton.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20)}
        root.addView(TextView(this).apply{text="Metal Photo";textSize=25f})
        previewView=PreviewView(this); root.addView(previewView, LinearLayout.LayoutParams(-1,0,1f))
        reviewImage=ImageView(this).apply{visibility=View.GONE;adjustViewBounds=true;scaleType=ImageView.ScaleType.CENTER_INSIDE};root.addView(reviewImage,LinearLayout.LayoutParams(-1,0,1f))
        statusView=TextView(this).apply{text="Fill the frame with one clean metal item in bright, even light. Avoid hands, faces, license plates, and unrelated objects.";setPadding(0,12,0,12)};root.addView(ScrollView(this).apply{addView(statusView)},LinearLayout.LayoutParams(-1,(160*resources.displayMetrics.density).toInt()))
        captureButton=Button(this).apply{text="Capture photo";setOnClickListener{capture()}};root.addView(captureButton)
        retryCameraButton=Button(this).apply{text="Retry camera";visibility=View.GONE;setOnClickListener{ensureCameraPermission()}};root.addView(retryCameraButton)
        retakeButton=Button(this).apply{text="Retake";visibility=View.GONE;setOnClickListener{showCamera()}};root.addView(retakeButton)
        identifyButton=Button(this).apply{text="Use identification helper";visibility=View.GONE;setOnClickListener{startActivity(Intent(this@CameraActivity,MainActivity::class.java).putExtra("open_identifier",true));finish()}};root.addView(identifyButton)
        aiButton=Button(this).apply{text="Analyze with AI";visibility=View.GONE;setOnClickListener{requestAnalysis()}};root.addView(aiButton)
        cancelAnalysisButton=Button(this).apply{text="Cancel analysis";visibility=View.GONE;setOnClickListener{cancelAnalysis()}};root.addView(cancelAnalysisButton)
        root.addView(Button(this).apply{text="Back";setOnClickListener{finish()}})
        setContentView(root)
        applyScreenInsets(root)
        cacheDir.listFiles()?.filter { it.name.startsWith("scrappy_") && it.name.endsWith(".jpg") && System.currentTimeMillis()-it.lastModified()>86_400_000L }?.forEach { it.delete() }
        analysis=savedInstanceState?.getString("analysis")
        val name=savedInstanceState?.getString("photo")
        val restored=name?.takeIf { it.startsWith("scrappy_") && it.endsWith(".jpg") && File(it).name == it }?.let { File(cacheDir,it) }?.takeIf { it.isFile }
        if(restored != null) { lastPhoto=restored;showReview(restored);return }
        ensureCameraPermission()
    }

    private fun ensureCameraPermission() {
        captureButton.isEnabled=false
        retryCameraButton.visibility=View.GONE
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCamera() else permission.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        // Settings grants do not invoke the permission launcher's callback.
        // Recover the preview when returning, without prompting again or
        // replacing a photo already being reviewed.
        if(lastPhoto!=null)return
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else if(imageCapture!=null) {
            releaseCamera()
            captureButton.isEnabled=false
            retryCameraButton.visibility=View.VISIBLE
            identifyButton.visibility=View.VISIBLE
            status("Camera permission is required to capture a photo. Use the identification helper or allow Camera in Android Settings.")
        }
    }

    private fun status(s:String){statusView.text=s}
    private fun openExternal(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            status("No browser is available to open the privacy policy. Keep the photo local or use the manual helper.")
        }
    }
    private fun startCamera(){
        // onResume and the permission callback may both request startup.
        if(cameraStarting || imageCapture!=null || lastPhoto!=null || isDestroyed || isFinishing)return
        captureButton.isEnabled=false
        retryCameraButton.visibility=View.GONE
        // Provider creation itself can throw before a future/listener exists.
        // Keep the manual helper available instead of crashing the screen.
        val future=try { ProcessCameraProvider.getInstance(this) } catch (_: Exception) {
            cameraStartupFailed()
            return
        }
        cameraStarting=true
        future.addListener({
            cameraStarting=false
            if(isDestroyed || isFinishing || lastPhoto != null)return@addListener
            // Provider initialization can finish after the user leaves the screen
            // or changes permission in Settings. onResume retries when visible.
            if(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
                captureButton.isEnabled=false
                return@addListener
            }
            try {
                val provider=future.get()
                cameraProvider=provider
                val preview=Preview.Builder().build().also{it.setSurfaceProvider(previewView.surfaceProvider)}
                val capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                val selector=when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> error("No camera is available on this device")
                }
                releaseCamera()
                cameraPreview=preview;imageCapture=capture
                provider.bindToLifecycle(this,selector,preview,capture)
                captureButton.isEnabled=true
                retryCameraButton.visibility=View.GONE
                identifyButton.visibility=View.GONE
                status("Fill the frame with one clean metal item in bright, even light. Avoid hands, faces, license plates, and unrelated objects.")
            } catch (_: Exception) { cameraStartupFailed() }
        },ContextCompat.getMainExecutor(this))
    }

    private fun cameraStartupFailed() {
        cameraStarting=false
        releaseCamera()
        captureButton.isEnabled=false
        retryCameraButton.visibility=View.VISIBLE
        identifyButton.visibility=View.VISIBLE
        status("Camera could not start. Tap Retry camera or use the identification helper.")
    }

    private fun capture(){
        val cap=imageCapture ?: return status("Camera is not ready yet.")
        captureButton.isEnabled=false
        val file=File(cacheDir,"scrappy_${System.currentTimeMillis()}.jpg")
        try {
            // Read rotation at capture time, including a half-turn without recreation.
            previewView.display?.let { cap.targetRotation=it.rotation }
            cap.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(this),object:ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults){
                if(isDestroyed || isFinishing) { file.delete();return }
                analysis=null;lastPhoto?.takeIf{it.exists()}?.delete();lastPhoto=file;showReview(file)
            }
            override fun onError(exception: androidx.camera.core.ImageCaptureException){
                file.delete()
                if(isDestroyed || isFinishing)return
                captureButton.isEnabled=imageCapture!=null
                retryCameraButton.visibility=if(imageCapture==null)View.VISIBLE else View.GONE
                identifyButton.visibility=View.VISIBLE
                status("Capture failed. Try again or use the identification helper.")
            }
            })
        } catch (_: Exception) {
            file.delete()
            if(!isDestroyed && !isFinishing) {
                captureButton.isEnabled=imageCapture!=null
                retryCameraButton.visibility=if(imageCapture==null)View.VISIBLE else View.GONE
                identifyButton.visibility=View.VISIBLE
                status("Capture could not start. Try again or use the identification helper.")
            }
        }
    }

    private fun showReview(file:File){
        // Release the camera while the captured photo is being reviewed or analyzed.
        releaseCamera()
        previewView.visibility=View.GONE;reviewImage.visibility=View.VISIBLE;reviewImage.setImageDrawable(null)
        captureButton.visibility=View.GONE;retakeButton.visibility=View.VISIBLE;identifyButton.visibility=View.VISIBLE
        retryCameraButton.visibility=View.GONE
        aiButton.visibility=View.VISIBLE;aiButton.isEnabled=!busy
        status(analysis ?: "Photo captured locally in temporary app cache. You can use the manual helper or choose AI analysis. No photo is sent without your permission.")
        worker.execute {
            val result=runCatching { AiPhotoClient.reviewBitmap(file) }
            runOnUiThread {
                if(isDestroyed || isFinishing || lastPhoto!=file) {
                    result.getOrNull()?.recycle()
                } else {
                    result.onSuccess { reviewImage.setImageBitmap(it) }
                        .onFailure { if(!busy)status("Photo preview could not be loaded. Retake the photo or use the identification helper.") }
                }
            }
        }
    }

    private fun showCamera(){
        consentDialog?.dismiss();consentDialog=null
        activeRequest?.cancel();activeRequest=null
        cancelAnalysisButton.visibility=View.GONE;retakeButton.isEnabled=true
        generation++;analysis=null;busy=false;aiButton.visibility=View.GONE;lastPhoto?.takeIf{it.exists()}?.delete();lastPhoto=null;reviewImage.setImageDrawable(null);reviewImage.visibility=View.GONE;previewView.visibility=View.VISIBLE
        captureButton.visibility=View.VISIBLE;captureButton.isEnabled=true;retakeButton.visibility=View.GONE;identifyButton.visibility=View.GONE
        status("Ready for another photo.");ensureCameraPermission()
    }

    private fun requestAnalysis() {
        if(busy || consentDialog!=null)return
        val configured=runCatching {
            AiIdentificationCodec.endpoint(BuildConfig.AI_ENDPOINT)
            val policy=java.net.URI(BuildConfig.AI_PRIVACY_URL)
            require(policy.scheme=="https" && policy.host!=null && policy.userInfo==null)
        }.isSuccess
        if(!configured) { status("AI service is not configured in this test build. Use the identification helper. Photos stay on this device.");return }
        val photo=lastPhoto ?: return
        consentDialog=AlertDialog.Builder(this).setTitle("Send this photo for AI analysis?")
            .setMessage("A resized copy of this photo will be sent to Scrappy's service and Google Gemini to suggest a metal group. The copy excludes photo metadata; anything visible in the picture remains. Scrappy's supplied server does not save photos. Google handles data under the service account's terms. AI can be wrong and cannot verify alloy, grade or value. Avoid faces, plates and private information.")
            .setNeutralButton("Privacy policy") { _,_ -> openExternal(Intent(Intent.ACTION_VIEW,Uri.parse(BuildConfig.AI_PRIVACY_URL))) }
            .setNegativeButton("Keep photo local",null)
            .setPositiveButton("Send and analyze") { _,_ ->
                if(isDestroyed || isFinishing || busy || lastPhoto!=photo || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))return@setPositiveButton
                busy=true;aiButton.isEnabled=false;retakeButton.isEnabled=false;status("Analyzing your photo…")
                cancelAnalysisButton.visibility=View.VISIBLE
                val request=++generation
                val cancellation=RequestCancellation()
                activeRequest=cancellation
                worker.execute {
                    val result=runCatching {
                        cancellation.check()
                        val jpeg=AiPhotoClient.jpeg(photo,cancellation)
                        cancellation.check()
                        AiPhotoClient.identify(jpeg,BuildConfig.AI_ENDPOINT,cancellation).report()
                    }
                    runOnUiThread {
                        if(!isDestroyed && !isFinishing && generation==request) {
                            activeRequest=null
                            busy=false;aiButton.isEnabled=true;retakeButton.isEnabled=true
                            cancelAnalysisButton.visibility=View.GONE
                            result.onSuccess { analysis=it;status(it) }.onFailure { status(it.message ?: "AI analysis failed. Use the manual helper.") }
                        }
                    }
                }
            }.create().also { dialog ->
                dialog.setOnDismissListener { if(consentDialog===dialog)consentDialog=null }
                dialog.show()
            }
    }

    private fun cancelAnalysis() {
        if(!busy)return
        // Invalidate the worker callback before disconnecting. A late result
        // must never replace this state or a later request's result.
        generation++;analysis=null
        activeRequest?.cancel();activeRequest=null
        busy=false;aiButton.isEnabled=true;retakeButton.isEnabled=true
        cancelAnalysisButton.visibility=View.GONE
        status("Analysis canceled. You can retry, retake, or use the identification helper. A photo already sent may still be processed by the service.")
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("photo",lastPhoto?.name);outState.putString("analysis",analysis)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() {
        // Returning from another app requires a fresh, visible upload decision.
        consentDialog?.dismiss();consentDialog=null
        super.onStop()
    }
    private fun releaseCamera() {
        // ProcessCameraProvider is shared: only release this screen's use cases.
        val owned=listOfNotNull<UseCase>(cameraPreview,imageCapture).toTypedArray()
        if(owned.isNotEmpty())cameraProvider?.unbind(*owned)
        cameraPreview=null;imageCapture=null
    }
    override fun onDestroy(){
        consentDialog?.dismiss();consentDialog=null
        releaseCamera()
        activeRequest?.cancel();activeRequest=null
        generation++;worker.shutdownNow()
        if(!isChangingConfigurations)lastPhoto?.takeIf{it.exists()}?.delete()
        super.onDestroy()
    }
}
