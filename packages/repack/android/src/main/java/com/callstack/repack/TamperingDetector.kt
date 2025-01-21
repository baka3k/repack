package com.callstack.repack


interface TamperingDetector {
    @Throws(SecurityException::class)
    fun verifyBundle(bundlePath: String): Boolean
    fun extractBundleAndTokenFromDownload(
        rawBundle: ByteArray?,
        absolutePath: String,
    )
}
