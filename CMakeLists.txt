cmake_minimum_required(VERSION 3.2.0)
project(PluginCommsQrWifiJava LANGUAGES CXX)

set(BUILD_VERSION "" CACHE STRING "Version of the plugin being built")
if("${BUILD_VERSION}" STREQUAL "")
    message(SEND_ERROR "Missing or empty BUILD_VERSION argument")
endif()

set(CMAKE_BUILD_TYPE Debug)
set(CMAKE_VERBOSE_MAKEFILE ON)

add_subdirectory(loader/source)
add_subdirectory(plugin)

if(NOT ANDROID)
    enable_testing()
    add_subdirectory(test/loader-test/source EXCLUDE_FROM_ALL)
endif()