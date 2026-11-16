package com.nowstation.app.core.result

/** 서버 호출 결과. 실패 종류는 화면 문구를 고르는 데 쓴다. */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    /** 서버에 닿지 못함(지하 끊김, 타임아웃). 잠시 뒤 다시 시도하면 된다. */
    data object Network : AppError
    data object NotFound : AppError
    data class BadRequest(val message: String) : AppError
    data class Server(val code: Int) : AppError
    data object Unknown : AppError
}
