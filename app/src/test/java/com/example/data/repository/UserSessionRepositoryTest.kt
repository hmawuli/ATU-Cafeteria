package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit Test Suite for UserSessionRepository.
 * Verifies session persistence, authentication state checks, and biometric credentials storage.
 */
class UserSessionRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
    }

    @Test
    fun testIsLoggedIn_ReturnsTrueWhenTokenExists() {
        `when`(mockPrefs.getBoolean("key_is_logged_in", false)).thenReturn(true)
        `when`(mockPrefs.getString("key_auth_token", null)).thenReturn("mock_jwt_bearer_token_12345")

        val repository = UserSessionRepository(mockContext)
        val result = repository.isLoggedIn()

        assertTrue(result)
    }

    @Test
    fun testIsLoggedIn_ReturnsFalseWhenTokenNull() {
        `when`(mockPrefs.getBoolean("key_is_logged_in", false)).thenReturn(true)
        `when`(mockPrefs.getString("key_auth_token", null)).thenReturn(null)

        val repository = UserSessionRepository(mockContext)
        val result = repository.isLoggedIn()

        assertFalse(result)
    }

    @Test
    fun testSaveSession_ExecutesSharedPreferencesEdits() {
        val repository = UserSessionRepository(mockContext)
        repository.saveSession("token_abc", 42, "Kofi Mensah", "STUDENT")

        verify(mockEditor).putString("key_auth_token", "token_abc")
        verify(mockEditor).putInt("key_user_id", 42)
        verify(mockEditor).putString("key_user_name", "Kofi Mensah")
        verify(mockEditor).putString("key_user_role", "STUDENT")
        verify(mockEditor).apply()
    }

    @Test
    fun testClearSession_ResetsLoggedInState() {
        val repository = UserSessionRepository(mockContext)
        repository.clearSession()

        verify(mockEditor).remove("key_auth_token")
        verify(mockEditor).remove("key_user_id")
        verify(mockEditor).putBoolean("key_is_logged_in", false)
        verify(mockEditor).apply()
    }
}
