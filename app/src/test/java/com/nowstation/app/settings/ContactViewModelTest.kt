package com.nowstation.app.settings

import com.nowstation.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeApi(var fail: Boolean = false) : FeedbackApi {
        val sent = mutableListOf<FeedbackRequest>()
        override suspend fun send(request: FeedbackRequest) {
            if (fail) throw IOException("끊김")
            sent += request
        }
    }

    @Test
    fun `보내는 JSON에 플랫폼이 들어간다(기본값 칸은 kotlinx가 빼서 서버에 null로 가던 것)`() {
        // NetworkModule과 같은 설정(encodeDefaults 끔)
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val text = json.encodeToString(FeedbackRequest("안녕", null, "android", "0.1.70", "Pixel"))
        assertTrue(text, text.contains("\"platform\":\"android\""))
    }

    @Test
    fun `내용이 없거나 답장 주소 모양이 틀리면 보낼 수 없고, 주소는 비워도 된다`() {
        val vm = ContactViewModel(FakeApi())
        assertFalse(vm.state.value.canSend)
        vm.onMessage("늦게 바뀌어요")
        assertTrue(vm.state.value.canSend)
        vm.onContact("메일 아님")
        assertTrue(vm.state.value.contactInvalid)
        assertFalse(vm.state.value.canSend)
        vm.onContact("a@b.com")
        assertTrue(vm.state.value.canSend)
    }

    @Test
    fun `보내면 앞뒤 공백을 지우고 빈 주소는 null로, 보낸 뒤 완료 화면`() = runTest {
        val api = FakeApi()
        val vm = ContactViewModel(api)
        vm.onMessage("  강남이 늦어요  ")
        vm.onContact("  ")
        vm.send()
        advanceUntilIdle()
        assertEquals("강남이 늦어요", api.sent.single().message)
        assertNull(api.sent.single().contact)
        assertEquals("android", api.sent.single().platform)
        assertTrue(vm.state.value.sent)
    }

    @Test
    fun `연결이 끊기면 글은 그대로 두고 다시 보내라고 알린다`() = runTest {
        val vm = ContactViewModel(FakeApi(fail = true))
        vm.onMessage("안녕")
        vm.send()
        advanceUntilIdle()
        assertFalse(vm.state.value.sent)
        assertEquals("안녕", vm.state.value.message)
        assertEquals("서버에 연결할 수 없어요. 잠시 뒤 다시 보내 주세요", vm.state.value.error)
    }
}
