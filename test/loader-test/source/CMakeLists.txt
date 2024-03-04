cmake_minimum_required(VERSION 3.2.0)

project(PluginCommsQrWifiJavaLoaderTest)
set(CMAKE_BUILD_TYPE Debug)

set(CMAKE_VERBOSE_MAKEFILE ON)

set(CMAKE_CXX_STANDARD 17)

# Set warning flags for the compiler
include(../../common/warnings.cmake.txt)
link_directories("/usr/local/lib/race/comms/")

add_executable(unitTestPluginCommsJava
    main.cpp
)
target_compile_definitions(unitTestPluginCommsJava PRIVATE
    TEST_SOURCE_DIR="${CMAKE_CURRENT_SOURCE_DIR}/"
)

find_package(Threads REQUIRED)

target_link_libraries(unitTestPluginCommsJava
    PluginCommsQrWifiJava
    raceSdkCommon
    gtest
    gmock
    Threads::Threads
    dl
)

if (TARGET raceSdkTestMocks)
    target_link_libraries(unitTestPluginCommsJava raceSdkTestMocks)
endif()

get_target_property(PLUGIN_BINARY_DIR PluginCommsQrWifiJavaJar BINARY_DIR)
add_custom_command(
    COMMAND cp ${PLUGIN_BINARY_DIR}/target/plugin-comms-qrwifi-java-1.jar ${CMAKE_CURRENT_BINARY_DIR}/plugin-comms-qrwifi-java-1.jar
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/plugin-comms-qrwifi-java-1.jar
    DEPENDS PluginCommsQrWifiJavaJar
    COMMENT "Copying plugin jar..."
)
add_custom_target(CopyPluginJarForCommsJavaTest ALL
    DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/plugin-comms-qrwifi-java-1.jar)
add_dependencies(unitTestPluginCommsJava CopyPluginJarForCommsJavaTest)


if (TARGET RaceSdkJavaShimsJar)
    get_target_property(RACE_JAVA_SHIMS_JAR_BINARY_DIR RaceSdkJavaShimsJar BINARY_DIR)
    add_custom_command(
        COMMAND cp ${RACE_JAVA_SHIMS_JAR_BINARY_DIR}/racesdk-java-shims-1.jar ${CMAKE_CURRENT_BINARY_DIR}/racesdk-java-shims-1.jar
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/racesdk-java-shims-1.jar
        DEPENDS
            RaceSdkJavaShimsJar
            ${RACE_JAVA_SHIMS_JAR_BINARY_DIR}/racesdk-java-shims-1.jar
        COMMENT "Copying Java Shims Jar..."
    )
    add_custom_target(CopyRaceJavaShimsJarForCommsJavaTest ALL
        DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/racesdk-java-shims-1.jar)
    add_dependencies(unitTestPluginCommsJava CopyRaceJavaShimsJarForCommsJavaTest)
endif()


if (TARGET RaceJavaShims)
    add_custom_command(
        COMMAND cp $<TARGET_FILE:RaceJavaShims> ${CMAKE_CURRENT_BINARY_DIR}/libRaceJavaShims.so
        OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/libRaceJavaShims.so
        DEPENDS RaceJavaShims
        COMMENT "Copying RaceJavaShims library..."
    )
    add_custom_target(CopyRaceJavaShimsLibForCommsJavaTest ALL
        DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/libRaceJavaShims.so)
    add_dependencies(unitTestPluginCommsJava CopyRaceJavaShimsLibForCommsJavaTest)
endif()


if (NOT TARGET build_tests)
    add_custom_target(build_tests)
endif()
add_dependencies(build_tests unitTestPluginCommsJava)


add_test(comms-java-unit ${CMAKE_CURRENT_BINARY_DIR}/unitTestPluginCommsJava)
set_tests_properties(comms-java-unit PROPERTIES LABELS unit)
