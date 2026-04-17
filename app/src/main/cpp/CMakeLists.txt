cmake_minimum_required(VERSION 3.22.1)

# Nama project besar kita
project("azura_native_engine")

# 🔥 Tambahkan standar C++ agar fitur std::string dan vector berjalan mulus
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 1. Mesin ML (Urusan Wajah & Absensi)
add_library(
        azura_secure_engine
        SHARED
        azura_secure_engine.cpp)

# 2. Mesin Security (Urusan Enkripsi & Brankas)
add_library(
        azura_security_vault
        SHARED
        azura_security_vault.cpp)

# 🔥 3. Mesin Penjaga Model AI (BARU)
add_library(
        azura_model_guard
        SHARED
        azura_model_guard.cpp)        

# Mencari library log bawaan Android agar kita bisa pakai __android_log_print
find_library(log-lib log)

# Link masing-masing library ke log-lib
target_link_libraries(azura_secure_engine ${log-lib})
target_link_libraries(azura_security_vault ${log-lib})
# 🔥 Link library baru kita ke log
target_link_libraries(azura_model_guard ${log-lib})