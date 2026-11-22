package io.github.grupo6.capturaprospectos.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.profile.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PersonalProfileState(val ownerId:String?=null,val ready:Boolean=false,val busy:Boolean=false,
    val mediaPending:Boolean=false,val data:PersonalProfile=PersonalProfile(),val nicknameDraft:String="",
    val error:String?=null,val message:String?=null) {
    override fun toString()="PersonalProfileState(content=redacted)"
}
class PersonalProfileViewModel(private val store:PersonalProfileStore,private val io:CoroutineDispatcher=Dispatchers.IO,
    private val isCurrent:(String)->Boolean={true}):ViewModel() {
    private val mutable=MutableStateFlow(PersonalProfileState())
    val uiState=mutable.asStateFlow()
    private var generation=0L
    private var work:Job?=null
    private data class Ticket(val token:String,val owner:String,val generation:Long)
    private var ticket:Ticket?=null
    private fun bound(owner:String,epoch:Long)=generation==epoch && mutable.value.ownerId==owner && isCurrent(owner)
    private fun writable()=mutable.value.let {it.ownerId!=null && it.ready && !it.busy && !it.mediaPending && isCurrent(it.ownerId)}
    fun openFor(ownerId:String?,force:Boolean=false) {
        if(ownerId==mutable.value.ownerId && !force) return
        generation++;work?.cancel();ticket=null
        mutable.value=PersonalProfileState(ownerId=ownerId,busy=ownerId!=null)
        if(ownerId==null) return
        val epoch=generation
        work=viewModelScope.launch {
            try {
                val value=withContext(io) {store.load(ownerId)}
                if(bound(ownerId,epoch)) mutable.value=PersonalProfileState(ownerId=ownerId,ready=true,data=value,nicknameDraft=value.nickname)
            } catch(cancelled:CancellationException) {throw cancelled}
            catch(_:Exception) {if(bound(ownerId,epoch)) mutable.update {it.copy(busy=false,error="No se pudo cargar tu personalización. Puedes reintentar.")}}
        }
    }
    fun retry()=openFor(mutable.value.ownerId,force=true)
    fun editNickname(value:String) {if(writable()) mutable.update {it.copy(nicknameDraft=value,error=null,message=null)}}
    fun saveNickname() {
        if(!writable()) return
        val value=mutable.value.nicknameDraft.trim()
        if(!validNickname(value)) {mutable.update {it.copy(error="El apodo admite hasta 40 caracteres y no puede contener controles.")};return}
        mutate("Apodo guardado.",nicknameSaved=true) {id ->store.nickname(id,value)}
    }
    fun removePhoto() {if(writable()) mutate("Foto eliminada.") {id ->store.photo(id,null)}}
    fun beginMedia():String? {
        if(!writable()) return null
        val owner=requireNotNull(mutable.value.ownerId)
        return UUID.randomUUID().toString().also {token ->
            ticket=Ticket(token,owner,generation)
            mutable.update {it.copy(mediaPending=true,error=null,message=null)}
        }
    }
    fun cancelMedia(token:String,message:String?=null) {
        val pending=ticket ?: return
        if(pending.token!=token || !bound(pending.owner,pending.generation)) return
        ticket=null;mutable.update {it.copy(mediaPending=false,error=message)}
    }
    fun receivePhoto(token:String,decode:suspend ()->ByteArray) {
        val pending=ticket ?: return
        if(pending.token!=token || !bound(pending.owner,pending.generation)) return
        ticket=null;mutable.update {it.copy(mediaPending=false)}
        mutate("Foto guardada.") {owner ->
            val bytes=withTimeout(15000) {decode()}
            require(bytes.size in 4..MAX_AVATAR_BYTES)
            if(!bound(pending.owner,pending.generation)) throw CancellationException("Account changed")
            store.photo(owner,bytes)
        }
    }
    private fun mutate(message:String,nicknameSaved:Boolean=false,save:suspend (String)->PersonalProfile) {
        if(!writable()) return
        val owner=requireNotNull(mutable.value.ownerId);val epoch=generation
        mutable.update {it.copy(busy=true,error=null,message=null)}
        work=viewModelScope.launch {
            try {
                val value=withContext(io) {save(owner)}
                if(bound(owner,epoch)) mutable.update {it.copy(busy=false,data=value,
                    nicknameDraft=if(nicknameSaved) value.nickname else it.nicknameDraft,message=message)}
            } catch(_:TimeoutCancellationException) {if(bound(owner,epoch)) mutable.update {it.copy(busy=false,error="La imagen tardó demasiado en abrirse. Intenta elegirla otra vez.")}}
            catch(cancelled:CancellationException) {throw cancelled}
            catch(_:Exception) {if(bound(owner,epoch)) mutable.update {it.copy(busy=false,error="No se pudo guardar el cambio. Se conserva la personalización anterior.")}}
        }
    }
    override fun onCleared() {generation++;work?.cancel();ticket=null;mutable.value=PersonalProfileState();super.onCleared()}
}
