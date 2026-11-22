package io.github.grupo6.capturaprospectos.domain.records

data class SyncIdentity(val id:String,val role:String,val siteIds:Set<String>)
data class SyncIssue(val captureId:String,val code:String,val label:String="Ficha")
data class SyncOverview(val pending:Int=0,val issues:List<SyncIssue> = emptyList(),val legacy:Int=0)
interface CaptureSynchronization {
    suspend fun overview(actorId:String):SyncOverview
    /** Devuelve código de sesión inválida; los demás errores quedan en la cola. */
    suspend fun drain(identity:SyncIdentity,stillAuthorized:()->Boolean):String?
}
