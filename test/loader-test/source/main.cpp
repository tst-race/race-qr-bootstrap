#include <IRacePluginComms.h>
#include <IRaceSdkComms.h>
#include <RaceLog.h>
#include <libgen.h>        // dirname
#include <linux/limits.h>  // PATH_MAX
#include <race/mocks/MockRaceSdkComms.h>
#include <string.h>  // memset
#include <unistd.h>  // readlink

#include <iostream>

#include "gmock/gmock.h"

std::int32_t main(std::int32_t argc, char **argv) {
    ::testing::InitGoogleMock(&argc, argv);
    return RUN_ALL_TESTS();
}

// TODO: break this test up into multiple test cases.
// TODO: add more assertions.
// TODO: move this into a separate file.
TEST(loader, test_it_all) {
    RaceLog::setLogLevel(RaceLog::LL_DEBUG);

    std::cout << "running main" << std::endl;

    MockRaceSdkComms sdk;
    ON_CALL(sdk, getActivePersona()).WillByDefault(::testing::Return("race-client-1"));
    ::testing::DefaultValue<SdkResponse>::Set(SDK_OK);

    const auto plugin = createPluginComms(&sdk);

    // Attempt the get the path of the executable.
    // TODO: WARNING: Linux specific code.
    // {
    //     char result[PATH_MAX];
    //     memset(&result, 0, sizeof(result));
    //     ssize_t count = readlink("/proc/self/exe", result, PATH_MAX);
    //     if (count != -1) {
    //         configPath = std::string(dirname(result)) + "/" + configPath;
    //     }
    // }

    PluginConfig pluginConfig;
    pluginConfig.pluginConfigFilePath = "not used";
    ASSERT_EQ(PLUGIN_OK, plugin->init(pluginConfig));
    ASSERT_EQ(PLUGIN_OK, plugin->shutdown());

    delete plugin;

    std::cout << "test main done" << std::endl;
}
