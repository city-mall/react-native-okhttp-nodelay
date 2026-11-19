package live.citymall.okhttpnodelay

import com.facebook.react.modules.network.CustomClientBuilder
import com.facebook.react.modules.network.NetworkingModule
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class OkHttpNoDelayPackageTest {

    // NetworkingModule exposes only a setter; read the slot it applies to every fetch / XHR.
    // If React Native moves or renames it, this test fails and the hook needs a new look.
    private fun registeredBuilder(): CustomClientBuilder? =
        NetworkingModule::class.java.getDeclaredField("customClientBuilder")
            .apply { isAccessible = true }
            .get(null) as CustomClientBuilder?

    @Before
    @After
    fun clearSlot() = NetworkingModule.setCustomClientBuilder(null)

    @Test
    fun `constructing the package makes React Native's per-request client use the no-delay factory`() {
        OkHttpNoDelayPackage()

        val builder = OkHttpClient.Builder()
        registeredBuilder()?.apply(builder)

        assertSame(NoDelaySocketFactory, builder.build().socketFactory)
    }
}
