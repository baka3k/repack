package com.baka3k.testsecuritymodule.ex

import java.security.MessageDigest

fun String.toSHA256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    val input = this.toByteArray()
    return md.digest(input)
}
