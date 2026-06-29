# AzuraTime Fortification Plan (ISO-Compliance)

## Goal
Migrasi sistem keamanan dari "Static/Weak XOR" ke "Hardware-Backed AES-GCM 256-bit" untuk memenuhi standar ISO/IEC dan best practice OWASP.

## Phase 1: Cryptographic Engine Setup (Kotlin)
- [ ] Create `CryptoEngine.kt` in `core/security/`.
- [ ] Implement `KeyGenParameterSpec` with `StrongBox` / `TEE` support.
- [ ] Implement `encrypt(data)` and `decrypt(data)` using `AES/GCM/NoPadding`.
- [ ] Ensure `CryptoEngine` acts as the single source of truth for all encryption operations.

## Phase 2: Native Security Refactoring (C++/JNI)
- [ ] Remove `const char key[]` from `azura_model_guard.cpp`.
- [ ] Implement `explicit_bzero` or `memset` for all sensitive memory buffers to prevent memory remnants.
- [ ] Update JNI bridge in `ModelGuard.kt` to pass decrypted streams or session-based keys.
- [ ] Clean up unused parameters (e.g., `iso_key` removal).

## Phase 3: Integration & Validation
- [ ] Integrate `CryptoEngine` with `ModelGuard` for TFLite model decryption.
- [ ] Integrate `CryptoEngine` with `SecurityVault` for anti-tampering validation.
- [ ] Final audit using `strings` command on `.so` files to ensure no keys are visible.

## Standards Checklist
- [ ] ISO/IEC 18033-3 (Block Ciphers)
- [ ] CWE-244 (Memory Scrubbing)
- [ ] OWASP MASVS (Cryptographic Standards)
