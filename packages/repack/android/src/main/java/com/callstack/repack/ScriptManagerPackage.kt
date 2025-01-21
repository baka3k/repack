package com.callstack.repack

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import java.security.PublicKey

/**
 * DO inject an external scope instead of using GlobalScope.
 * GlobalScope can be used indirectly. Here as a default parameter makes sense.
 * refer: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
 * */
class
ScriptManagerPackage(
    private val tamperingDetector: TamperingDetector? = null,
    private val coroutineScope: CoroutineScope = GlobalScope, // default
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO //default
) : TurboReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
        return if (name == ScriptManagerModule.NAME) {
            ScriptManagerModule(
                reactContext,
                tamperingDetector = tamperingDetector,
                coroutineScope = coroutineScope,
                coroutineDispatcher = coroutineDispatcher
            )
        } else {
            null
        }
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        return ReactModuleInfoProvider {
            val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
            val isTurboModule: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            // Use deprecated constructor for backwards compatibility
            moduleInfos[ScriptManagerModule.NAME] = ReactModuleInfo(
                ScriptManagerModule.NAME,
                ScriptManagerModule.NAME,
                false,
                true,
                false,
                false,
                isTurboModule
            )
            moduleInfos
        }
    }
}
