#!/usr/bin/env bash
set -e

# -----------------------------------------------------------------
# 1️⃣ Create the feature‑auth‑api directory tree
# -----------------------------------------------------------------
FEATURE_AUTH_API="feature-auth-api"
if [ ! -d "$FEATURE_AUTH_API" ]; then
    echo "Creating module directory: $FEATURE_AUTH_API"
    mkdir -p "$FEATURE_AUTH_API/src/main/java/com/azuratech/azuratime/feature/auth/api"
else
    echo "Directory $FEATURE_AUTH_API already exists – skipping."
fi

# -----------------------------------------------------------------
# 2️⃣ Write a minimal build.gradle.kts for the API module
# -----------------------------------------------------------------
FEATURE_BUILD_FILE="${FEATURE_AUTH_API}/build.gradle.kts"
if [ ! -f "$FEATURE_BUILD_FILE" ]; then
    cat > "$FEATURE_BUILD_FILE" <<'EOF'
plugins {
    kotlin("android")
}
repositories {
    google()
    mavenCentral()
}
dependencies {
    // API‑only deps – no heavy runtime libraries here.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
EOF
    echo "Created $FEATURE_BUILD_FILE"
else
    echo "$FEATURE_BUILD_FILE already exists – skipping."
fi

# -----------------------------------------------------------------
# 3️⃣ Append the module to settings.gradle.kts (proper include syntax)
# -----------------------------------------------------------------
SETTINGS_FILE="settings.gradle.kts"
if ! grep -Fxq "include(\":feature-auth-api\")" "$SETTINGS_FILE"; then
    echo "include(\":feature-auth-api\")" >> "$SETTINGS_FILE"
    echo "Appended 'include(\":feature-auth-api\")' to $SETTINGS_FILE"
else
    echo "' include(\":feature-auth-api\")' already present in $SETTINGS_FILE"
fi

# -----------------------------------------------------------------
# 4️⃣ Add the core API files (interfaces & DTOs)
# -----------------------------------------------------------------
PKG="com.azuratech.azuratime.feature.auth.api"
DIR="${FEATURE_AUTH_API}/src/main/java/${PKG//./\/}"
mkdir -p "$DIR"

# AuthService.kt
cat > "$DIR/AuthService.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("status")
    suspend fun getStatus(@Query("userId") userId: String): AuthStatusResponse
}
EOT

# LoginRequest.kt
cat > "$DIR/LoginRequest.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

data class LoginRequest(val email: String, val password: String)
EOT

# LoginResponse.kt
cat > "$DIR/LoginResponse.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

data class LoginResponse(val token: String, val expiresIn: Long)
EOT

# AuthStatus.kt
cat > "$DIR/AuthStatus.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

sealed interface AuthStatus
object LoggedIn : AuthStatus
object LoggedOut : AuthStatus
data class Error(val message: String) : AuthStatus
EOT

# AuthStatusResponse.kt
cat > "$DIR/AuthStatusResponse.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

data class AuthStatusResponse(val status: AuthStatus, val userId: String?)
EOT

# AuthState.kt
cat > "$DIR/AuthState.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

data class AuthState(val status: AuthStatus, val userId: String? = null)
EOT

# AuthRepository.kt
cat > "$DIR/AuthRepository.kt" <<'EOT'
package com.azuratech.azuratime.feature.auth.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineResult

interface AuthRepository {
    fun login(request: LoginRequest): CoroutineResult<AuthStatus>
    fun logout()
    fun observeStatus(): Flow<AuthState>
}
EOT

echo "API contract files created under $FEATURE_AUTH_API"

# -----------------------------------------------------------------
# 5️⃣ Run a quick assemble to ensure the module compiles
# -----------------------------------------------------------------
./gradlew ":${FEATURE_AUTH_API}:assembleDebug" -q

# -----------------------------------------------------------------
# 6️⃣ Show the generated AAR (if any)
# -----------------------------------------------------------------
if [ -d "${FEATURE_AUTH_API}/build/outputs/aar" ]; then
    echo "AAR generated at ${FEATURE_AUTH_API}/build/outputs/aar/"
    ls -l "${FEATURE_AUTH_API}/build/outputs/aar/"
fi

echo "All done."