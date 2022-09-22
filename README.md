# Ktor Hmac Auth Provider

## Example Usage

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication

data class Config(val signingKey: String)

const val ExampleAuth = "example"

fun Application.configureSecurity(config: Config) {
    install(Authentication) {
        hmacSha256(ExampleAuth) {
            headerName = "X-Example-Signature"
            signingKey = config.signingKey
            onUnauthorized { throw it }
        }
    }
}
```