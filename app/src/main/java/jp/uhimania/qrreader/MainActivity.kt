package jp.uhimania.qrreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import jp.uhimania.qrreader.ui.QRReaderNavGraph
import jp.uhimania.qrreader.ui.theme.QRReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRReaderTheme {
                QRReaderNavGraph()
            }
        }
    }
}
