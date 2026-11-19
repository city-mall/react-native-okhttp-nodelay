package live.citymall.okhttpnodelay

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.network.NetworkingModule
import com.facebook.react.uimanager.ViewManager

/**
 * Picked up by the host app's autolinking, which constructs it before React Native runs any JS.
 * Constructing it is the whole library: NetworkingModule applies the custom client builder to the
 * per-request `client.newBuilder()` behind fetch / XHR, so every such connection gets Nagle off
 * while the shared client (cache, cookie jar, pool, timeouts) stays as React Native built it.
 *
 * NetworkingModule has a single static custom-builder slot, and this package owns it.
 */
class OkHttpNoDelayPackage : ReactPackage {

    init {
        NetworkingModule.setCustomClientBuilder { it.socketFactory(NoDelaySocketFactory) }
    }

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> =
        emptyList()

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> =
        emptyList()
}
