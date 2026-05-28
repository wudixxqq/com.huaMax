package com.noobexon.xposedfakelocation

import com.noobexon.xposedfakelocation.xposed.HookMessages
import org.junit.Assert.assertEquals
import org.junit.Test

class HookMessagesTest {
    @Test
    fun `returns localized fake location toast message`() {
        assertEquals("Fake Location Is Active!", HookMessages.fakeLocationActive(""))
        assertEquals("Fake Location Is Active!", HookMessages.fakeLocationActive("en"))
        assertEquals("虚拟位置已启用！", HookMessages.fakeLocationActive("zh-CN"))
        assertEquals("虚拟位置已启用！", HookMessages.fakeLocationActive("zh-Hans-CN"))
    }
}
