package auth4k.types.user

sealed interface Password {
    val value: String
}

@JvmInline
value class RawPassword(override val value: String) : Password

@JvmInline
value class HashedPassword(override val value: String) : Password
