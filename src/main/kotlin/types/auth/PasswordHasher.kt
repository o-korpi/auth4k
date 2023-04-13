package types.auth

interface PasswordHasher {
    /** Takes a raw password and hashes it. */
    fun hash(rawPassword: String): String

    /** */

    /** Checks if a raw password matches a hashed password */
    fun validate(rawPassword: String, password: String): Boolean
}