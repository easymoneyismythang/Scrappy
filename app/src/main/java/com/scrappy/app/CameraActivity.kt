package com.scrappy.app

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.scrappy.app.data.AiIdentificationCodec
import com.scrappy.app.data.GeorgiaPrices
import com.scrappy.app.data.PriceQuotes
import com.scrappy.app.data.Prefs
import com.scrappy.app.data.RealYards
import com.scrappy.app.data.RequestCancellation
import java.util.concurrent.Executors
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {
    private lateinit var cameraRoot: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var reviewImage: ImageView
    private lateinit var captureButton: Button
    private lateinit var retryCameraButton: Button
    private lateinit var retakeButton: Button
    private lateinit var identifyButton: Button
    private lateinit var statusView: TextView
    private lateinit var aiButton: Button
    private lateinit var cancelAnalysisButton: Button
    private var scanFrame: ScanFrameView? = null
    private var zoomRow: LinearLayout? = null
    private var zoomPills = mutableListOf<Button>()
    private val worker=Executors.newSingleThreadExecutor()
    private var generation=0
    private var analysis: String?=null
    private var busy=false
    private var imageCapture: ImageCapture? = null
    private var cameraPreview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
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
            zoomRow?.visibility = View.GONE
            scanFrame?.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraRoot = FrameLayout(this).apply { setBackgroundColor(ScrappyTheme.BG) }
        previewView = PreviewView(this)
        cameraRoot.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        reviewImage = ImageView(this).apply { visibility = View.GONE; adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_INSIDE }
        cameraRoot.addView(reviewImage, FrameLayout.LayoutParams(-1, -1))
        scanFrame = ScanFrameView(this)
        cameraRoot.addView(scanFrame, FrameLayout.LayoutParams(-1, -1))
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xCC0B1F14.toInt())
            setPadding(24, 18, 24, 18)
        }
        topBar.addView(TextView(this).apply {
            text = "SCRAPPY · Scan metal"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ScrappyTheme.WHITE)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        topBar.addView(ScrappyTheme.darkButton(this, "Back") { finish() }, LinearLayout.LayoutParams(-2, -2))
        cameraRoot.addView(topBar, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ScrappyTheme.roundedTop(this@CameraActivity, ScrappyTheme.CARD, 24f)
            setPadding(24, 20, 24, 24)
        }
        zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, 0, 0, 12)
        }
        sheet.addView(zoomRow)
        listOf("0.5" to 0.5f, "1×" to 1f, "2×" to 2f).forEach { (label, ratio) ->
            val pill = Button(this).apply {
                text = label; textSize = 15f; setAllCaps(false)
                setOnClickListener { setZoom(ratio, this) }
            }
            zoomPills.add(pill)
            zoomRow!!.addView(pill, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = 6; rightMargin = 6 })
        }
        styleZoomPills(null)
        statusView = TextView(this).apply {
            text = "Fill the frame with one clean metal item in bright, even light. Avoid hands, faces, license plates, and unrelated objects."
            textSize = 15f; setTextColor(ScrappyTheme.MUTED); setPadding(0, 4, 0, 12)
        }
        sheet.addView(ScrollView(this).apply { addView(statusView) }, LinearLayout.LayoutParams(-1, (120 * resources.displayMetrics.density).toInt()))
        captureButton = ScrappyTheme.mintButton(this, "Capture photo") { capture() }
        sheet.addView(captureButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        retryCameraButton = ScrappyTheme.darkButton(this, "Retry camera") { ensureCameraPermission() }.apply { visibility = View.GONE }
        sheet.addView(retryCameraButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        retakeButton = ScrappyTheme.darkButton(this, "Retake") { showCamera() }.apply { visibility = View.GONE }
        sheet.addView(retakeButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        identifyButton = ScrappyTheme.darkButton(this, "Use identification helper") {
            startActivity(Intent(this@CameraActivity, MainActivity::class.java).putExtra("open_identifier", true)); finish()
        }.apply { visibility = View.GONE }
        sheet.addView(identifyButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        aiButton = ScrappyTheme.mintButton(this, "Analyze with AI") { requestAnalysis() }.apply { visibility = View.GONE }
        sheet.addView(aiButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        cancelAnalysisButton = ScrappyTheme.darkButton(this, "Cancel analysis") { cancelAnalysis() }.apply { visibility = View.GONE }
        sheet.addView(cancelAnalysisButton, LinearLayout.LayoutParams(-1, -2))
        cameraRoot.addView(sheet, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(cameraRoot)
        applyScreenInsets(cameraRoot)
        cacheDir.listFiles()?.filter { it.name.startsWith("scrappy_") && it.name.endsWith(".jpg") && System.currentTimeMillis()-it.lastModified()>86_400_000L }?.forEach { it.delete() }
        analysis=savedInstanceState?.getString("analysis")
        val name=savedInstanceState?.getString("photo")
        val restored=name?.takeIf { it.startsWith("scrappy_") && it.endsWith(".jpg") && File(it).name == it }?.let { File(cacheDir,it) }?.takeIf { it.isFile }
        if(restored != null) {
            lastPhoto=restored;showReview(restored)
            analysis?.let { showResult(it) }
            return
        }
        ensureCameraPermission()
    }

    private fun styleZoomPills(selected: Button?) {
        zoomPills.forEach { pill ->
            val isSelected = pill === selected || (selected == null && pill.text.toString() == "1×")
            if (isSelected) {
                pill.setTextColor(ScrappyTheme.BG)
                pill.background = ScrappyTheme.rounded(this, ScrappyTheme.MINT, 28f)
                pill.typeface = Typeface.DEFAULT_BOLD
            } else {
                pill.setTextColor(ScrappyTheme.MUTED)
                pill.background = ScrappyTheme.rounded(this, ScrappyTheme.CARD2, 28f, ScrappyTheme.LINE, 1f)
                pill.typeface = Typeface.DEFAULT
            }
            pill.setPadding(ScrappyTheme.dp(this, 18f), ScrappyTheme.dp(this, 8f), ScrappyTheme.dp(this, 18f), ScrappyTheme.dp(this, 8f))
        }
    }

    private fun setZoom(target: Float, pill: Button) {
        val cam = camera
        if (cam != null) {
            val state = cam.cameraInfo.zoomState.value
            val min = state?.minZoomRatio ?: 1f
            val max = state?.maxZoomRatio ?: 1f
            runCatching { cam.cameraControl.setZoomRatio(target.coerceIn(min, max)) }
        }
        styleZoomPills(pill)
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
            zoomRow?.visibility=View.GONE
            scanFrame?.visibility=View.GONE
            status("Camera permission is required to capture a photo. Use the identification helper or allow Camera in Android Settings.")
        }
    }

    private fun status(s:String){statusView.text=s}
    private fun openExternal(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            status("No browser is available to open the privacy policy. Keep the photo local or use the manual helper.")
        }
    }
    private fun money(v:Double)=String.format(Locale.US,"$%.2f",v)
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
                val preview=Preview.Builder().build().also{it.setSurfaceProvider(previewView.surfaceProvider)}
                val capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                val selector=when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> error("No camera is available on this device")
                }
                releaseCamera()
                cameraPreview=preview;imageCapture=capture
                cameraProvider=provider
                camera=provider.bindToLifecycle(this,selector,preview,capture)
                captureButton.isEnabled=true
                retryCameraButton.visibility=View.GONE
                identifyButton.visibility=View.GONE
                zoomRow?.visibility=View.VISIBLE
                scanFrame?.visibility=View.VISIBLE
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
        zoomRow?.visibility=View.GONE
        scanFrame?.visibility=View.GONE
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
        zoomRow?.visibility=View.GONE
        scanFrame?.visibility=View.GONE
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
        setContentView(cameraRoot)
        applyScreenInsets(cameraRoot)
        consentDialog?.dismiss();consentDialog=null
        activeRequest?.cancel();activeRequest=null
        cancelAnalysisButton.visibility=View.GONE;retakeButton.isEnabled=true
        generation++;analysis=null;busy=false;aiButton.visibility=View.GONE;lastPhoto?.takeIf{it.exists()}?.delete();lastPhoto=null;reviewImage.setImageDrawable(null);reviewImage.visibility=View.GONE;previewView.visibility=View.VISIBLE
        captureButton.visibility=View.VISIBLE;captureButton.isEnabled=true;retakeButton.visibility=View.GONE;identifyButton.visibility=View.GONE
        zoomRow?.visibility=View.VISIBLE
        scanFrame?.visibility=View.VISIBLE
        status("Ready for another photo.");ensureCameraPermission()
    }

    /** Result-detail screen in the Copper Pipe style, shown after a successful AI identification. */
    private fun showResult(report: String) {
        val group = Regex("Likely group: (.+)").find(report)?.groupValues?.getOrNull(1)?.trim() ?: "Unknown"
        val confidence = Regex("Visual confidence: (low|medium|high)").find(report)?.groupValues?.getOrNull(1)?.trim() ?: "low"
        val sv = ScrollView(this).apply { setBackgroundColor(ScrappyTheme.BG) }
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 28) }
        sv.addView(l)
        val header = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ScrappyTheme.CARD)
            reviewImage.drawable?.let { setImageDrawable(it) }
        }
        l.addView(header, LinearLayout.LayoutParams(-1, ScrappyTheme.dp(this, 240f)).apply { bottomMargin = 14 })
        val photo = lastPhoto
        if (photo != null && header.drawable == null) {
            worker.execute {
                val result = runCatching { AiPhotoClient.reviewBitmap(photo) }
                runOnUiThread {
                    if (!isDestroyed && !isFinishing && lastPhoto == photo) result.getOrNull()?.let { header.setImageBitmap(it) }
                }
            }
        }
        val card = ScrappyTheme.cardLayout(this); l.addView(card)
        card.addView(ScrappyTheme.serifText(this, group, 26f))
        val confLabel = confidence.replaceFirstChar { it.uppercaseChar() }
        val ringRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 12, 0, 4) }
        val ring = ConfidenceRingView(this).apply {
            fraction = when (confidence) { "high" -> 1f; "medium" -> 0.66f; else -> 0.33f }
            label = confLabel
        }
        ringRow.addView(ring, LinearLayout.LayoutParams(ScrappyTheme.dp(this, 96f), ScrappyTheme.dp(this, 96f)))
        val confText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 0, 0, 0) }
        confText.addView(TextView(this).apply { text = "$confLabel visual confidence"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
        confText.addView(TextView(this).apply { text = "AI visual estimate — not independently verified."; textSize = 14f; setTextColor(ScrappyTheme.MUTED) })
        ringRow.addView(confText, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(ringRow)
        card.addView(TextView(this).apply { text = resultPriceLine(group); textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.MINT); setPadding(0, 8, 0, 0) })
        val parts = report.split("\n\n")
        val observations = parts.getOrNull(1) ?: ""
        val checks = parts.getOrNull(2)?.removePrefix("Confirm with:")?.trim() ?: ""
        if (observations.isNotBlank()) {
            val seen = ScrappyTheme.cardLayout(this); l.addView(seen)
            seen.addView(TextView(this).apply { text = "What the AI saw"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
            seen.addView(TextView(this).apply { text = observations; textSize = 15f; setTextColor(ScrappyTheme.MUTED) })
        }
        if (checks.isNotBlank()) {
            val checkCard = ScrappyTheme.cardLayout(this); l.addView(checkCard)
            checkCard.addView(TextView(this).apply { text = "Confirm with"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
            checkCard.addView(TextView(this).apply { text = checks; textSize = 15f; setTextColor(ScrappyTheme.MUTED) })
        }
        val yardsCard = ScrappyTheme.cardLayout(this); l.addView(yardsCard)
        yardsCard.addView(TextView(this).apply { text = "Top yards near you"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
        yardsCard.addView(TextView(this).apply { text = "Call for today's price — yards don't publish per-metal prices."; textSize = 14f; setTextColor(ScrappyTheme.MUTED) })
        RealYards.yards.forEach { yard ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 10, 0, 10) }
            row.addView(TextView(this).apply { text = yard.name; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
            row.addView(TextView(this).apply { text = "${yard.address} · ${RealYards.statusAt(yard)}"; textSize = 14f; setTextColor(ScrappyTheme.MUTED) })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(ScrappyTheme.darkButton(this@CameraActivity, "Call") {
                openExternal(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", yard.phone.filter { it.isDigit() || it == '+' }, null)))
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 8 })
            actions.addView(ScrappyTheme.darkButton(this@CameraActivity, "Directions") {
                openExternal(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(yard.address))))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actions)
            yardsCard.addView(row)
        }
        val tips = ScrappyTheme.cardLayout(this); l.addView(tips)
        tips.addView(TextView(this).apply { text = "Sorting tips"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ScrappyTheme.WHITE) })
        tips.addView(TextView(this).apply { text = GeorgiaPrices.sortingTip(group); textSize = 15f; setTextColor(ScrappyTheme.MUTED) })
        val directions = ScrappyTheme.mintButton(this, "Get directions") {
            openExternal(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(RealYards.closest.address))))
        }
        l.addView(directions, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
        val again = ScrappyTheme.darkButton(this, "Scan another item") { showCamera() }
        l.addView(again, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })
        val home = ScrappyTheme.darkButton(this, "Back to Prices") { finish() }
        l.addView(home, LinearLayout.LayoutParams(-1, -2))
        setContentView(sv)
    }

    private fun resultPriceLine(group: String): String {
        val quoteMetal = when (group) {
            "Copper" -> "Copper #2"
            "Brass" -> "Brass"
            "Aluminum" -> "Aluminum"
            "Steel/Iron" -> "Steel"
            "Stainless Steel" -> "Stainless Steel"
            "Lead" -> "Lead"
            "Zinc" -> "Zinc"
            else -> null
        }
        val quote = quoteMetal?.let { PriceQuotes.latest(it, Prefs(this).priceQuotes()) }
        if (quote != null) {
            val date = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(quote.recordedAt))
            return "Your saved quote: ${money(quote.pricePerLb)}/lb at ${quote.yard} ($date)"
        }
        val ga = GeorgiaPrices.priceForGroup(group)
        if (ga != null) return "${money(ga.pricePerLb)}/lb — Georgia index (${ga.grade}), ${ga.priceDate}"
        return "No Georgia index price for this metal — ask the yard."
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
                            result.onSuccess { analysis=it;showResult(it) }.onFailure { status(it.message ?: "AI analysis failed. Use the manual helper.") }
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
        cameraPreview=null;imageCapture=null;camera=null
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
