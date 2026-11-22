package io.github.grupo6.capturaprospectos.data.remote

data class SyncCaptureRequestDto(val deviceId:String,val ownerId:String,val sourceRevision:Long,val expectedRevision:Long,
    val registration:CreateProspectRequestDto,val state:String,val reason:String?,val snapshotJson:String) {
    override fun toString()="SyncCaptureRequestDto(payload=redacted)"
}
data class SyncCaptureAckDto(val localCaptureId:String,val remoteId:String,val sourceRevision:Long,val deviceId:String)
