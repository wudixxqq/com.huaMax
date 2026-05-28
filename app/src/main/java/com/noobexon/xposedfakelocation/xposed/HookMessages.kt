package com.noobexon.xposedfakelocation.xposed

object HookMessages {
    fun fakeLocationActive(languageTag: String): String {
        return if (languageTag.startsWith("zh", ignoreCase = true)) {
            "虚拟位置已启用！"
        } else {
            "Fake Location Is Active!"
        }
    }
}
