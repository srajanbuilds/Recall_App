cmake_minimum_required(VERSION 3.22.1)

project("recall-ai")

# Stub for TRD v2.0 JNI bridge
add_library(
        recall-ai
        SHARED
        llama-bridge.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        recall-ai
        ${log-lib})
