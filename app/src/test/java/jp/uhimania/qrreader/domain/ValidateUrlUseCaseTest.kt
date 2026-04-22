package jp.uhimania.qrreader.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateUrlUseCaseTest {
    @Test
    fun testInvoke() {
        val useCase = ValidateUrlUseCase()
        assertTrue(useCase("http://google.com/"))
        assertTrue(useCase("https://google.co.jp/"))
        assertTrue(useCase("http://google.com"))
        assertTrue(useCase("http://google.com/?query=test"))
        assertFalse(useCase("google.com"))
        assertFalse(useCase(""))
    }
}