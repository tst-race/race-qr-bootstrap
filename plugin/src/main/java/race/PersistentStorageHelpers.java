package race;

import ShimsJava.JRaceSdkComms;
import ShimsJava.SdkResponse.SdkStatus;

abstract class PersistentStorageHelpers {

    private static String logLabel = "Java Comms Plugin";

    /**
     * Save a value to persistent storage. This storage is per node. The value may be retrieved by
     * passing the same key to readValue.
     *
     * @param <T> The type of the value being saved
     * @param sdk The SDK instance used to access persistent storage APIs
     * @param key The key that may be used to retrieve the value
     * @param value The value associated with the key
     * @return true on success, false on failure
     */
    public static <T> boolean saveValue(JRaceSdkComms sdk, String key, T value) {
        String valueString = String.valueOf(value);
        return sdk.writeFile(key, valueString.getBytes()).getStatus().equals(SdkStatus.SDK_OK);
    }

    public static <T> T readValue(JRaceSdkComms sdk, String key, T defaultValue) {
        String loggingPrefix = "PeristentStorageHelpers::readValue (" + key + "): ";

        byte[] valueData = sdk.readFile(key);
        if (valueData == null || valueData.length == 0) {
            return defaultValue;
        }

        String valueString = new String(valueData);
        Log.logDebug(loggingPrefix + "key: " + key + " value: " + valueString);

        try {
            return ((Class<T>) defaultValue.getClass())
                    .getConstructor(String.class)
                    .newInstance(valueString);
        } catch (ReflectiveOperationException error) {
            Log.logError(
                    loggingPrefix
                            + "Could not read value of type "
                            + defaultValue.getClass().getName()
                            + " from key "
                            + key);
            return defaultValue;
        }
    }
}
