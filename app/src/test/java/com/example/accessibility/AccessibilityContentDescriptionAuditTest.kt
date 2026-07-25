package com.example.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AccessibilityContentDescriptionAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val accessibilityAuditRule = AccessibilityAuditRule(composeTestRule)

    @Test
    fun auditCafeteriaUiComponents_allInteractiveElementsHaveContentDescriptions() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "View Cart"
                        )
                    }

                    Button(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item to Order"
                        )
                        Text("Add to Order")
                    }
                }
            }
        }

        val auditResult = accessibilityAuditRule.auditContentDescriptions()

        assertTrue("All interactive cafeteria elements must have TalkBack content descriptions", auditResult.isCompliant)
        assertEquals("Unlabelled count should be 0", 0, auditResult.unlabelledCount)
    }
}
