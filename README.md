
Simple authentication library to add session authentication to http4k applications. Requires the Arrow library.


## Usage


### User and UserBuilder
First, we need to plug in the right types for everything to work. Our user model must extend `UserEntity`. 
```kotlin
import auth4k.types.user.*

data class User : UserEntity(
	override val userId: UserId?,
	override val userDetails: UserDetails?,
	override val userCredentials: HashedUserCredentials?
)
```

Unfortunately, because of some limitations, we also need to implement `UserBuilder`:
```kotlin
object MyUserBuilder : UserBuilder<User> {
	override fun addId(user: User, userId: UserId): User =  
		User(userId, user.userDetails, user.userCredentials)  
  
	override fun addCredentials(user: User, credentials: HashedUserCredentials): User =  
		User(user.userId, user.userDetails, credentials)  
  
	override fun addDetails(user: User, details: UserDetails): User =  
		User(user.userId, details, user.userCredentials)
}

```
Above is the most simple implementation of it, but you may adjust it as your user model changes. The user builder is used during registration to put together our user, before it has been given an ID and the password has been hashed.

### Authentication

The `Authentication` class contains most of the auth boilerplate. We need to create an instance of this class, providing it with the following:
- A password hasher, which by default is `DefaultBcryptHasher`.
- Our user builder, created in the previous step.
- A function to find an user given their login key. The login key is simply their username or email, or, if you choose, any other string.

```kotlin
import auth4k.*

val auth = Authentication(
	userBuilder = MyUserBuilder
) { loginKey ->
	myDatabase.getUserByEmail(loginKey)
}
```

### Auth Filter

Once we have our authentication class finished, we can finally plug it into the authentication filter:
```kotlin

val authFilter = AuthFilters.sessionAuth(
	auth = auth,
	exemptRoutes = setOf(),
	getUserBySession = { 
		session -> mySessionDb.getUser(session)?.right() ?: 
			SessionException.UserNotFound(session).left() 
	}
)

authFilter.then(myRoutes)

```

Our authentication filter takes the following arguments:
- auth - Which `Authentication` implementation to use
- redirectRoute - Which route to send unauthenticated users to
- cookieFactory - Already set by default, configures cookies
- exemptRoutes - Exempt routes from authentication, such as your login or landing page routes
- getUserBySession - Tells the authentication filter how to find users in the session database
- onLoginResponse - Additional code to be run on successful authentication

### Route implementations

Users must be able to log in or register. Our `Authentication` implementation provides us with methods to do this:

```kotlin
val myRoutes = routes(
	"/login" bind POST to {
		// use a JSON library of your choice here to get the user credentials
		// here we will assume we already have them:
		val userCredentials: RawUserCredentials = req.body.toUserCredentials()

		auth.login(userCredentials) { session, userId ->
			// We must tell the login function how to add our user to the session database on authentication success. How this is done depends on your database and the libraries used for it.
			mySessionDb.addSession(session, userId)
		}
		
		Response(OK)
	},

	"/register" bind POST to { req ->
		// use a JSON library of your choice here to get the user credentials
		// here we will assume we already have them:
		val userCredentials: RawUserCredentials = req.body.toUserCredentials()

		auth.register(
			// Unfortunately, because of the same limitations that caused UserBuilder to be necessary, we must do the following:
			User(null, myUserDetails, null),
			userCredentials
		) { user ->
			val userId = myDatabase.createUser(user)
			// We must also give our new user their user ID, which should be done in the database, but if not:
			myDatabase.updateUserId(MyUserBuilder.addId(user, userId))
			
			userId
		}.fold(
			{Response(INTERNAL_SERVER_ERROR)},
			{Response(OK)}
		)
	}
)
```

Don't forget to add these routes to the `exemptRoutes` set:
`exemptRoutes = listOf("/login", "/register")`

