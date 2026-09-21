package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Helper utility for capturing photos from the device camera, picking images
 * from the system photo gallery, compressing them to prevent giant files,
 * and loading them seamlessly with Coil in Jetpack Compose.
 */
object ProfileImageHelper {

    private const val FILE_PROVIDER_AUTHORITY = "com.aistudio.dutrachat.qxmp.fileprovider"

    /**
     * Creates a temporary file in cacheDir and returns its content Uri via FileProvider.
     */
    fun createTempPictureUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File.createTempFile(
            "camera_capture_${System.currentTimeMillis()}",
            ".jpg",
            imagesDir
        )
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, tempFile)
    }

    /**
     * Reads the source image URI, downsamples and rotates it based on Exif metadata,
     * compresses it into JPEG (quality 85%), and saves it to app-internal storage.
     */
    suspend fun compressAndSaveImage(
        context: Context,
        sourceUri: Uri,
        isCover: Boolean = false
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Measure dimensions without loading the entire bitmap into memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return@withContext null

            val targetMaxDimension = if (isCover) 1400 else 800
            var inSampleSize = 1
            val maxOriginal = max(originalWidth, originalHeight)
            while (maxOriginal / (inSampleSize * 2) >= targetMaxDimension) {
                inSampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return@withContext null

            // 3. Check EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (_: Exception) {
                // Ignore exif parsing issues
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(
                    sampledBitmap,
                    0,
                    0,
                    sampledBitmap.width,
                    sampledBitmap.height,
                    matrix,
                    true
                ).also {
                    if (it != sampledBitmap) sampledBitmap.recycle()
                }
            } else {
                sampledBitmap
            }

            // 4. Save to persistent app files directory
            val profileDir = File(context.filesDir, "profile").apply {
                if (!exists()) mkdirs()
            }
            val prefix = if (isCover) "cover" else "avatar"
            val targetFile = File(profileDir, "${prefix}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(targetFile).use { outStream ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                outStream.flush()
            }
            finalBitmap.recycle()

            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Controller holder for profile image actions.
 */
class ProfileImagePickerController(
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit,
    val showSourceSelector: () -> Unit,
    val hideSourceSelector: () -> Unit,
    val isSelectorVisible: Boolean
)

/**
 * Composable state hook that registers the Camera and Gallery activity result contracts,
 * handles camera runtime permissions gracefully, and performs background image compression.
 */
@Composable
fun rememberProfileImagePicker(
    isCover: Boolean = false,
    onImageProcessed: (Uri) -> Unit,
    onRemoveImage: (() -> Unit)? = null
): ProfileImagePickerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Gallery Picker launcher (uses modern Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ProfileImageHelper.compressAndSaveImage(context, uri, isCover)
                if (savedUri != null) {
                    onImageProcessed(savedUri)
                } else {
                    Toast.makeText(context, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera Capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = tempCameraUri
        if (success && uri != null) {
            scope.launch {
                val savedUri = ProfileImageHelper.compressAndSaveImage(context, uri, isCover)
                if (savedUri != null) {
                    onImageProcessed(savedUri)
                } else {
                    Toast.makeText(context, "Erro ao salvar foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = ProfileImageHelper.createTempPictureUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                "Permissão de câmera necessária para tirar fotos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val launchCamera = {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val uri = ProfileImageHelper.createTempPictureUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchGallery = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    return remember(showBottomSheet) {
        ProfileImagePickerController(
            launchCamera = launchCamera,
            launchGallery = launchGallery,
            showSourceSelector = { showBottomSheet = true },
            hideSourceSelector = { showBottomSheet = false },
            isSelectorVisible = showBottomSheet
        )
    }
}

/**
 * Bottom Sheet dialog for picking Camera or Gallery or removing photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileImageSelectorSheet(
    title: String = "Escolher Imagem",
    hasExistingImage: Boolean = false,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("image_source_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Camera Option
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismissRequest()
                        onCameraClick()
                    }
                    .testTag("action_camera")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Tirar foto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Tirar foto com a câmera",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Usar a câmera do celular agora",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gallery Option
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismissRequest()
                        onGalleryClick()
                    }
                    .testTag("action_gallery")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Escolher da galeria",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Escolher da galeria",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Selecionar foto armazenada no aparelho",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (hasExistingImage && onRemoveClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                // Remove photo
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismissRequest()
                            onRemoveClick()
                        }
                        .testTag("action_remove_photo")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remover foto",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Remover foto",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Voltar ao avatar padrão",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity profile avatar with Coil image loading, initials placeholder,
 * and optional camera edit badge overlay.
 */
@Composable
fun ProfileAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showEditBadge: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "D" }

    Box(
        modifier = modifier
            .size(size)
            .testTag("profile_avatar_box"),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto de perfil de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(size * 0.4f)
                        )
                    }
                },
                error = {
                    DefaultInitialsAvatar(initials = initials, size = size)
                }
            )
        } else {
            DefaultInitialsAvatar(initials = initials, size = size)
        }

        // Camera edit icon badge overlay
        if (showEditBadge && onEditClick != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomEnd)
                    .clickable { onEditClick() }
                    .testTag("avatar_edit_badge")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Editar foto de perfil",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size * 0.2f)
                    )
                }
            }
        }
    }
}

/**
 * Fallback avatar with gradient and user initials.
 */
@Composable
fun DefaultInitialsAvatar(
    initials: String,
    size: Dp
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(gradient)
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

/**
 * Profile cover image loader with Coil and edit overlay.
 */
@Composable
fun ProfileCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    showEditBadge: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("profile_cover_box")
    ) {
        if (!coverUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto de capa do perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                },
                error = {
                    DefaultCoverGradient()
                }
            )
        } else {
            DefaultCoverGradient()
        }

        if (showEditBadge && onEditClick != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clickable { onEditClick() }
                    .testTag("cover_edit_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Alterar capa",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alterar Capa",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultCoverGradient() {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1B4B),
            Color(0xFF312E81),
            Color(0xFF4338CA)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    )
}
