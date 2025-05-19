package pigeon.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.thoughtcrime.securesms.R

@Composable
fun PreLoader(modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    containerColor = Color.Black
  ) { paddingValues ->
    Image(
      painter = painterResource(id = R.drawable.mp02_splash_bg),
      contentDescription = null,
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentScale = ContentScale.Crop
    )
  }
}

@Preview
@Composable
fun PreLoaderPreview() {
  PreLoader()
}