package io.github.grupo6.capturaprospectos.ui.access

import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.data.local.DemoSeed

/** Doble explícito para probar importación Room; no acredita comunicación con servidor. */
internal class DeviceAccountRemote : AccountRemote {
    private val account=UserSummaryDto(DemoSeed.id("supervisor"),"supervisor","supervisor@g6.example","Supervisor ficticio",
        "ACTIVE","SUPERVISOR",listOf("site-1","site-2"),0,"2026-11-21T12:00:00Z","2026-11-21T12:00:00Z")
    override suspend fun login(username:String,password:String):TokenResponseDto {
        if(password=="incorrecta") throw AccountApiException("INVALID_CREDENTIALS",401)
        return TokenResponseDto("TOKEN-DE-PRUEBA",userId=account.id,username=account.username,email=account.email,displayName=account.displayName,rol="SUPERVISOR",roles=listOf("SUPERVISOR"),siteIds=account.siteIds)
    }
    override suspend fun me()=account
    override suspend fun register(request:RegisterRequestDto):RegistrationReceiptDto=error("Not used")
    override suspend fun users(status:String,page:Int):UserPageDto=error("Not used")
    override suspend fun user(id:String):UserSummaryDto=error("Not used")
    override suspend fun sites():List<ManagedSiteDto> = error("Not used")
    override suspend fun update(id:String,request:AccountAccessRequestDto):UserSummaryDto=error("Not used")
    override suspend fun logout() {}
}
