package com.baka3k.testsecuritymodule.ex

import java.security.MessageDigest
private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.toSHA256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(this)
}

/**
 * Converts the byte array to HEX string.
 *
 * @param buffer
 * the buffer.
 * @return the HEX string.
 */
fun ByteArray.toHexString(): String {
    return toHexString(0, this.size)
}

/**
 * Converts the byte array to HEX string.
 *
 * @param buffer
 * the buffer.
 * @return the HEX string.
 */
fun ByteArray.toHexString(offset: Int, length: Int): String {
    val sb = StringBuilder()
    for (i in offset until (offset + length)) {
        val b = this[i]
        val octet = b.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        sb.append(HEX_CHARS[firstIndex])
        sb.append(HEX_CHARS[secondIndex])
    }
    return sb.toString()
}
