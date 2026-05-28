package com.noobexon.xposedfakelocation

import com.noobexon.xposedfakelocation.manager.localization.LanguageOption
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageOptionTest {
    @Test
    fun `matches stored language tags to options`() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromTag(""))
        assertEquals(LanguageOption.ENGLISH, LanguageOption.fromTag("en"))
        assertEquals(LanguageOption.CHINESE, LanguageOption.fromTag("zh-CN"))
    }

    @Test
    fun `falls back to system for unknown language tags`() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromTag("fr"))
    }
}
