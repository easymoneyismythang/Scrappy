package com.scrappy.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import com.scrappy.app.data.RequestCancellation
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real camera or an emulator with an emulated camera.
 * No AI endpoint is needed and no photo is uploaded by these tests.
 */
@RunWith(AndroidJUnit4::class)
class CameraFlowTest {
    @Before
    fun grantCameraPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val descriptor = instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${context.packageName} ${Manifest.permission.CAMERA}"
        )
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        assertEquals(PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.CAMERA))
    }

    @Test
    fun retryRecoversFromCameraStartupFailure() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            awaitCapture(scenario)
            scenario.onActivity { activity ->
                // Simulate the shared failure handler without depending on a
                // particular camera device or introducing a production hook.
                CameraActivity::class.java.getDeclaredMethod("cameraStartupFailed")
                    .apply { isAccessible = true }.invoke(activity)
                assertEquals(false, button(activity, "Capture photo").isEnabled)
                assertEquals(true, button(activity, "Retry camera").isShown)
                assertEquals(true, button(activity, "Use identification helper").isShown)
            }
            click(scenario, "Retry camera")
            awaitCapture(scenario)
            awaitUi(scenario, "Successful retry clears recovery controls") {
                !button(it, "Retry camera").isShown &&
                    !button(it, "Use identification helper").isShown
            }
            capturePhoto(scenario)
            awaitReview(scenario)
            awaitUi(scenario, "Camera retry stays hidden during photo review") {
                !button(it, "Retry camera").isShown
            }
        }
    }

    @Test
    fun captureThenRetakeRestartsPreview() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            capturePhoto(scenario)
            awaitReview(scenario)
            click(scenario, "Retake")
            awaitCapture(scenario)
            // A second capture proves that retake restored a usable use case.
            capturePhoto(scenario)
            awaitReview(scenario)
        }
    }

    @Test
    fun capturedPhotoSurvivesActivityRecreation() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            capturePhoto(scenario)
            awaitReview(scenario)
            scenario.recreate()
            awaitReview(scenario)
            awaitUi(scenario, "Capture remains hidden during restored review") {
                !button(it, "Capture photo").isShown
            }
            click(scenario, "Retake")
            awaitCapture(scenario)
        }
    }

    @Test
    fun previewRecoversAfterBackgrounding() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            awaitCapture(scenario)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            capturePhoto(scenario)
            awaitReview(scenario)
        }
    }

    @Test
    fun startupRecoversWhenImmediatelyBackgrounded() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            // Do not wait for CameraX provider initialization before leaving.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            capturePhoto(scenario)
            awaitReview(scenario)
        }
    }

    @Test
    fun previewRecoversAfterActivityRecreation() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            awaitCapture(scenario)
            scenario.recreate()
            capturePhoto(scenario)
            awaitReview(scenario)
        }
    }

    @Test
    fun localReviewSurvivesBackgrounding() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            capturePhoto(scenario)
            awaitReview(scenario)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitReview(scenario)
            awaitUi(scenario, "Local review controls remain available") {
                !button(it, "Capture photo").isShown &&
                    button(it, "Analyze with AI").isShown &&
                    button(it, "Use identification helper").isShown
            }
        }
    }

    @Test
    fun cancelAnalysisKeepsPhotoAndAllowsRetake() {
        ActivityScenario.launch(CameraActivity::class.java).use { scenario ->
            capturePhoto(scenario)
            awaitReview(scenario)
            val request = RequestCancellation()
            var disconnected = false
            request.attach { disconnected = true }
            var photo: File? = null
            scenario.onActivity { activity ->
                assertFalse(button(activity, "Cancel analysis").isShown)
                // Supply an in-flight request without uploading a photo or
                // requiring live credentials in the device test suite.
                CameraActivity::class.java.getDeclaredField("lastPhoto").apply {
                    isAccessible = true
                    photo = get(activity) as File
                }
                CameraActivity::class.java.getDeclaredField("activeRequest").apply {
                    isAccessible = true; set(activity, request)
                }
                CameraActivity::class.java.getDeclaredField("busy").apply {
                    isAccessible = true; setBoolean(activity, true)
                }
                button(activity, "Analyze with AI").isEnabled = false
                button(activity, "Retake").isEnabled = false
                button(activity, "Cancel analysis").visibility = View.VISIBLE
            }
            click(scenario, "Cancel analysis")
            scenario.onActivity { activity ->
                assertTrue("Cancel must close the attached transport", disconnected)
                assertTrue("Cancel must preserve the local photo", photo?.isFile == true)
                assertFalse(button(activity, "Cancel analysis").isShown)
                assertTrue(button(activity, "Analyze with AI").isEnabled)
                assertTrue(button(activity, "Retake").isEnabled)
                assertTrue(views(activity.window.decorView).filterIsInstance<TextView>()
                    .any { it.text.toString().startsWith("Analysis canceled.") })
            }
            click(scenario, "Retake")
            awaitCapture(scenario)
            assertFalse("Retake should remove the canceled photo", photo?.exists() == true)
            capturePhoto(scenario)
            awaitReview(scenario)
        }
    }

    private fun capturePhoto(scenario: ActivityScenario<CameraActivity>) {
        awaitCapture(scenario)
        click(scenario, "Capture photo")
    }

    private fun awaitCapture(scenario: ActivityScenario<CameraActivity>) =
        awaitUi(scenario, "Camera capture ready") {
            button(it, "Capture photo").let { b -> b.isShown && b.isEnabled }
        }

    private fun awaitReview(scenario: ActivityScenario<CameraActivity>) =
        awaitUi(scenario, "Captured photo decoded and review ready") { activity ->
            button(activity, "Retake").isShown &&
                // Review image is a direct child of the app's vertical layout;
                // action-bar icons must not satisfy the decoded-photo check.
                views(activity.findViewById<ViewGroup>(android.R.id.content))
                    .filterIsInstance<ImageView>()
                    .any { it.isShown && it.drawable != null }
        }

    private fun click(scenario: ActivityScenario<CameraActivity>, label: String) {
        scenario.onActivity { activity ->
            val button = button(activity, label)
            check(button.isShown && button.isEnabled) { "$label is not available" }
            check(button.performClick()) { "$label did not handle the click" }
        }
    }

    private fun button(activity: CameraActivity, label: String): Button =
        views(activity.window.decorView).filterIsInstance<Button>()
            .first { it.text.toString() == label }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            yieldAll(views(view.getChildAt(index)))
        }
    }

    private fun awaitUi(
        scenario: ActivityScenario<CameraActivity>,
        description: String,
        predicate: (CameraActivity) -> Boolean
    ) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 15_000
        do {
            var ready = false
            scenario.onActivity { ready = predicate(it) }
            if (ready) return
            Thread.sleep(100)
        } while (android.os.SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("Timed out: $description")
    }
}
