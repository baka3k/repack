package com.baka3k.testsecuritymodule

import java.security.PublicKey

interface SecurityProvider {
    fun getPublicKeyByAppId(): PublicKey?
    fun getPublicKeyByBundlePath(bundlePath: String): PublicKey?
    fun setBundleIdentify(bundlePath: String, hash: String)
    fun getHash(bundlePath: String): String?
}
