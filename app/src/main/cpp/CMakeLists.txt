cmake_minimum_required(VERSION 3.22.1)

project("callorderapp")

set(CMAKE_CXX_STANDARD 17)

# Absolute path to whisper source
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/../whisper.cpp)

# Add whisper library
add_subdirectory(${WHISPER_DIR} ${CMAKE_BINARY_DIR}/whisper_build)

# Native bridge
add_library(
        native-lib
        SHARED
        native-lib.cpp
)

# Tell compiler where whisper headers are
target_include_directories(
        native-lib
        PRIVATE
        ${WHISPER_DIR}
        ${WHISPER_DIR}/include
)

# Android log
find_library(
        log-lib
        log
)

# Link whisper and log
target_link_libraries(
        native-lib
        whisper
        ${log-lib}
)