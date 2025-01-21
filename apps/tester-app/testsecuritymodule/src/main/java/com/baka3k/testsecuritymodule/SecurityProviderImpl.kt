package com.baka3k.testsecuritymodule

import android.content.Context
import android.util.Base64
import com.baka3k.testsecuritymodule.data.Preference
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class SecurityProviderImpl(
    private val miniAppId: String? = null,
    private val appContext: Context,
    private val preference: Preference
) : SecurityProvider {

    override fun getPublicKeyByAppId(): PublicKey? {
        if (miniAppId.isNullOrBlank()) {
            val packageName: String = appContext.packageName
            val resId: Int =
                appContext.resources.getIdentifier("public_key_default", "string", packageName)
            if (resId != 0) {
                val publicKeyAsString = appContext.getString(resId).ifEmpty {
                    null
                }
                if (publicKeyAsString == null) {
                    return null
                }
                return parsePublicKey(publicKeyAsString)
            } else {
                return null
            }
        } else {
            val publicKeyAsString = preference.getPublicKey(appId = miniAppId)
            return parsePublicKey(publicKeyAsString)
        }
    }

    override fun getPublicKeyByBundlePath(bundlePath: String): PublicKey? {
        return getPublicKeyByAppId()// just for test
    }

    override fun setBundleIdentify(bundlePath: String, hash: String) {
        preference.setBundleHash(hash=hash, path = bundlePath)
    }

    override fun getHash(bundlePath: String): String? {
        return preference.getBundleHash(path = bundlePath)
    }

    private fun parsePublicKey(stringPublicKey: String): PublicKey? {
        val formattedPublicKey = stringPublicKey.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(System.getProperty("line.separator")!!, "")

        val byteKey: ByteArray = Base64.decode(formattedPublicKey.toByteArray(), Base64.DEFAULT)
        val x509Key = X509EncodedKeySpec(byteKey)
        val kf = KeyFactory.getInstance("RSA")

        return kf.generatePublic(x509Key)
    }
}
