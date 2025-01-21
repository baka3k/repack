package com.callstack.repack

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContext
import java.io.File
import java.io.FileInputStream
import java.security.PublicKey

class FileSystemScriptLoader(
    private val reactContext: ReactContext,
    private val nativeLoader: NativeScriptLoader,
    private val tamperingDetector: TamperingDetector?
) {
    fun load(config: ScriptConfig, promise: Promise) {
        try {
            if (config.absolute) {
                val path = config.url.path
                val file = File(path)
                if (validateLocalBundle(file)) {
                    val code: ByteArray = FileInputStream(file).use { it.readBytes() }
                    nativeLoader.evaluate(code, config.sourceUrl, promise)
                } else {
                    rejectBundle(promise, SecurityException("Bundle is invalid"))
                }
            } else {
                val assetName = config.url.file.split("/").last()
                val inputStream = reactContext.assets.open(assetName)
                val code: ByteArray = inputStream.use { it.readBytes() }
                // asset file is static file - do not need verify
                nativeLoader.evaluate(code, config.sourceUrl, promise)
            }
        } catch (error: Exception) {
            rejectBundle(promise, error)
        }
    }

    private fun rejectBundle(promise: Promise, error: Exception) {
        promise.reject(
            ScriptLoadingError.ScriptEvalFailure.code, error.message ?: error.toString()
        )
    }

    /**
     * Anti-tampering
     */
    @Throws(SecurityException::class)
    private fun validateLocalBundle(file: File): Boolean {
        if (tamperingDetector == null) {
            //bypasss tamperingDetector
            return true
        } else {
            return tamperingDetector.verifyBundle(bundlePath = file.path)
        }
    }
}
