package com.baka3k.testsecuritymodule

import android.util.Log
import com.baka3k.testsecuritymodule.ex.toHexString
import com.baka3k.testsecuritymodule.ex.toSHA256
import com.callstack.repack.TamperingDetector
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.text.ParseException

class TamperingDetectorImpl(
    private val securityProvider: SecurityProvider? = null,
) : TamperingDetector {
    @Throws(IOException::class, SecurityException::class)
    override fun verifyBundle(bundlePath: String): Boolean {
        val bundleFile = File(bundlePath)
        if (!bundleFile.exists()) {
            throw IOException("Bundle File not found")
        }
        if (bundleFile.isDirectory) {
            throw IOException("bundlePath is invalid")
        }
        val bundleFileContent = bundleFile.readBytes()
        val bundleAndToken = extractBundleAndToken(bundleFileContent)
        val bundleContent = bundleAndToken.first
        val bundleToken = bundleAndToken.second

        if (bundleToken.isNullOrBlank()) {
            // there are 2 case bundleToken null
            // 1. bundle is extracted - just check hash in DB
            // 2. no need to verify
            return verifyExtractedBundle(bundlePath, bundleContent)
        } else {
            // extract hash & save to DB
            val publicKey = securityProvider?.getPublicKeyByBundlePath(bundlePath)
            val verify = verifyUnExtractedBundle(
                bundlePath = bundlePath,
                bundleContent = bundleContent,
                bundleToken = bundleToken,
                publicKey = publicKey
            )
            return verify
        }
    }

    override fun extractBundleAndTokenFromDownload(
        rawBundle: ByteArray?,
        absolutePath: String,
    ) {
        if (rawBundle == null) {
            Log.w(TAG, "#extractBundleAndTokenFromDownload() rawBundle null")
            return
        }
        val publicKey = securityProvider?.getPublicKeyByAppId()
        if (publicKey == null) {
            Log.w(TAG, "#extractBundleAndTokenFromDownload() public key null")
            return
        }
        val bundleAndToken = extractBundleAndToken(rawBundle)
        val bundleContent = bundleAndToken.first
        val bundleToken = bundleAndToken.second
        if (bundleToken.isNullOrBlank()) {
            Log.w(
                TAG,
                "#extractBundleAndTokenFromDownload() bundleToken null - package is not signed"
            )
            return
        }
        val hash = extractHashFromToken(bundleToken = bundleToken, publicKey = publicKey)
        if (hash.isNullOrBlank()) {
            Log.w(
                TAG,
                "#extractBundleAndTokenFromDownload() bundleToken null - public key is invalid or hash is not included in package"
            )
            return
        }
        val bundleHash = getHash(bundleContent)
        if (bundleHash == hash) {
            // save hash
            securityProvider?.setBundleIdentify(bundlePath = absolutePath, hash = hash)
            // save bundle
            saveByteArrayToFile(byteArray = bundleContent, filePath = absolutePath)
        }
    }

    private fun verifyExtractedBundle(bundlePath: String, bundleContent: ByteArray): Boolean {
        // query hash from DB
        val hashInDB = securityProvider?.getHash(bundlePath)
        if (hashInDB == null) {
            return false
        } else {
            // check hash in DB & hash of file
            val hashFile = getHash(bundleContent)
            return hashInDB == hashFile
        }
    }

    private fun verifyUnExtractedBundle(
        bundlePath: String,
        bundleContent: ByteArray,
        bundleToken: String?,
        publicKey: PublicKey?
    ): Boolean {
        val hashBundle = bundleContent.toSHA256().toHexString()
        if (bundleToken.isNullOrBlank() || publicKey == null) {
            return false
        } else {
            val hash = extractHashFromToken(bundleToken = bundleToken, publicKey = publicKey)
            if (!hash.isNullOrBlank()) {
                if (hashBundle == hash) {
                    // save hash after verify
                    securityProvider?.setBundleIdentify(
                        bundlePath = bundlePath,
                        hash = hash
                    )
                    // save bundle
                    // replace file
                    saveByteArrayToFile(byteArray = bundleContent, filePath = bundlePath)
                    return true
                }
            }
        }
        return false
    }

    private fun extractHashFromToken(bundleToken: String, publicKey: PublicKey): String? {
        try {
            val signedJWT = runCatching { SignedJWT.parse(bundleToken) }.getOrNull()
            val jwtVerifer = RSASSAVerifier(publicKey as RSAPublicKey)
            val value = signedJWT!!.verify(jwtVerifer)
            if (value) {
                val claims = signedJWT.jwtClaimsSet.claims
                return claims["hash"] as? String?
            } else {
                return null
            }
        } catch (ex: ParseException) {
            Log.e(TAG, "#extractHashFromToken() ParseException:${ex.message}", ex)
        } catch (ex: JOSEException) {
            Log.e(TAG, "#extractHashFromToken() JOSEException:${ex.message}", ex)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "#extractHashFromToken() IllegalStateException:${ex.message}", ex)
        }
        return null
    }

    private fun extractBundleAndToken(fileContent: ByteArray): Pair<ByteArray, String?> {
        val signatureSize = SIGNATURE_SIZE // 821 byte token + 10 byte delimiter
        if (fileContent.size < signatureSize) {
            return Pair(fileContent, null)
        }
        val lastBytes = fileContent.takeLast(signatureSize).toByteArray()
        val signatureString = lastBytes.toString(Charset.forName("UTF-8"))
        if (signatureString.startsWith(DELIMITER)) {
            val token = signatureString.removePrefix(DELIMITER).replace("\u0000", "").trim()
            val bundle = fileContent.copyOfRange(0, fileContent.size - signatureSize)
            return Pair(bundle, token)
        } else {
            return Pair(fileContent, null)
        }
    }

    private fun getHash(bundleContent: ByteArray): String {
        return bundleContent.toSHA256().toHexString()
    }

    private fun saveByteArrayToFile(byteArray: ByteArray, filePath: String) {
        try {
            FileOutputStream(filePath).use { outputStream ->
                outputStream.write(byteArray)
            }
        } catch (e: IOException) {
            Log.w(TAG, "#saveByteArrayToFile() err ${e.message}", e)
        }
    }

    companion object {
        const val DELIMITER = "/* CSSB */"
        const val TAG = "TamperingDetector"
        const val SIGNATURE_SIZE = 831 // 821 byte token + 10 byte delimiter
    }
}
