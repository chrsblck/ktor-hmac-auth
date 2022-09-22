package com.chrsblck.hmacauth

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.bouncycastle.util.encoders.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SignatureVerificationFailedException(msg: String) : Exception(msg)

const val HmacSha256AuthKey = "HmacSha256"

class HmacSha256AuthenticationProvider internal constructor(
    config: Config
) : AuthenticationProvider(config) {
    private val headerName = config.headerName
    private val signingKey = config.signingKey
    private val onUnauthorized = config.ohUnauthorizedFunction

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val incomingSignature = call.request.header(headerName)
        val data = call.receive<ByteArray>()

        val cause = when (incomingSignature) {
            null -> Pair(
                AuthenticationFailedCause.NoCredentials,
                SignatureVerificationFailedException("$headerName was missing or empty")
            )

            else -> verifySignature(data, signingKey, incomingSignature).fold(
                { null },
                {
                    Pair(AuthenticationFailedCause.InvalidCredentials, it)
                }
            )
        }

        if (cause != null) {
            context.challenge(HmacSha256AuthKey, cause.first) { challenge, _ ->
                call.respond(HttpStatusCode.Unauthorized)
                onUnauthorized(cause.second)
                challenge.complete()
            }
        }
    }

    private fun verifySignature(
        data: ByteArray,
        signingKey: String,
        incomingSignature: String,
    ): Result<Unit> = runCatching {
        val calculatedSignature = hmacEncode(signingKey, data)
        if (incomingSignature != calculatedSignature) {
            throw SignatureVerificationFailedException("Incoming: $incomingSignature, Calculated: $calculatedSignature")
        }
    }

    private fun hmacEncode(key: String, data: ByteArray): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(secretKey)
        return Hex.toHexString(mac.doFinal(data))
    }

    class Config internal constructor(name: String?) : AuthenticationProvider.Config(name) {
        var headerName: String = "X-Signature"
        var signingKey: String = ""
        var ohUnauthorizedFunction: suspend (Throwable) -> Unit = { }

        fun onUnauthorized(body: suspend (Throwable) -> Unit) {
            ohUnauthorizedFunction = body
        }
    }
}

fun AuthenticationConfig.hmacSha256(
    name: String? = null,
    configure: HmacSha256AuthenticationProvider.Config.() -> Unit
) {
    val provider = HmacSha256AuthenticationProvider(
        HmacSha256AuthenticationProvider.Config(name).apply(configure)
    )
    register(provider)
}