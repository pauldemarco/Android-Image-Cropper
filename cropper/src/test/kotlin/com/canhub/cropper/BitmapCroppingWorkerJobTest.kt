package com.canhub.cropper

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BitmapCroppingWorkerJobTest {
  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    mockkObject(BitmapUtils)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `output failure reaches the crop listener once`() = runTest(timeout = 3.seconds) {
    val bitmap = mockk<Bitmap>()
    val context = mockk<Context>()
    val view = mockk<CropImageView>()
    val output = mockk<Uri>()
    val failure = IOException("Output directory was removed")
    val completion = CompletableDeferred<BitmapCroppingWorkerJob.Result>()
    every {
      BitmapUtils.cropBitmapObjectHandleOOM(any(), any(), any(), any(), any(), any(), any(), any())
    } returns BitmapUtils.BitmapSampled(bitmap, 1)
    every { BitmapUtils.resizeBitmap(any(), any(), any(), any()) } returns bitmap
    every { BitmapUtils.writeBitmapToUri(context, bitmap, any(), any(), output) } throws failure
    every { view.onImageCroppingAsyncComplete(any()) } answers {
      completion.complete(firstArg())
      Unit
    }
    val worker = BitmapCroppingWorkerJob(
      context = context,
      cropImageViewReference = WeakReference(view),
      uri = null,
      bitmap = bitmap,
      cropPoints = floatArrayOf(),
      degreesRotated = 0,
      orgWidth = 16,
      orgHeight = 16,
      fixAspectRatio = false,
      aspectRatioX = 1,
      aspectRatioY = 1,
      reqWidth = 0,
      reqHeight = 0,
      flipHorizontally = false,
      flipVertically = false,
      options = CropImageView.RequestSizeOptions.NONE,
      saveCompressFormat = Bitmap.CompressFormat.JPEG,
      saveCompressQuality = 90,
      customOutputUri = output,
    )
    try {
      worker.start()
      val result = completion.await()
      assertTrue(result.error is IOException)
      assertEquals(failure.message, result.error?.message)
      assertNull(result.uri)
      verify(exactly = 1) { view.onImageCroppingAsyncComplete(any()) }
    } finally {
      worker.cancel()
    }
  }
}
