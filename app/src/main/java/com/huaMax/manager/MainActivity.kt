package com.huaMax.manager

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.huaMax.manager.localization.LocaleController
import com.huaMax.manager.ui.navigation.AppNavGraph
import com.huaMax.manager.ui.theme.LocationMaxTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleController.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !hasRequiredFrameworkApis()) {
            showUnsupportedAndroidDialog()
            return
        }

        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        setContent {
            LocationMaxTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }

    private fun showUnsupportedAndroidDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsupported Android version")
            .setMessage("LocationMax requires Android 11 or newer.")
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .show()
    }

    private fun hasRequiredFrameworkApis(): Boolean =
        runCatching {
            android.view.View::class.java.getMethod(
                "setForceDarkAllowed",
                java.lang.Boolean.TYPE
            )
            android.view.Window::class.java.getMethod(
                "setDecorFitsSystemWindows",
                java.lang.Boolean.TYPE
            )
        }.isSuccess
}
