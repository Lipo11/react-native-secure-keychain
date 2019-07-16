cmake_minimum_required(VERSION 3.4.1)

find_library( log-lib log )

# --------------------------------------------------------------------------------------------------
#   SECURE KEYCHAIN
#
add_library(
    secure-keychain
    SHARED
    src/main/cpp/secure_keychain_jni.cpp
)

target_compile_options(
    secure-keychain
    PUBLIC
    -std=c++11
)

target_link_libraries(
    secure-keychain
    ${log-lib}
)