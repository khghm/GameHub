package com.gamehub.host

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamehub.host.ui.navigation.NavGraph
import com.gamehub.host.ui.theme.GameHubTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameHubTheme {
                NavGraph()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as GameHubApp).powerHelper.register()
    }

    override fun onPause() {
        super.onPause()
        (application as GameHubApp).powerHelper.unregister()
    }
}