package types.user

data class UsernameUserCredentials(
    override val loginKey: Username,
    override val password: Password
) : UserCredentials
