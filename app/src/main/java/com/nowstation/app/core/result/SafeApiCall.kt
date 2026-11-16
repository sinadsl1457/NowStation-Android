package com.nowstation.app.core.result

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/** 저장소의 모든 서버 호출을 한 곳에서 감싼다. 서버 오류 본문은 Spring ProblemDetail(detail 칸)이다. */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    AppResult.Error(
        when (e.code()) {
            400 -> AppError.BadRequest(problemDetail(e) ?: "잘못된 요청")
            404 -> AppError.NotFound
            else -> AppError.Server(e.code())
        },
    )
} catch (e: IOException) {
    AppResult.Error(AppError.Network)
} catch (e: SerializationException) {
    AppResult.Error(AppError.Unknown)
}

private fun problemDetail(e: HttpException): String? = try {
    e.response()?.errorBody()?.string()?.let { Json.parseToJsonElement(it).jsonObject["detail"]?.jsonPrimitive?.content }
} catch (e: Exception) {
    null
}
