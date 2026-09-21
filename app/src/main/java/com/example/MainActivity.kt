package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ProfileAvatar
import com.example.util.ProfileCoverImage
import com.example.util.ProfileImageSelectorSheet
import com.example.util.rememberProfileImagePicker

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          DutraProfileScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutraProfileScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var avatarUri by remember { mutableStateOf<String?>(null) }
  var coverUri by remember { mutableStateOf<String?>(null) }
  var userName by remember { mutableStateOf("Alice Dutra") }
  val userCode = "81"
  var userBio by remember { mutableStateOf("Conectado ao Dutra Chat. Fale comigo pelo código 81!") }

  // Avatar Picker Controller
  val avatarPickerController = rememberProfileImagePicker(
    isCover = false,
    onImageProcessed = { uri: Uri ->
      avatarUri = uri.toString()
      Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
    },
    onRemoveImage = {
      avatarUri = null
      Toast.makeText(context, "Foto removida", Toast.LENGTH_SHORT).show()
    }
  )

  // Cover Picker Controller
  val coverPickerController = rememberProfileImagePicker(
    isCover = true,
    onImageProcessed = { uri: Uri ->
      coverUri = uri.toString()
      Toast.makeText(context, "Foto de capa atualizada!", Toast.LENGTH_SHORT).show()
    },
    onRemoveImage = {
      coverUri = null
      Toast.makeText(context, "Capa removida", Toast.LENGTH_SHORT).show()
    }
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
  ) {
    // Cover Image Header with Coil & edit button
    Box(modifier = Modifier.fillMaxWidth()) {
      ProfileCoverImage(
        coverUrl = coverUri,
        height = 180.dp,
        showEditBadge = true,
        onEditClick = { coverPickerController.showSourceSelector() }
      )

      // Top Header Overlay Bar
      Surface(
        color = Color.Black.copy(alpha = 0.35f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .clip(RoundedCornerShape(12.dp))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = "DUTRA CHAT",
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.titleMedium
          )

          Surface(
            shape = CircleShape,
            color = Color(0xFF10B981),
            modifier = Modifier.padding(end = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(Color.White, CircleShape)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "ONLINE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Avatar + User Code Row with overlap
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .offset(y = (-45).dp)
    ) {
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Profile Avatar with Coil & Camera Badge
        ProfileAvatar(
          imageUrl = avatarUri,
          name = userName,
          size = 96.dp,
          showEditBadge = true,
          onEditClick = { avatarPickerController.showSourceSelector() }
        )

        // Code Badge Box
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.testTag("user_code_card")
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
          ) {
            Text(
              text = "SEU CÓDIGO",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
              text = userCode,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // Profile Details & Actions
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .offset(y = (-30).dp)
    ) {
      Text(
        text = userName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = userBio,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Share & Copy Code Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Código Dutra Chat", userCode))
            Toast.makeText(context, "Código $userCode copiado com sucesso!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("button_copy_code")
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copiar código",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Copiar Código", fontWeight = FontWeight.SemiBold)
        }

        FilledTonalButton(
          onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(
                Intent.EXTRA_TEXT,
                "Olá! Vamos conversar pelo DUTRA CHAT. Meu código é $userCode. Baixe o aplicativo e fale comigo: https://dutra.chat/u/$userCode"
              )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar meu código Dutra Chat"))
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("button_share_code")
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Compartilhar",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Compartilhar", fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Card: Quick Photo Upload Options
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Personalizar Foto de Perfil",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Capture uma nova foto usando a câmera do aparelho ou escolha uma imagem direto da galeria. As imagens são comprimidas automaticamente para máxima performance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = { avatarPickerController.launchCamera() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_quick_camera")
            ) {
              Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Câmera")
            }

            OutlinedButton(
              onClick = { avatarPickerController.launchGallery() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_quick_gallery")
            ) {
              Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Galeria")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Card: User Concept Highlight
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Como funciona o Dutra Chat?",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Você não precisa procurar pelo nome nem cadastrar número de telefone. Para iniciar uma conversa com qualquer pessoa, basta informar o código público exclusivo dela!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
          }
        }
      }
    }
  }

  // Avatar Modal Bottom Sheet
  if (avatarPickerController.isSelectorVisible) {
    ProfileImageSelectorSheet(
      title = "Foto de Perfil",
      hasExistingImage = avatarUri != null,
      onDismissRequest = { avatarPickerController.hideSourceSelector() },
      onCameraClick = { avatarPickerController.launchCamera() },
      onGalleryClick = { avatarPickerController.launchGallery() },
      onRemoveClick = {
        avatarUri = null
        Toast.makeText(context, "Foto de perfil removida", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Cover Modal Bottom Sheet
  if (coverPickerController.isSelectorVisible) {
    ProfileImageSelectorSheet(
      title = "Foto de Capa",
      hasExistingImage = coverUri != null,
      onDismissRequest = { coverPickerController.hideSourceSelector() },
      onCameraClick = { coverPickerController.launchCamera() },
      onGalleryClick = { coverPickerController.launchGallery() },
      onRemoveClick = {
        coverUri = null
        Toast.makeText(context, "Foto de capa removida", Toast.LENGTH_SHORT).show()
      }
    )
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}

