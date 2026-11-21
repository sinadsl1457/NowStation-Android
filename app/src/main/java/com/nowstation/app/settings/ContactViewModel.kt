package com.nowstation.app.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nowstation.app.BuildConfig
import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.core.result.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactUiState(
    val message: String = "",
    val contact: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = message.isNotBlank() && !sending && (contact.isBlank() || EMAIL.matches(contact.trim()))
    val contactInvalid: Boolean get() = contact.isNotBlank() && !EMAIL.matches(contact.trim())

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

/** 앱 안 문의하기: 글과(선택) 답장 이메일을 서버로. 메일 앱을 열지 않는다. */
@HiltViewModel
class ContactViewModel @Inject constructor(private val api: FeedbackApi) : ViewModel() {
    private val _state = MutableStateFlow(ContactUiState())
    val state: StateFlow<ContactUiState> = _state.asStateFlow()

    fun onMessage(text: String) = _state.update { it.copy(message = text.take(MAX_MESSAGE), error = null) }
    fun onContact(text: String) = _state.update { it.copy(contact = text.take(200), error = null) }

    fun send() {
        val s = _state.value
        if (!s.canSend) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val request = FeedbackRequest(
                message = s.message.trim(),
                contact = s.contact.trim().ifEmpty { null },
                platform = "android",
                appVersion = BuildConfig.VERSION_NAME,
                device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
            )
            when (val result = safeApiCall { api.send(request) }) {
                is AppResult.Success -> _state.update { it.copy(sending = false, sent = true) }
                is AppResult.Error -> _state.update {
                    it.copy(sending = false, error = if (result.error == AppError.Network) "서버에 연결할 수 없어요. 잠시 뒤 다시 보내 주세요" else "보내지 못했어요. 잠시 뒤 다시 보내 주세요")
                }
            }
        }
    }

    companion object {
        const val MAX_MESSAGE = 2000
    }
}
