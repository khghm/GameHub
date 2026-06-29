package com.gamehub.host.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "😀", "😂", "🤣", "😍", "😎", "🥳", "😢", "😡", "👍", "👎",
        "🔥", "💯", "❤️", "💔", "🎉", "🏆", "⚽", "🎮", "🕹️", "👾",
        "💬", "🤝", "🙏", "💪", "🧠", "👀", "🚀", "⭐", "🌟", "✨",
        "🍕", "☕", "🎵", "🎶", "📱", "💡", "🔑", "🗝️", "🎁", "💎",
        "🐶", "🐱", "🦊", "🐼", "🐨", "🦁", "🐸", "🐵", "🦄", "🐲",
        "🌈", "❄️", "☀️", "🌙", "⚡", "🌊", "🍀", "🌹", "🌸", "🌻"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Emoji", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.height(250.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(emojis) { emoji ->
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                onEmojiSelected(emoji)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}