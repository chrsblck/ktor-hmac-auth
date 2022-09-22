object Versions {
    const val ktor = "2.1.1"
    const val bouncyCastle = "1.69"
}

object Dependencies {
    object Ktor {
        const val serverAuth = "io.ktor:ktor-server-auth:${Versions.ktor}"
    }

    object BouncyCastle {
        const val bcprovJdk15On = "org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}"
    }
}