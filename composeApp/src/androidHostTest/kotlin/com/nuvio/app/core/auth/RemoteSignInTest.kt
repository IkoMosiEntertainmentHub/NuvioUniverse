package com.nuvio.app.core.auth

import android.app.Application
import com.nuvio.app.core.network.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RemoteSignInTest {
    @BeforeTest fun initialize() {
        AuthStorage.initialize(RuntimeEnvironment.getApplication())
        AuthStorage.clearAnonymousUserId()
    }

    @AfterTest fun clean() = runTest {
        AuthStorage.clearAnonymousUserId()
        SupabaseProvider.client.auth.clearSession()
    }

    @Test fun successfulRemoteSessionReplacesGuestMode() = runTest {
        AuthRepository.signInAnonymously()
        assertNotNull(AuthStorage.loadAnonymousUserId())
        val id = "f7082501-853e-4e40-bd0b-a6cf222a0f79"
        // Local SDK session import only. No password or remote account login is used.
        SupabaseProvider.client.auth.importSession(UserSession(
            accessToken = "local-unit-test-token", refreshToken = "local-unit-test-refresh",
            expiresIn = 3600, tokenType = "bearer", user = UserInfo(id = id),
        ))
        AuthRepository.acceptRemoteSignIn()
        assertNull(AuthStorage.loadAnonymousUserId())
        assertEquals(AuthState.Authenticated(id, null, false), AuthRepository.state.value)
    }

    @Test fun absentSessionDoesNotDiscardGuestMode() = runTest {
        SupabaseProvider.client.auth.clearSession()
        AuthRepository.signInAnonymously()
        val guest = AuthStorage.loadAnonymousUserId()
        AuthRepository.acceptRemoteSignIn()
        assertEquals(guest, AuthStorage.loadAnonymousUserId())
        assertTrue(AuthRepository.state.value.isAnonymous)
    }
}
