package com.nowstation.app.settings

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** 서버 POST /api/feedback(앱 안 문의하기). */
interface FeedbackApi {
    @POST("api/feedback")
    suspend fun send(@Body request: FeedbackRequest)
}

@Serializable
data class FeedbackRequest(
    val message: String,
    val contact: String? = null,
    /** 기본값을 두지 않는다: kotlinx.serialization은 기본값인 칸을 보내지 않아 서버에 null로 갔다. */
    val platform: String,
    val appVersion: String,
    val device: String,
)
