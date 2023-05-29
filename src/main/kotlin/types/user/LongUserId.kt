package auth4k.types.user

import java.util.*


interface IdType

@JvmInline
value class LongUserId(val value: Long) : IdType

@JvmInline
value class IntUserid(val value: Int) : IdType

@JvmInline
value class UUIDUserId(val value: UUID) : IdType
