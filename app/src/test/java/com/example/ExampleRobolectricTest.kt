package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.HumanBehaviorUtility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AutoClicker AI", appName)
  }

  @Test
  fun `verify human behavior utility coordinates in android runtime`() {
    val result = HumanBehaviorUtility.calculateHumanizedPoint(500f, 1000f, 8f)
    assertNotNull(result)
    assertTrue(result.x > 0f)
    assertTrue(result.y > 0f)
  }
}

