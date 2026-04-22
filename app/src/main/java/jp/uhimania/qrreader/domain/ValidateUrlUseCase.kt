package jp.uhimania.qrreader.domain

import java.net.URL

class ValidateUrlUseCase {
    operator fun invoke(text: String): Boolean {
        return try {
            URL(text)
            true
        } catch (_: Exception) {
            false
        }
    }
}
