package com.nowstation.app.core

import com.nowstation.app.core.ui.lineSymbol
import org.junit.Assert.assertEquals
import org.junit.Test

class LineSymbolTest {
    @Test
    fun `호선은 숫자, 그 밖의 노선은 첫 글자`() {
        assertEquals("2", lineSymbol("2호선"))
        assertEquals("9", lineSymbol("9호선"))
        assertEquals("수", lineSymbol("수인분당선"))
        assertEquals("신", lineSymbol("신분당선"))
    }
}
