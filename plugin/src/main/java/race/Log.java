package race;

import ShimsJava.RaceLog;

abstract class Log {

    private static final String pluginNameForLogging = "PluginCommsQrWifiJava";

    static void logDebug(String message) {
        RaceLog.logDebug(pluginNameForLogging, message, "");
    }

    static void logInfo(String message) {
        RaceLog.logInfo(pluginNameForLogging, message, "");
    }

    static void logWarning(String message) {
        RaceLog.logWarning(pluginNameForLogging, message, "");
    }

    static void logError(String message) {
        RaceLog.logError(pluginNameForLogging, message, "");
    }
}
