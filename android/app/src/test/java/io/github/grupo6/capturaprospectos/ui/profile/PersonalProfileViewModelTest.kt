package io.github.grupo6.capturaprospectos.ui.profile

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.profile.*
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalProfileViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private val owners=ViewModelStore()
    private val a=UUID(0,1).toString();private val b=UUID(0,2).toString()
    private var active:String?=a
    private val jpeg=byteArrayOf(0xff.toByte(),0xd8.toByte(),1,0xff.toByte(),0xd9.toByte())
    private val data=mutableMapOf<String,PersonalProfile>()
    private var loadError=false;private var saveError=false
    private var writeGate:CompletableDeferred<Unit>?=null
    private val repository=object:PersonalProfileStore {
        override suspend fun load(accountId:String):PersonalProfile {if(loadError) throw IOException("test");return data[accountId] ?: PersonalProfile()}
        override suspend fun nickname(accountId:String,value:String):PersonalProfile {
            writeGate?.let {withContext(NonCancellable) {it.await()}}
            if(saveError) throw IOException("test")
            return (data[accountId] ?: PersonalProfile()).copy(nickname=value).also {data[accountId]=it}
        }
        override suspend fun photo(accountId:String,jpeg:ByteArray?):PersonalProfile {
            if(saveError) throw IOException("test")
            return (data[accountId] ?: PersonalProfile()).copy(photoJpeg=jpeg).also {data[accountId]=it}
        }
    }
    private fun model()=PersonalProfileViewModel(repository,dispatcher) {it==active}.also {owners.put(UUID.randomUUID().toString(),it)}
    private fun tick()=dispatcher.scheduler.runCurrent()
    private fun open(vm:PersonalProfileViewModel,id:String?=a) {active=id;vm.openFor(id);tick()}
    @Before fun setup() {Dispatchers.setMain(dispatcher)}
    @After fun cleanup() {owners.clear();tick();Dispatchers.resetMain()}
    @Test fun nicknameIsTrimmedEmptyRestoresNameAndUnicodeLimitUsesCharacters() {
        val vm=model();open(vm);vm.editNickname("  Apodo ficticio  ");vm.saveNickname();tick()
        assertEquals("Apodo ficticio",data[a]?.nickname)
        vm.editNickname("😀".repeat(40));vm.saveNickname();tick();assertEquals("😀".repeat(40),data[a]?.nickname)
        vm.editNickname("😀".repeat(41));vm.saveNickname();tick();assertNotNull(vm.uiState.value.error);assertEquals("😀".repeat(40),data[a]?.nickname)
        vm.editNickname("A\nB");vm.saveNickname();tick();assertNotNull(vm.uiState.value.error)
        vm.editNickname("");vm.saveNickname();tick();assertEquals("",data[a]?.nickname)
    }
    @Test fun photoCancelAndFailedImportPreserveSavedPhotoAndNickname() {
        data[a]=PersonalProfile("Alias",jpeg);val vm=model();open(vm)
        val token=vm.beginMedia()!!;assertNull(vm.beginMedia());vm.cancelMedia(token)
        assertArrayEquals(jpeg,vm.uiState.value.data.photoJpeg)
        val next=vm.beginMedia()!!;vm.receivePhoto(next) {throw IOException("image unavailable")};tick()
        assertEquals("Alias",data[a]?.nickname);assertArrayEquals(jpeg,data[a]?.photoJpeg);assertNotNull(vm.uiState.value.error)
    }
    @Test fun savedPersonalizationSurvivesLogoutAndNewOwnerWithoutCrossingAccounts() {
        val vm=model();open(vm);vm.editNickname("Alias A");vm.saveNickname();tick()
        vm.receivePhoto(vm.beginMedia()!!) {jpeg};tick();open(vm,null)
        assertNull(vm.uiState.value.ownerId);assertNull(vm.uiState.value.data.photoJpeg)
        open(vm,b);assertEquals("",vm.uiState.value.data.nickname);assertNull(vm.uiState.value.data.photoJpeg)
        open(model());val fresh=model();open(fresh)
        assertEquals("Alias A",fresh.uiState.value.data.nickname);assertArrayEquals(jpeg,fresh.uiState.value.data.photoJpeg)
    }
    @Test fun resultForPreviousAccountIsIgnoredBeforeDecoding() {
        val vm=model();open(vm);val token=vm.beginMedia()!!;open(vm,b)
        vm.receivePhoto(token) {fail("Stale selection must not be read");jpeg};tick()
        assertNull(data[a]);assertNull(data[b]);assertEquals(b,vm.uiState.value.ownerId)
    }
    @Test fun accountChangeWhileDecodingCannotSaveToNewAccount() {
        val vm=model();open(vm);val gate=CompletableDeferred<Unit>()
        vm.receivePhoto(vm.beginMedia()!!) {gate.await();jpeg};tick()
        open(vm,b);gate.complete(Unit);tick()
        assertNull(data[a]);assertNull(data[b]);assertEquals(b,vm.uiState.value.ownerId)
    }
    @Test fun alreadyStartedSaveRemainsBoundToOriginalAccount() {
        val vm=model();open(vm);writeGate=CompletableDeferred()
        vm.editNickname("Alias A");vm.saveNickname();tick();open(vm,b)
        writeGate!!.complete(Unit);tick()
        assertEquals("Alias A",data[a]?.nickname);assertNull(data[b]);assertEquals("",vm.uiState.value.data.nickname)
    }
    @Test fun readOrWriteFailuresDoNotResetStoredData() {
        data[a]=PersonalProfile("Alias",jpeg);val vm=model();loadError=true;open(vm)
        assertFalse(vm.uiState.value.ready);assertNotNull(vm.uiState.value.error)
        loadError=false;vm.retry();tick();assertTrue(vm.uiState.value.ready)
        saveError=true;vm.editNickname("Otro alias");vm.saveNickname();tick()
        assertEquals("Alias",vm.uiState.value.data.nickname);assertArrayEquals(jpeg,data[a]?.photoJpeg)
    }
    @Test fun removalPreservesNicknameAndDoesNotSaveAnUnsubmittedDraft() {
        data[a]=PersonalProfile("Alias guardado",jpeg);val vm=model();open(vm)
        vm.editNickname("Borrador sin guardar");vm.removePhoto();tick()
        assertNull(data[a]?.photoJpeg);assertEquals("Alias guardado",data[a]?.nickname)
        assertEquals("Borrador sin guardar",vm.uiState.value.nicknameDraft)
    }
    @Test fun processDeathDiscardsPendingTicketAndSessionRevocationBlocksWrites() {
        val vm=model();open(vm);val token=vm.beginMedia()!!;owners.clear();tick()
        val fresh=model();open(fresh);fresh.receivePhoto(token) {fail("Ticket lost on process death");jpeg};tick()
        active=null;fresh.editNickname("No autorizado");fresh.saveNickname();assertNull(fresh.beginMedia());tick()
        assertTrue(data.isEmpty())
    }
}
