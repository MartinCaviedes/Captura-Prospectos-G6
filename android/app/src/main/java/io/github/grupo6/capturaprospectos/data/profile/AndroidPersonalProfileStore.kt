package io.github.grupo6.capturaprospectos.data.profile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import com.google.gson.Gson
import io.github.grupo6.capturaprospectos.domain.profile.*
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidPersonalProfileStore(context:Context):PersonalProfileStore {
    private val directory=context.applicationContext.noBackupFilesDir
    private data class Stored(val schema:Int,val accountId:String,val nickname:String,val photo:String?)
    private fun id(value:String)=UUID.fromString(value).toString().also {require(it==value)}
    private fun file(id:String)=AtomicFile(File(directory,"personal-profile-$id.bin"))
    private fun key(id:String):SecretKey {
        val alias="g6.personal.profile.$id"
        val keys=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        (keys.getKey(alias,null) as? SecretKey)?.let {return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun checked(profile:PersonalProfile):PersonalProfile {
        require(validNickname(profile.nickname))
        profile.photoJpeg?.let {require(it.size in 4..MAX_AVATAR_BYTES && it[0]==0xff.toByte() && it[1]==0xd8.toByte() && it[it.size-2]==0xff.toByte() && it.last()==0xd9.toByte())}
        return profile
    }
    private fun read(id:String):PersonalProfile {
        val file=file(id)
        val backup=File(file.baseFile.path+".bak")
        if(!file.baseFile.exists() && !backup.exists()) return PersonalProfile()
        require(file.baseFile.length()<=300000 && backup.length()<=300000)
        val bytes=file.readFully();require(bytes.size in 29..300000)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(id),GCMParameterSpec(128,bytes.copyOfRange(0,12)));cipher.updateAAD(id.toByteArray())
        val stored=Gson().fromJson(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8),Stored::class.java)
        require(stored.schema==1 && stored.accountId==id)
        return checked(PersonalProfile(stored.nickname,stored.photo?.let {Base64.decode(it,Base64.NO_WRAP)}))
    }
    private fun write(id:String,profile:PersonalProfile):PersonalProfile {
        checked(profile)
        val stored=Stored(1,id,profile.nickname,profile.photoJpeg?.let {Base64.encodeToString(it,Base64.NO_WRAP)})
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(id));cipher.updateAAD(id.toByteArray())
        require(cipher.iv.size==12)
        val bytes=cipher.iv+cipher.doFinal(Gson().toJson(stored).toByteArray(Charsets.UTF_8));require(bytes.size<=300000)
        val file=file(id);val stream=file.startWrite()
        try {stream.write(bytes);file.finishWrite(stream)} catch(error:Exception) {file.failWrite(stream);throw error}
        return profile
    }
    override suspend fun load(accountId:String)=withContext(Dispatchers.IO) {mutex.withLock {read(id(accountId))}}
    override suspend fun nickname(accountId:String,value:String)=withContext(Dispatchers.IO) {mutex.withLock {
        val id=id(accountId);write(id,read(id).copy(nickname=value.trim()))
    }}
    override suspend fun photo(accountId:String,jpeg:ByteArray?)=withContext(Dispatchers.IO) {mutex.withLock {
        val id=id(accountId);write(id,read(id).copy(photoJpeg=jpeg))
    }}
    companion object {private val mutex=Mutex()}
}
