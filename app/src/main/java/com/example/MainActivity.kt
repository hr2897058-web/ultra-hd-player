package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.navigation.UltraPlayerNavGraph
import com.example.ui.theme.DarkBg
import com.example.ui.theme.UltraPlayerTheme

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.entries.any { it.value }
        if (isGranted) {
            libraryViewModel.refreshMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestStoragePermissions()
        handleIntent(intent)

        setContent {
            UltraPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    UltraPlayerNavGraph(
                        libraryViewModel = libraryViewModel,
                        onVideoSelected = { video ->
                            Toast.makeText(
                                this,
                                "Selected: ${video.displayName}\nResolution: ${video.resolutionString} (${video.resolutionCategory.label})\nDecoder: ${video.recommendedDecoderMode}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                libraryViewModel.importSingleVideoUri(uri) { video ->
                    Toast.makeText(
                        this,
                        "Opened Video: ${video.displayName}\n${video.resolutionString} [${video.codec}]",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun requestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}

