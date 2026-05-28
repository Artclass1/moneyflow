package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Global MoneyFlow", appName)
  }

  @Test
  fun testChatScreenLaunch() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(context)
    val repository = ChatRepository(database.chatDao())
    val viewModel = ChatViewModel(context, repository)
    
    composeTestRule.setContent {
      ChatScreen(viewModel = viewModel)
    }
    
    composeTestRule.waitForIdle()
    assertNotNull(viewModel)
  }
}
