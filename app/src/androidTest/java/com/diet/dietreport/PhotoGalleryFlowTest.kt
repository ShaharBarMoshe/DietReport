package com.diet.dietreport

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.diet.dietreport.data.db.AppDatabase
import com.diet.dietreport.data.db.LogSource
import com.diet.dietreport.data.db.MealLog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

private const val TIMEOUT = 10_000L

@RunWith(AndroidJUnit4::class)
class PhotoGalleryFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val db = AppDatabase.getInstance(context)
    private lateinit var testImageFile: File
    private var testLogId = -1L
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        device.pressHome()

        // Create a minimal JPEG
        testImageFile = File(context.filesDir, "meals/test_gallery.jpg")
            .also { it.parentFile?.mkdirs() }
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        testImageFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bmp.recycle()

        // Insert a meal log
        testLogId = runBlocking {
            db.mealLogDao().insert(
                MealLog(
                    reminderSlotId = null,
                    photoPath = testImageFile.absolutePath,
                    loggedAt = System.currentTimeMillis(),
                    source = LogSource.CAMERA,
                )
            )
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { db.mealLogDao().deleteById(testLogId) }
        testImageFile.delete()
    }

    @Test
    fun galleryScreen_showsPhotos() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Photos tab
        assertTrue(device.wait(Until.hasObject(By.text("Photos")), TIMEOUT))
        device.findObject(By.text("Photos")).click()

        // Gallery screen is visible
        assertTrue(device.wait(Until.hasObject(By.res("photo_gallery_screen")), TIMEOUT))

        // Photo card is shown
        assertTrue(device.wait(Until.hasObject(By.res("photo_card_$testLogId")), TIMEOUT))
    }

    @Test
    fun galleryScreen_deletePhotoFlow() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Photos tab
        assertTrue(device.wait(Until.hasObject(By.text("Photos")), TIMEOUT))
        device.findObject(By.text("Photos")).click()
        assertTrue(device.wait(Until.hasObject(By.res("photo_gallery_screen")), TIMEOUT))

        // Tap delete icon on the photo card
        val deleteButton = device.findObject(By.desc("Delete photo"))
        deleteButton.click()

        // Confirmation dialog appears
        assertTrue(device.wait(Until.hasObject(By.text("Delete Photo")), TIMEOUT))

        // Confirm deletion
        device.findObject(By.text("Delete")).click()

        // Photo card should disappear
        assertTrue(device.wait(Until.gone(By.res("photo_card_$testLogId")), TIMEOUT))

        // Verify DB
        val logs = runBlocking { db.mealLogDao().logsForSlot(testLogId) }
        // After delete, the meal log should be gone
        val allRemainingForId = runBlocking {
            db.mealLogDao().offScheduleLogsForRange(0, Long.MAX_VALUE)
                .filter { it.id == testLogId }
        }
        assertEquals(0, allRemainingForId.size)
    }
}
