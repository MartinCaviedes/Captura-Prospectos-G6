package io.github.grupo6.capturaprospectos.ui.profile

import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.data.profile.AvatarImages
import io.github.grupo6.capturaprospectos.ui.admission.rememberDevicePermissionAction
import io.github.grupo6.capturaprospectos.ui.components.*
import kotlinx.coroutines.*

@Composable fun PersonalProfileRoute(model:PersonalProfileViewModel,accountId:String,officialName:String,email:String,role:String) {
    val state by model.uiState.collectAsStateWithLifecycle()
    if(state.ownerId!=accountId) return
    key(accountId) { PersonalProfileMedia(model,state,officialName,email,role) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PersonalProfileMedia(model:PersonalProfileViewModel,state:PersonalProfileState,officialName:String,email:String,role:String) {
    val context=LocalContext.current.applicationContext
    val activity=LocalContext.current.profileActivity()
    var galleryTicket by rememberSaveable {mutableStateOf<String?>(null)}
    var cameraTicket by rememberSaveable {mutableStateOf<String?>(null)}
    var showPhotoChoices by rememberSaveable {mutableStateOf(false)}
    DisposableEffect(model,activity) {
        onDispose {
            if(activity?.isChangingConfigurations!=true) {
                galleryTicket?.let {model.cancelMedia(it)}
                cameraTicket?.let {model.cancelMedia(it)}
            }
        }
    }
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {uri ->
        val token=galleryTicket;galleryTicket=null
        if(token!=null) {
            if(uri==null) model.cancelMedia(token)
            else model.receivePhoto(token) {runInterruptible {AvatarImages.selected(context,uri)}}
        }
    }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {bitmap ->
        val token=cameraTicket;cameraTicket=null
        if(token!=null) {
            if(bitmap==null) model.cancelMedia(token)
            else model.receivePhoto(token) {runInterruptible {AvatarImages.thumbnail(bitmap)}}
        }
    }
    val takePhoto=rememberDevicePermissionAction(camera=true,
        denialMessage="La cámara permite tomar una miniatura para tu perfil. Puedes elegir una imagen de galería o continuar sin foto.",
        dismissLabel="Continuar sin foto") {
        model.beginMedia()?.let {token ->
            cameraTicket=token
            try {camera.launch(null)}
            catch(_:ActivityNotFoundException) {cameraTicket=null;model.cancelMedia(token,"No hay una cámara disponible. Puedes elegir una imagen de galería.")}
            catch(_:SecurityException) {cameraTicket=null;model.cancelMedia(token,"No se pudo abrir la cámara con los permisos actuales.")}
        }
    }
    if(showPhotoChoices) ModalBottomSheet(onDismissRequest={showPhotoChoices=false}) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Cambiar foto",style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick={
                showPhotoChoices=false
                model.beginMedia()?.let {token ->
                    galleryTicket=token
                    try {gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}
                    catch(_:ActivityNotFoundException) {galleryTicket=null;model.cancelMedia(token,"No hay un selector de imágenes disponible.")}
                    catch(_:SecurityException) {galleryTicket=null;model.cancelMedia(token,"No se pudo abrir el selector de imágenes.")}
                }
            },modifier=Modifier.fillMaxWidth()) {Text("Elegir de galería")}
            OutlinedButton(onClick={showPhotoChoices=false;takePhoto()},modifier=Modifier.fillMaxWidth()) {Text("Tomar foto")}
            OutlinedButton(onClick={showPhotoChoices=false;model.removePhoto()},enabled=state.data.photoJpeg!=null,modifier=Modifier.fillMaxWidth()) {Text("Eliminar foto")}
            TextButton(onClick={showPhotoChoices=false}) {Text("Cancelar")}
        }
    }
    PersonalProfileCard(state,officialName,email,role,model::editNickname,model::saveNickname,
        onChangePhoto={showPhotoChoices=true},onRetry=model::retry)
}

private fun Context.profileActivity():Activity?=when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless {it===this}?.profileActivity()
    else -> null
}

@Composable fun PersonalProfileCard(state:PersonalProfileState,officialName:String,email:String,role:String,
    onNickname:(String)->Unit,onSave:()->Unit,onChangePhoto:()->Unit,onRetry:()->Unit) {
    val name=state.data.nickname.ifBlank {officialName}
    val initials=name.split(" ").filter(String::isNotBlank).take(2).joinToString("") {it.take(1)}.uppercase()
    val enabled=state.ready && !state.busy && !state.mediaPending
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(state.ownerId,initials,state.data.photoJpeg)
            Text(name,style=MaterialTheme.typography.headlineMedium)
            Text("Nombre de cuenta: $officialName",style=MaterialTheme.typography.bodyMedium)
            if(email.isNotBlank()) Text(email,style=MaterialTheme.typography.bodyMedium)
            Text("Rol: $role")
            Text("Apodo y foto se guardan para esta cuenta en este dispositivo.",style=MaterialTheme.typography.bodySmall)
            if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(state.mediaPending) Text("Esperando la selección de foto…",style=MaterialTheme.typography.bodySmall)
            state.error?.let {Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})}
            state.message?.let {Text(it,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})}
            if(!state.ready && !state.busy) OutlinedButton(onRetry) {Text("Reintentar personalización")}
            OutlinedTextField(state.nicknameDraft,onNickname,label={Text("Apodo")},singleLine=true,enabled=enabled,
                supportingText={Text("Máximo 40 caracteres. Vacío utiliza el nombre de cuenta.")},modifier=Modifier.fillMaxWidth())
            Button(onSave,enabled=enabled && state.nicknameDraft.trim()!=state.data.nickname,modifier=Modifier.fillMaxWidth()) {Text("Guardar apodo")}
            OutlinedButton(onChangePhoto,enabled=enabled,modifier=Modifier.fillMaxWidth()) {Text("Cambiar foto")}
        }
    }
}

@Composable fun ProfileAvatar(owner:String?,initials:String,bytes:ByteArray?,modifier:Modifier=Modifier.size(88.dp)) {
    var image by remember(owner,bytes) {mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(owner,bytes) {
        image=withContext(Dispatchers.Default) {bytes?.let {BitmapFactory.decodeByteArray(it,0,it.size)?.asImageBitmap()}}
    }
    image?.let {Image(it,"Foto de perfil",modifier.clip(CircleShape),contentScale=ContentScale.Crop)} ?: Avatar(initials,modifier)
}
