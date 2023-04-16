package types.auth

import types.user.HashedPassword
import types.user.RawPassword

interface PasswordHasher {
    /** Takes a raw password and hashes it. */
    fun hash(rawPassword: RawPassword): HashedPassword

    /** Checks if a raw password matches a hashed password */
    fun validate(rawPassword: RawPassword, password: HashedPassword): Boolean
}