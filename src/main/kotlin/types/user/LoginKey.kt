package types.user

interface LoginKey {
    val value: String
}


data class Email(override val value: String) : LoginKey {
    fun validate(): Boolean = value.contains('@')
}

@JvmInline
value class Username(override val value: String) : LoginKey
