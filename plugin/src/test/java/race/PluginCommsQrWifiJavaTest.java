package race;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import ShimsJava.JRaceSdkComms;
import ShimsJava.PluginConfig;
import ShimsJava.PluginResponse;

import org.junit.Test;

public class PluginCommsQrWifiJavaTest {
    // Load the C++ so
    static {
        System.loadLibrary("RaceJavaShims");
    }

    @Test
    public void initShouldGetPersona() {
        JRaceSdkComms sdk = mock(JRaceSdkComms.class);
        when(sdk.getActivePersona()).thenReturn("race-server-1");

        PluginCommsQrWifiJava plugin = new PluginCommsQrWifiJava(sdk);

        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.pluginConfigFilePath = "src/test/resources/good-configs";
        assertEquals(PluginResponse.PLUGIN_OK, plugin.init(pluginConfig));
    }
}
