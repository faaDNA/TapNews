package com.example.tapnews.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {
    @Test
    fun `isValidEmail returns true for valid email`() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"))
        assertTrue(ValidationUtils.isValidEmail("test.user@domain.co"))
    }

    @Test
    fun `isValidEmail returns false for invalid email`() {
        assertFalse(ValidationUtils.isValidEmail(""))
        assertFalse(ValidationUtils.isValidEmail("userexample.com"))
        assertFalse(ValidationUtils.isValidEmail("user@.com"))
    }

    @Test
    fun isValidPassword_returnsTrue_forPasswordLength6OrMore() {
        assertTrue(ValidationUtils.isValidPassword("123456"))
        assertTrue(ValidationUtils.isValidPassword("abcdef"))
    }

    @Test
    fun isValidPassword_returnsFalse_forPasswordLengthLessThan6() {
        assertFalse(ValidationUtils.isValidPassword("12345"))
        assertFalse(ValidationUtils.isValidPassword("abc"))
    }
}
