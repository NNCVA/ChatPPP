package com.chatppp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chatppp.app.navigation.ChatPppNavGraph
import com.chatppp.app.ui.theme.ChatPppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatPppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatPppApp()
                }
            }
        }
    }
}

@Composable
private fun ChatPppApp() {
    ChatPppNavGraph()
}

@Preview(showBackground = true)
@Composable
private fun ChatPppAppPreview() {
    ChatPppTheme {
        ChatPppApp()
    }
}
