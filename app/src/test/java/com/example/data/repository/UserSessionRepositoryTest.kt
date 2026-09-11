package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test Suite for UserSessionRepository.
 * Verifies session persistence, authentication state checks, and biometric credentials storage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserSessionRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserSessionRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = UserSessionRepository(context)
        repository.clearSession()
    }

    @Test
    fun testIsLoggedIn_ReturnsTrueWhenTokenExists() {
        repository.saveSession("mock_jwt_bearer_token_12345", 42, "Kofi Mensah", "STUDENT")
        val result = repository.isLoggedIn()
        assertTrue(result)
        assertEquals(42, repository.getUserId())
        assertEquals("Kofi Mensah", repository.getUserName())
        assertEquals("STUDENT", repository.getUserRole())
    }

    @Test
    fun testIsLoggedIn_ReturnsFalseWhenCleared() {
        repository.saveSession("mock_jwt_bearer_token_12345", 42, "Kofi Mensah", "STUDENT")
        assertTrue(repository.isLoggedIn())

        repository.clearSession()
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun testBiometricPreferences() {
        repository.setBiometricLoginEnabled(true, "kofi_student", "1234")
        assertTrue(repository.isBiometricLoginEnabled())
        assertEquals("kofi_student", repository.getSavedBiometricUsername())
        assertEquals("1234", repository.getSavedBiometricPin())

        repository.setBiometricLoginEnabled(false)
        assertFalse(repository.isBiometricLoginEnabled())
    }
}
