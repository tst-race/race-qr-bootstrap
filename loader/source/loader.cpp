#include <IRacePluginComms.h>
#include <RaceLog.h>

#include "PluginCommsJavaWrapper.h"

#ifndef TESTBUILD

extern "C" const RaceVersionInfo raceVersion = RACE_VERSION;
extern "C" const char *const racePluginId = "PluginCommsQrWifiJava";
extern "C" const char *const racePluginDescription = "Plugin COMMS Java Exemplar (Two Six Labs) ";

// replace this signature with your plugin signature
const char *const pluginClassSignature = "race/PluginCommsQrWifiJava";
const char *const logLabel = "JavaCommsLoader";

IRacePluginComms *createPluginComms(IRaceSdkComms *sdk) {
    RaceLog::logDebug(logLabel, "Loading Java Comms Plugin", "");
    return new PluginCommsJavaWrapper(sdk, racePluginId, pluginClassSignature);
}

void destroyPluginComms(IRacePluginComms *plugin) {
    if (plugin != nullptr) {
        delete plugin;
    }
}

#endif
