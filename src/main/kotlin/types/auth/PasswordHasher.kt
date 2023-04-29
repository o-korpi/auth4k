package auth4k.types.auth

import auth4k.types.user.HashedPassword
import auth4k.types.user.RawPassword

interface PasswordHasher {
    /** Takes a raw password and hashes it. */
    fun hash(rawPassword: RawPassword): HashedPassword

    /** Checks if a raw password matches a hashed password */
    fun validate(rawPassword: RawPassword, password: HashedPassword): Boolean
}