package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class MainActivityLaunchTest {

  private val firebaseRule = object : ExternalResource() {
    override fun before() {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
            .setApplicationId("1:345795744222:android:75197eee81b278a9b3cd4f")
            .setProjectId("gen-lang-client-0389450959")
            .setApiKey("fake-api-key")
            .build()
        )
      }
    }
  }

  private val composeRule = createAndroidComposeRule<MainActivity>()

  @get:Rule
  val ruleChain: RuleChain = RuleChain.outerRule(firebaseRule).around(composeRule)

  @Test
  fun mainActivity_rendersAuthScreenWithoutCrash() {
    composeRule.onNodeWithText("Cadence").assertIsDisplayed()
    composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
  }
}
