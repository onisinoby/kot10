package com.example.kotlin10

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.kotlin10.ui.theme.Kotlin10Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : ComponentActivity() {

    private val imageUrlState = mutableStateOf("")
    private val bitmapState = mutableStateOf<Bitmap?>(null)
    private val isLoadingState = mutableStateOf(false)
    private val messageState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Kotlin10Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ImageDownloaderScreen(
                        imageUrlState = imageUrlState,
                        bitmapState = bitmapState,
                        isLoadingState = isLoadingState,
                        messageState = messageState,
                        onDownloadClick = { downloadAndSaveImage() }
                    )
                }
            }
        }
    }

    private fun downloadAndSaveImage() {
        CoroutineScope(Dispatchers.Main).launch {
            isLoadingState.value = true
            messageState.value = null
            val bitmapDeferred = downloadImage(imageUrlState.value)
            val bitmap = bitmapDeferred.await()
            if (bitmap != null) {
                bitmapState.value = bitmap
                saveImageToDisk(bitmap)
                messageState.value = "Изображение сохранено"
            } else {
                messageState.value = "Ошибка загрузки изображения"
            }
            isLoadingState.value = false
        }
    }

    private fun downloadImage(imageUrl: String): Deferred<Bitmap?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val input = connection.getInputStream()
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveImageToDisk(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "downloaded_image.jpg"
                )
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                messageState.value = "Ошибка сохранения изображения"
            }
        }
    }
}

@Composable
fun ImageDownloaderScreen(
    imageUrlState: MutableState<String>,
    bitmapState: MutableState<Bitmap?>,
    isLoadingState: MutableState<Boolean>,
    messageState: MutableState<String?>,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = imageUrlState.value,
            onValueChange = { imageUrlState.value = it },
            label = { Text("Введите URL изображения") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDownloadClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Загрузить изображение")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoadingState.value) {
            CircularProgressIndicator()
        } else {
            bitmapState.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Загруженное изображение",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        messageState.value?.let {
            Text(text = it)
        }
    }
}