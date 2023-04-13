package types.user

interface LoginKey {
    val value: String
}


data class Email(override val value: String) : LoginKey {

}

@JvmInline
value class Username(override val value: String) : LoginKey
