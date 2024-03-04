set(CMAKE_CXX_STANDARD 17)
include(warnings.cmake.txt)

if(NOT ANDROID)
  # Link JVM
  include_directories("/usr/lib/jvm/java-1.8.0-openjdk-amd64/include")
  include_directories("/usr/lib/jvm/java-8-openjdk-amd64/include/linux/")
  link_directories("/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre/lib/amd64/server/")
endif()

# Add the source to the shared library
add_library(PluginCommsQrWifiJava SHARED
    loader.cpp
)

set_target_properties(PluginCommsQrWifiJava PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
)

if(TARGET raceSdkCommon)
  set(RACE_SDK_COMMON raceSdkCommon)
  set(RACE_JAVA_SHIMS RaceJavaShims)
else()
  find_library(RACE_SDK_COMMON raceSdkCommon)
  find_library(RACE_JAVA_SHIMS RaceJavaShims)
endif()


target_link_libraries(PluginCommsQrWifiJava
  ${RACE_SDK_COMMON}
  ${RACE_JAVA_SHIMS}
)

install(
  TARGETS PluginCommsQrWifiJava
    LIBRARY DESTINATION lib/race/comms/PluginCommsQrWifiJava
)

install(
  FILES ${CMAKE_CURRENT_SOURCE_DIR}/manifest.json
    DESTINATION lib/race/comms/PluginCommsQrWifiJava
)

if(ANDROID)
    if ("${TARGET_ARCHITECTURE}" STREQUAL "ANDROID_x86_64")
        add_custom_command(TARGET PluginCommsQrWifiJava POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-x86_64-client/PluginCommsQrWifiJava
            COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:PluginCommsQrWifiJava> ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-x86_64-client/PluginCommsQrWifiJava/
            COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/manifest.json ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-x86_64-client/PluginCommsQrWifiJava/
        )
        set_property(DIRECTORY PROPERTY ADDITIONAL_MAKE_CLEAN_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-x86_64-client/
        )
    else()
        add_custom_command(TARGET PluginCommsQrWifiJava POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-arm64-v8a-client/PluginCommsQrWifiJava
            COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:PluginCommsQrWifiJava> ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-arm64-v8a-client/PluginCommsQrWifiJava/
            COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/manifest.json ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-arm64-v8a-client/PluginCommsQrWifiJava/
        )
        set_property(DIRECTORY PROPERTY ADDITIONAL_MAKE_CLEAN_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/android-arm64-v8a-client/
        )
    endif()
else()
    add_custom_command(TARGET PluginCommsQrWifiJava POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-client/PluginCommsQrWifiJava
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-server/PluginCommsQrWifiJava
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:PluginCommsQrWifiJava> ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-client/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/manifest.json ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-client/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:PluginCommsQrWifiJava> ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-server/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/manifest.json ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-server/PluginCommsQrWifiJava/
    )
    set_property(DIRECTORY PROPERTY ADDITIONAL_MAKE_CLEAN_FILES
        ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-client/
        ${CMAKE_CURRENT_SOURCE_DIR}/../../plugin/artifacts/linux-x86_64-server/
    )
endif()
