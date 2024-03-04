################################################################################
# Java plugin jar
################################################################################

#set(JAR_FILE ${CMAKE_CURRENT_BINARY_DIR}/target/plugin-comms-qrwifi-java-1.jar)
#set(DEX_FILE ${CMAKE_CURRENT_BINARY_DIR}/target/PluginCommsQrWifiJava.dex)
if(ANDROID)
    set(JAR_FILE ${CMAKE_CURRENT_BINARY_DIR}/build/outputs/aar/classes.jar)
    set(DEX_FILE ${CMAKE_CURRENT_BINARY_DIR}/build/outputs/aar/PluginCommsQrWifiJava.dex)
else()
    set(JAR_FILE ${CMAKE_CURRENT_BINARY_DIR}/target/plugin-comms-qrwifi-java-1.jar)
endif()

file(GLOB_RECURSE PLUGIN_SOURCES CONFIGURE_DEPENDS src/*.java)

if(TARGET RaceSdkJavaShimsJar)
    get_target_property(RACE_JAVA_SHIMS_JAR_BINARY_DIR RaceSdkJavaShimsJar BINARY_DIR)
    get_target_property(RACE_JAVA_SHIMS_BINARY_DIR     RaceJavaShims       BINARY_DIR)
    set(RACE_JAVA_SHIMS_JAR_DEPEND RaceSdkJavaShimsJar)
    set(RACE_JAVA_SHIMS_DEPEND     RaceJavaShims)
else()
    set(RACE_JAVA_SHIMS_JAR_BINARY_DIR ${CMAKE_INSTALL_PREFIX}/lib/race/java)
    set(RACE_JAVA_SHIMS_BINARY_DIR     ${CMAKE_INSTALL_PREFIX}/lib)
    set(RACE_JAVA_SHIMS_JAR_DEPEND ${RACE_JAVA_SHIMS_JAR_BINARY_DIR}/racesdk-java-shims-1.jar)
    set(RACE_JAVA_SHIMS_DEPEND     ${RACE_JAVA_SHIMS_BINARY_DIR}/libRaceJavaShims.so)
endif()

list(APPEND MVN_ARGS
    -Dsystem.java.dir=${CMAKE_INSTALL_PREFIX}/lib/race/java
    -Dracesdk.java.shims.jar=${RACE_JAVA_SHIMS_JAR_BINARY_DIR}/racesdk-java-shims-1.jar
    -Djava.library.path=${RACE_JAVA_SHIMS_BINARY_DIR}
)

if(ANDROID)
#    list(APPEND MVN_ARGS -DskipTests)
#endif()
#
#add_custom_command(
add_custom_command(
        COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/gradle ./gradle
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/gradlew ./gradlew
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/build.gradle ./build.gradle
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/settings.gradle ./settings.gradle
        COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/gradle.properties ./gradle.properties
        COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/src ./src
        COMMAND ./gradlew build -x test -Drace.java.dependencies=${RACE_JAVA_SHIMS_JAR_BINARY_DIR}/
        DEPENDS ${PLUGIN_SOURCES} build.gradle ${RACE_JAVA_SHIMS_JAR_DEPEND} ${RACE_JAVA_SHIMS_DEPEND}
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        OUTPUT  ${CMAKE_CURRENT_SOURCE_DIR}/build/outputs/aar/plugin-comms-qrwifi-java-debug.aar
    )
    add_custom_command(
        COMMAND unzip -o plugin-comms-qrwifi-java-debug.aar
        DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/build/outputs/aar/plugin-comms-qrwifi-java-debug.aar
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/build/outputs/aar/
        OUTPUT  ${JAR_FILE}
    )
    add_custom_target(PluginCommsQrWifiJavaJar ALL DEPENDS ${JAR_FILE})
    set_property(TARGET PluginCommsQrWifiJavaJar PROPERTY ADDITIONAL_CLEAN_FILES
        ${CMAKE_CURRENT_BINARY_DIR}/build.gradle
        ${CMAKE_CURRENT_BINARY_DIR}/gradle.properties
        ${CMAKE_CURRENT_BINARY_DIR}/settings.gradle
        ${CMAKE_CURRENT_BINARY_DIR}/src/
        ${CMAKE_CURRENT_BINARY_DIR}/build/
    )  
else()
    add_custom_command(
    COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml ./pom.xml
    COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_SOURCE_DIR}/src ./src
    COMMAND mvn ${MVN_ARGS} compile package
    DEPENDS ${PLUGIN_SOURCES} pom.xml ${RACE_JAVA_SHIMS_JAR_DEPEND} ${RACE_JAVA_SHIMS_DEPEND}
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    OUTPUT ${JAR_FILE}
#)
)
    add_custom_target(PluginCommsQrWifiJavaJar ALL DEPENDS ${JAR_FILE})
    set_property(TARGET PluginCommsQrWifiJavaJar PROPERTY ADDITIONAL_CLEAN_FILES
#add_custom_target(PluginCommsQrWifiJavaJar ALL DEPENDS ${JAR_FILE})

#set_property(TARGET PluginCommsQrWifiJavaJar PROPERTY ADDITIONAL_CLEAN_FILES

    ${CMAKE_CURRENT_BINARY_DIR}/pom.xml
    ${CMAKE_CURRENT_BINARY_DIR}/src/
    ${CMAKE_CURRENT_BINARY_DIR}/target/
    #)
    )
  endif()

if(ANDROID)
    add_custom_command(
#        COMMAND /opt/android/build-tools/default/d8
#            --min-api ${ANDROID_PLATFORM}
#            --output ${CMAKE_CURRENT_BINARY_DIR}/target/
#            ${JAR_FILE}
#        COMMAND mv ${CMAKE_CURRENT_BINARY_DIR}/target/classes.dex ${DEX_FILE}

        COMMAND /opt/android/build-tools/default/d8
        --min-api ${ANDROID_PLATFORM}
	--output ${CMAKE_CURRENT_BINARY_DIR}/build/ classes.jar

	COMMAND mv ${CMAKE_CURRENT_BINARY_DIR}/build/classes.dex ${DEX_FILE}
        DEPENDS PluginCommsQrWifiJavaJar
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/build/outputs/aar/
	OUTPUT ${DEX_FILE}
    )

    add_custom_target(PluginCommsQrWifiJavaDex ALL DEPENDS ${DEX_FILE})
    install(FILES ${DEX_FILE} DESTINATION lib/race/comms/PluginCommsQrWifiJava)

    if ("${TARGET_ARCHITECTURE}" STREQUAL "ANDROID_x86_64")
        add_custom_command(TARGET PluginCommsQrWifiJavaDex POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-x86_64-client/PluginCommsQrWifiJava/
            COMMAND ${CMAKE_COMMAND} -E copy ${DEX_FILE} ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-x86_64-client/PluginCommsQrWifiJava/
        )
        set_property(TARGET PluginCommsQrWifiJavaDex PROPERTY ADDITIONAL_CLEAN_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-x86_64-client/
        )
    else()
        add_custom_command(TARGET PluginCommsQrWifiJavaDex POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-arm64-v8a-client/PluginCommsQrWifiJava/
            COMMAND ${CMAKE_COMMAND} -E copy ${DEX_FILE} ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-arm64-v8a-client/PluginCommsQrWifiJava/
        )
        set_property(TARGET PluginCommsQrWifiJavaDex PROPERTY ADDITIONAL_CLEAN_FILES
            ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/android-arm64-v8a-client/
        )
    endif()
else()
    install(FILES ${JAR_FILE} DESTINATION lib/race/comms/PluginCommsQrWifiJava)

    add_custom_command(TARGET PluginCommsQrWifiJavaJar POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-client/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E copy ${JAR_FILE} ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-client/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-server/PluginCommsQrWifiJava/
        COMMAND ${CMAKE_COMMAND} -E copy ${JAR_FILE} ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-server/PluginCommsQrWifiJava/
    )
    set_property(TARGET PluginCommsQrWifiJavaJar APPEND PROPERTY ADDITIONAL_CLEAN_FILES
        ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-client/
        ${CMAKE_CURRENT_SOURCE_DIR}/../plugin/artifacts/linux-x86_64-server/
    )
endif()


# It should be noted that there are Maven plugins to run this, *but* google-java-format
# requires Java 11 and Maven is running under Java 8
set(GOOGLE_JAVA_FORMAT_JAR google-java-format-1.8-all-deps.jar)
add_custom_command(
    COMMAND wget -q https://github.com/google/google-java-format/releases/download/google-java-format-1.8/${GOOGLE_JAVA_FORMAT_JAR}
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${GOOGLE_JAVA_FORMAT_JAR}
    COMMENT "Downloading google-java-format"
)

set(JAVA_11_TOOL /usr/lib/jvm/java-11-openjdk-amd64/bin/java)
add_custom_target(format_PluginCommsQrWifiJava
    COMMAND ${JAVA_11_TOOL} -jar ${CMAKE_CURRENT_BINARY_DIR}/${GOOGLE_JAVA_FORMAT_JAR}
        --aosp --replace ${PLUGIN_SOURCES}
    DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/${GOOGLE_JAVA_FORMAT_JAR}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)

if(TARGET format)
    add_dependencies(format format_PluginCommsQrWifiJava)
else()
    add_custom_target(format DEPENDS format_PluginCommsQrWifiJava)
endif()

add_custom_target(check_format_PluginCommsQrWifiJava
    COMMAND ${JAVA_11_TOOL} -jar ${CMAKE_CURRENT_BINARY_DIR}/${GOOGLE_JAVA_FORMAT_JAR}
        --aosp --dry-run --set-exit-if-changed ${PLUGIN_SOURCES}
    DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/${GOOGLE_JAVA_FORMAT_JAR}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)

if(TARGET check_format)
    add_dependencies(check_format check_format_PluginCommsQrWifiJava)
else()
    add_custom_target(check_format DEPENDS check_format_PluginCommsQrWifiJava)
endif()
