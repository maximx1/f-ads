package com.maximx1.fads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.maximx1.fads.core.model.Suit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.felt))
                    .safeDrawingPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = Suit.entries.joinToString(" ") { it.symbol.toString() },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
