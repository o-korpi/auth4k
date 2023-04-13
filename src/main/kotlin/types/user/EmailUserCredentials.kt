package types.user

data class EmailUserCredentials(
    override val loginKey: Email,
    override val password: Password
) : UserCredentials