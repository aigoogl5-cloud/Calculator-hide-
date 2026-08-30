package com.example.security

import java.security.MessageDigest

object CryptoUtils {
    fun hashString(input: String): String {
        if (input.isEmpty()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPasscode(input: String, expectedHash: String): Boolean {
        if (expectedHash.isEmpty()) return false
        return hashString(input) == expectedHash
    }
}
