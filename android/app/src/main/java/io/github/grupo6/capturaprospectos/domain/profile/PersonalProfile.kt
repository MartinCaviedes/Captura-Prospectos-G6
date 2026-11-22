package io.github.grupo6.capturaprospectos.domain.profile

const val MAX_AVATAR_BYTES=128*1024
data class PersonalProfile(val nickname:String="",val photoJpeg:ByteArray?=null) {
    override fun toString()="PersonalProfile(content=redacted)"
}
fun validNickname(value:String)=value.codePointCount(0,value.length)<=40 && value.none(Char::isISOControl)
interface PersonalProfileStore {
    suspend fun load(accountId:String):PersonalProfile
    suspend fun nickname(accountId:String,value:String):PersonalProfile
    suspend fun photo(accountId:String,jpeg:ByteArray?):PersonalProfile
}
