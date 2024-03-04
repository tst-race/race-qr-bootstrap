

package race;

import ShimsJava.ChannelStatus;
import ShimsJava.ConnectionStatus;
import ShimsJava.IRacePluginComms;
import ShimsJava.JEncPkg;
import ShimsJava.JLinkConfig;
import ShimsJava.JLinkProperties;
import ShimsJava.JRaceSdkComms;
import ShimsJava.LinkStatus;
import ShimsJava.LinkType;
import ShimsJava.PackageStatus;
import ShimsJava.PluginConfig;
import ShimsJava.PluginResponse;
import ShimsJava.RaceHandle;
import ShimsJava.RaceLog;
import ShimsJava.SdkResponse;
import ShimsJava.UserDisplayType;
import ShimsJava.BootstrapActionType;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.app.Activity;

import android.nfc.Tag;
import android.util.Log;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/*
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
*/

/*
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
*/

//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.activity.result.ActivityResultCaller;
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;

//import com.twosix.race.R;


public class PluginCommsQrWifiJava implements IRacePluginComms {

    private static String logLabel = "Java Comms QrWifi Plugin";
    private static final String TAG = "PluginCommsQrWifiJava";

    private JRaceSdkComms jSdk;
    String racePersona;
    // Map of linkIds to Links
    Map<String, Link> links = new HashMap<>();
    // Map of connectionIds to Connections
    Map<String, Connection> connections = new HashMap<>();

    // Map of channel GID keys to their respective status values.
    Map<String, ChannelStatus> channelStatuses =
            new HashMap<String, ChannelStatus>() {
                {
                    put(Channels.bootstrapQrWifiChannelGid, ChannelStatus.CHANNEL_UNAVAILABLE);
                    put(Channels.indirectChannelGid, ChannelStatus.CHANNEL_UNAVAILABLE);
                }
            };

    // The next available port for creating direct links.
    int nextAvailablePort = 10000;
    // The whiteboard hostname for indirect channel.
    String whiteboardHostname = "twosix-whiteboard";
    // The whiteboard port for indirect channel.
    int whiteboardPort = 5000;
    // The next available hashtag for creating indirect links.
    int nextAvailableHashtag = 0;

    // The node hostname to use with direct links
    String hostname = "0.0.0.0";
    RaceHandle requestHostnameHandle;
    RaceHandle requestStartPortHandle;

    Set<RaceHandle> userInputRequests = new HashSet<RaceHandle>();

    private String currentPassphrase = "";
    String pluginConfigFilePath;
    Random generator = new Random();
    WifiManager wifiManager;
    WifiConfiguration currentConfig;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;
    FinishActivityBroadcastReceiver finishBroadcastReceiver;
    private static Context context;
    private int numPeers = -1;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    /** @return the jSdk */
    public JRaceSdkComms getjSdk() {
        return jSdk;
    }

    PluginCommsQrWifiJava(JRaceSdkComms sdk) {
        RaceLog.logDebug(logLabel, "PluginCommsQrWifiJava constructor called", "");
        jSdk = sdk;
        RaceLog.logDebug(logLabel, "PluginCommsQrWifiJava constructor returned", "");
    }

    /**
     * Initialize the plugin. Set the RaceSdk object and other prep work to begin allowing calls
     * from TA3 and other plugins.
     *
     * @param pluginConfig Config object containing dynamic config variables (e.g. paths)
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse init(PluginConfig pluginConfig) {
        RaceLog.logDebug(logLabel, "inside init", "");
        Log.d(TAG, " inside init");
        RaceLog.logDebug(
                logLabel,
                " Plugin Config: "
                        + "{ etcDirectory: \""
                        + pluginConfig.etcDirectory
                        + "\", loggingDirectory: \""
                        + pluginConfig.loggingDirectory
                        + "\", auxDataDirectory: \""
                        + pluginConfig.auxDataDirectory
                        + "\", tmpDirectory: \""
                        + pluginConfig.tmpDirectory
                        + "\" }",
                "");

        // Configuring Persona
        racePersona = jSdk.getActivePersona();
        RaceLog.logDebug(logLabel, "    active persona: " + racePersona, "");

        SdkResponse response =
                jSdk.writeFile("initialized.txt",  (logLabel + "Initialized\n").getBytes());
        if (response == null || response.getStatus() != SdkResponse.SdkStatus.SDK_OK) {
            RaceLog.logWarning(logLabel, "Failed to write to plugin storage", "");
        }
        byte[] readMsg = jSdk.readFile("initialized.txt");
        if (readMsg != null) {
            RaceLog.logDebug(logLabel, new String(readMsg), "");
        }

        RaceLog.logDebug(logLabel, "ending init", "");
        return PluginResponse.PLUGIN_OK;
    }

    /**
     * Activate a specific channel
     *
     * @param raceHandle The RaceHandle to use for activateChannel calls
     * @param channelGid The channel to activate
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse activateChannel(RaceHandle handle, String channelGid, String roleName) {

        RaceLog.logDebug(logLabel, "activateChannel called for " + channelGid, "");
        jSdk.onChannelStatusChanged(
                RaceHandle.NULL_RACE_HANDLE,
                channelGid,
                ChannelStatus.CHANNEL_AVAILABLE,
                Channels.getDefaultChannelPropertiesForChannel(jSdk, channelGid),
                jSdk.getBlockingTimeout());

        return PluginResponse.PLUGIN_OK;
    }

    /**
     * Shutdown the plugin. Close open connections, remove state, etc.
     *
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse shutdown() {
        RaceLog.logDebug(logLabel, "shutdown: called", "");

        TinyWebServer.stopServer();
        stopHotspot();

        ArrayList<String> connectionIds = new ArrayList<>(connections.keySet());
        for (String connectionId : connectionIds) {
            closeConnection(RaceHandle.NULL_RACE_HANDLE, connectionId);
        }
        System.gc();
        return PluginResponse.PLUGIN_OK;
    }

    /**
     * Open a connection with a given type on the specified link. Additional configuration info can
     * be provided via the config param.
     *
     * @param handle The RaceHandle to use for onConnectionStatusChanged calls
     * @param linkType The type of link to open: send, receive, or bi-directional.
     * @param linkId The ID of the link that the connection should be opened
     * @param linkHints Additional optional configuration information provided by NM as a
     *     stringified JSON Object.
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse openConnection(
            RaceHandle handle,
            LinkType linkType,
            String linkId,
            String linkHints,
            int sendTimeout) {
        synchronized (this) {
            RaceLog.logDebug(logLabel, "openConnection called", "");

            RaceLog.logDebug(logLabel, "    type:         " + linkType.name(), "");
            RaceLog.logDebug(logLabel, "    ID:           " + linkId, "");

            String newConnectionId = jSdk.generateConnectionId(linkId);

            RaceLog.logDebug(logLabel, "    Assigned connectionID: " + newConnectionId, "");
            RaceLog.logDebug(logLabel, "    Current number of connectionLinkIds: " + connections.size(), "");

            Link link = links.get(linkId);

            Connection connection = link.openConnection(linkType, newConnectionId, linkHints);
            connections.put(connection.connectionId, connection);

            JLinkProperties linkProps = new JLinkProperties();
            if (links.containsKey(linkId)) {
                linkProps = links.get(linkId).getLinkProperties();
            }

            jSdk.onConnectionStatusChanged(
                    handle,
                    newConnectionId,
                    ConnectionStatus.CONNECTION_OPEN,
                    linkProps,
                    JRaceSdkComms.getBlockingTimeout());
            return PluginResponse.PLUGIN_OK;
        }
    }

    /**
     * Close a connection with a given ID.
     *
     * @param handle The RaceHandle to use for onConnectionStatusChanged calls
     * @param connectionId The ID of the connection to close.
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse closeConnection(RaceHandle handle, String connectionId) {
        synchronized (this) {
            RaceLog.logDebug(logLabel, "closeConnection called", "");
            RaceLog.logDebug(logLabel, "    ID: " + connectionId, "");

            Link link = connections.get(connectionId).getLink();
            connections.remove(connectionId);
            link.closeConnection(connectionId);

            step5_finish();

            jSdk.onConnectionStatusChanged(
                    handle,
                    connectionId,
                    ConnectionStatus.CONNECTION_CLOSED,
                    link.getLinkProperties(),
                    JRaceSdkComms.getBlockingTimeout());
            return PluginResponse.PLUGIN_OK;
        }
    }

    @Override
    public PluginResponse destroyLink(RaceHandle handle, String linkId) {
        String logPrefix =
                String.format("destroyLink: (handle: %s link ID: %s):", handle.toString(), linkId);
        RaceLog.logDebug(logLabel, String.format("%s called", logPrefix), "");

        Link link = this.links.get(linkId);
        if (link == null) {
            RaceLog.logError(
                    logLabel, String.format("%s link with ID does not exist", logPrefix), "");
            return PluginResponse.PLUGIN_ERROR;
        }

        jSdk.onLinkStatusChanged(
                handle,
                linkId,
                LinkStatus.LINK_DESTROYED,
                links.get(linkId).getLinkProperties(),
                jSdk.getBlockingTimeout());

        Vector<Connection> connectionsInLink = link.getLinkConnections();
        for (Connection connection : connectionsInLink) {
            // Makes SDK API call to onConnectionStatusChanged.
            this.closeConnection(handle, connection.connectionId);
        }

        links.remove(linkId);

        step5_finish();

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    @Override
    public PluginResponse createLink(RaceHandle handle, String channelGid) {
        String logPrefix =
                String.format(
                        "createLink: (handle: %s channel GID: %s):", handle.toString(), channelGid);
        RaceLog.logDebug(logLabel, String.format("%s called", logPrefix), "");
/*
        if (this.channelStatuses.get(channelGid) != ChannelStatus.CHANNEL_AVAILABLE) {
            RaceLog.logError(logLabel, String.format("%s channel not available.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }
*/
        JLinkProperties linkProps = Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid);

        if (channelGid.equals(Channels.bootstrapQrWifiChannelGid)) {
            RaceLog.logDebug(logLabel, String.format("%s Creating TwoSix Direct Link", logPrefix), "");

            String linkAddress =
                    String.format(
                            "{\"hostname\":\"%s\",\"port\":%d}",
                            this.hostname, this.nextAvailablePort++);

            DirectLinkProfileParser parser = new DirectLinkProfileParser(linkAddress);
            linkProps.linkType = LinkType.LT_RECV;
            linkProps.channelGid = channelGid;

            JLinkConfig linkConfig = new JLinkConfig();
            linkConfig.linkProfile = linkAddress;
            linkConfig.linkProps = linkProps;
            Link link = parser.createLink(this, linkConfig, channelGid);
            if (link == null) {
                RaceLog.logError(logLabel, String.format("%s failed to create direct link.", logPrefix), "");
                jSdk.onLinkStatusChanged(
                        handle,
                        "",
                        LinkStatus.LINK_DESTROYED,
                        Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                        jSdk.getBlockingTimeout());
                return PluginResponse.PLUGIN_ERROR;
            }
            linkProps.linkAddress = linkAddress;

            links.put(link.linkId, link);
            RaceLog.logError(
                    logLabel,
                    String.format(
                            "%s calling onLinkStatusChanged with channel GID: %s",
                            logPrefix, linkProps.channelGid),
                    "");
            jSdk.onLinkStatusChanged(
                    handle,
                    link.linkId,
                    LinkStatus.LINK_CREATED,
                    linkProps,
                    jSdk.getBlockingTimeout());
            jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());

            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s Created TwoSix Direct Link with link ID: %s and link address: %s",
                            logPrefix, link.linkId, linkAddress),
                    "");
        } else {
            RaceLog.logError(logLabel, String.format("%s invalid channel GID.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    @Override
    public PluginResponse loadLinkAddress(
            RaceHandle handle, String channelGid, String linkAddress) {
        String logPrefix =
                String.format(
                        "loadLinkAddress: (handle: %s channel GID: %s):",
                        handle.toString(), channelGid);
        RaceLog.logDebug(
                logLabel,
                String.format("%s called with link address: %s", logPrefix, linkAddress),
                "");
        this.channelStatuses.put(channelGid, ChannelStatus.CHANNEL_AVAILABLE);
        if (this.channelStatuses.get(channelGid) != ChannelStatus.CHANNEL_AVAILABLE) {
            RaceLog.logError(logLabel, String.format("%s channel not available.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }

        JLinkProperties linkProps = Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid);

        if (channelGid.equals(Channels.bootstrapQrWifiChannelGid)) {
            RaceLog.logDebug(
                    logLabel, String.format("%s Loading TwoSix Direct Link", logPrefix), "");

            DirectLinkProfileParser parser = new DirectLinkProfileParser(linkAddress);
            linkProps.linkType = LinkType.LT_SEND;

            JLinkConfig linkConfig = new JLinkConfig();
            linkConfig.linkProfile = linkAddress;
            linkConfig.linkProps = linkProps;
            Link link = parser.createLink(this, linkConfig, channelGid);
            if (link == null) {
                RaceLog.logError(
                        logLabel, String.format("%s failed to load direct link.", logPrefix), "");
                jSdk.onLinkStatusChanged(
                        handle,
                        "",
                        LinkStatus.LINK_DESTROYED,
                        Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                        jSdk.getBlockingTimeout());
                return PluginResponse.PLUGIN_ERROR;
            }

            links.put(link.linkId, link);
            RaceLog.logError(
                    logLabel,
                    String.format(
                            "%s calling onLinkStatusChanged with channel GID: %s",
                            logPrefix, linkProps.channelGid),
                    "");
            jSdk.onLinkStatusChanged(
                    handle,
                    link.linkId,
                    LinkStatus.LINK_LOADED,
                    linkProps,
                    jSdk.getBlockingTimeout());
            jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());

            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s Loaded TwoSix Direct Link with link ID: %s and link address: %s",
                            logPrefix, link.linkId, linkAddress),
                    "");
        } else if (channelGid.equals(Channels.indirectChannelGid)) {
            RaceLog.logDebug(
                    logLabel, String.format("%s Loading TwoSix Indirect Link", logPrefix), "");

            TwosixWhiteboardLinkProfileParser parser =
                    new TwosixWhiteboardLinkProfileParser(linkAddress);
            linkProps.linkType = LinkType.LT_BIDI;

            JLinkConfig linkConfig = new JLinkConfig();
            linkConfig.linkProfile = linkAddress;
            linkConfig.linkProps = linkProps;
            Link link = parser.createLink(this, linkConfig, channelGid);
            if (link == null) {
                RaceLog.logError(
                        logLabel, String.format("%s failed to load indirect link.", logPrefix), "");
                jSdk.onLinkStatusChanged(
                        handle,
                        "",
                        LinkStatus.LINK_DESTROYED,
                        Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                        jSdk.getBlockingTimeout());
                return PluginResponse.PLUGIN_ERROR;
            }

            links.put(link.linkId, link);
            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s calling onLinkStatusChanged with channel GID: %s",
                            logPrefix, linkProps.channelGid),
                    "");
            jSdk.onLinkStatusChanged(
                    handle,
                    link.linkId,
                    LinkStatus.LINK_LOADED,
                    linkProps,
                    jSdk.getBlockingTimeout());
            jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());

            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s Loaded TwoSix Indirect Link with link ID: %s and link address: %s",
                            logPrefix, link.linkId, linkAddress),
                    "");
        } else {
            RaceLog.logError(logLabel, String.format("%s invalid channel GID.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    @Override
    public PluginResponse loadLinkAddresses(
            RaceHandle handle, String channelGid, String[] linkAddresses) {
        String logPrefix =
                String.format(
                        "loadLinkAddresses: (handle: %s channel GID: %s):",
                        handle.toString(), channelGid);
        RaceLog.logDebug(
                logLabel,
                String.format(
                        "%s called with link addresses: %s",
                        logPrefix, Arrays.toString(linkAddresses)),
                "");
        RaceLog.logError(
                logLabel,
                String.format("%s API not supported for any TwoSix channels", logPrefix),
                "");
        jSdk.onLinkStatusChanged(
                handle,
                "",
                LinkStatus.LINK_DESTROYED,
                Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                jSdk.getBlockingTimeout());
        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_ERROR;
    }

    @Override
    public PluginResponse createLinkFromAddress(
            RaceHandle handle, String channelGid, String linkAddress) {
        String logPrefix =
                String.format(
                        "createLinkFromAddress: (handle: %s channel GID: %s):",
                        handle.toString(), channelGid);
        RaceLog.logDebug(
                logLabel,
                String.format("%s called with link address: %s", logPrefix, linkAddress),
                "");

        if (this.channelStatuses.get(channelGid) != ChannelStatus.CHANNEL_AVAILABLE) {
            RaceLog.logError(logLabel, String.format("%s channel not available.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }

        JLinkProperties linkProps = Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid);
        linkProps.linkAddress = linkAddress;

        if (channelGid.equals(Channels.bootstrapQrWifiChannelGid)) {
            RaceLog.logDebug(
                    logLabel, String.format("%s Creating TwoSix Direct Link", logPrefix), "");

            DirectLinkProfileParser parser = new DirectLinkProfileParser(linkAddress);
            linkProps.linkType = LinkType.LT_RECV;

            JLinkConfig linkConfig = new JLinkConfig();
            linkConfig.linkProfile = linkAddress;
            linkConfig.linkProps = linkProps;
            Link link = parser.createLink(this, linkConfig, channelGid);
            if (link == null) {
                RaceLog.logError(
                        logLabel, String.format("%s failed to create direct link.", logPrefix), "");
                jSdk.onLinkStatusChanged(
                        handle,
                        "",
                        LinkStatus.LINK_DESTROYED,
                        Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                        jSdk.getBlockingTimeout());
                return PluginResponse.PLUGIN_ERROR;
            }

            links.put(link.linkId, link);
            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s calling onLinkStatusChanged with channel GID: %s",
                            logPrefix, linkProps.channelGid),
                    "");
            jSdk.onLinkStatusChanged(
                    handle,
                    link.linkId,
                    LinkStatus.LINK_CREATED,
                    linkProps,
                    jSdk.getBlockingTimeout());
            jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());

            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s Created TwoSix Direct Link with link ID: %s and link address: %s",
                            logPrefix, link.linkId, linkAddress),
                    "");
        } else if (channelGid.equals(Channels.indirectChannelGid)) {
            RaceLog.logDebug(
                    logLabel, String.format("%s Creating TwoSix Indirect Link", logPrefix), "");

            TwosixWhiteboardLinkProfileParser parser =
                    new TwosixWhiteboardLinkProfileParser(linkAddress);
            linkProps.linkType = LinkType.LT_BIDI;

            JLinkConfig linkConfig = new JLinkConfig();
            linkConfig.linkProfile = linkAddress;
            linkConfig.linkProps = linkProps;
            Link link = parser.createLink(this, linkConfig, channelGid);
            if (link == null) {
                RaceLog.logError(
                        logLabel,
                        String.format("%s failed to create indirect link.", logPrefix),
                        "");
                jSdk.onLinkStatusChanged(
                        handle,
                        "",
                        LinkStatus.LINK_DESTROYED,
                        Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                        jSdk.getBlockingTimeout());
                return PluginResponse.PLUGIN_ERROR;
            }

            links.put(link.linkId, link);
            RaceLog.logError(
                    logLabel,
                    String.format(
                            "%s calling onLinkStatusChanged with channel GID: %s",
                            logPrefix, linkProps.channelGid),
                    "");
            jSdk.onLinkStatusChanged(
                    handle,
                    link.linkId,
                    LinkStatus.LINK_CREATED,
                    linkProps,
                    jSdk.getBlockingTimeout());
            jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());

            RaceLog.logDebug(
                    logLabel,
                    String.format(
                            "%s Created TwoSix Indirect Link with link ID: %s and link address: %s",
                            logPrefix, link.linkId, linkAddress),
                    "");
        } else {
            RaceLog.logError(logLabel, String.format("%s invalid channel GID.", logPrefix), "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return PluginResponse.PLUGIN_ERROR;
        }

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    @Override
    public PluginResponse deactivateChannel(RaceHandle handle, String channelGid) {
        String logPrefix =
                String.format(
                        "deactivateChannel: (handle: %s channel GID: %s):",
                        handle.toString(), channelGid);
        RaceLog.logDebug(logLabel, String.format("%s called", logPrefix), "");

        if (this.channelStatuses.get(channelGid) == null) {
            RaceLog.logError(logLabel, String.format("%s channel does not exist.", logPrefix), "");
            return PluginResponse.PLUGIN_ERROR;
        }

        this.channelStatuses.put(channelGid, ChannelStatus.CHANNEL_UNAVAILABLE);
        jSdk.onChannelStatusChanged(
                handle,
                channelGid,
                ChannelStatus.CHANNEL_UNAVAILABLE,
                Channels.getDefaultChannelPropertiesForChannel(jSdk, channelGid),
                jSdk.getBlockingTimeout());

        List<String> linkIdsOfLinksToDestroy = new ArrayList<String>();
        Iterator iter = this.links.entrySet().iterator();

        for (String linkId : links.keySet()) {
            if (links.get(linkId).getLinkProperties().channelGid.equals(channelGid)) {
                linkIdsOfLinksToDestroy.add(linkId);
            }
        }

        for (String linkId : linkIdsOfLinksToDestroy) {
            destroyLink(handle, linkId);
        }

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    @Override
    public PluginResponse onUserInputReceived(
            RaceHandle handle, boolean answered, String response) {
        String logPrefix = String.format("onUserInputReceived (handle: %s):", handle.toString());
        RaceLog.logDebug(logLabel, String.format("%s called", logPrefix), "");

        if (handle.equals(this.requestHostnameHandle)) {
            if (answered) {
                this.hostname = response;
                RaceLog.logInfo(
                        logLabel,
                        String.format("%s using hostname %s", logPrefix, this.hostname),
                        "");
            } else {
                RaceLog.logError(
                        logLabel,
                        String.format(
                                "%s direct channel not available without the hostname", logPrefix),
                        "");
                jSdk.onChannelStatusChanged(
                        RaceHandle.NULL_RACE_HANDLE,
                        Channels.bootstrapQrWifiChannelGid,
                        ChannelStatus.CHANNEL_UNAVAILABLE,
                        Channels.getDefaultChannelPropertiesForChannel(
                                jSdk, Channels.bootstrapQrWifiChannelGid),
                        jSdk.getBlockingTimeout());
            }
        } else if (handle.equals(this.requestStartPortHandle)) {
            if (answered) {
                try {
                    int port = Integer.parseInt(response);
                    RaceLog.logInfo(
                            logLabel, String.format("%s using start port %d", logPrefix, port), "");
                    this.nextAvailablePort = port;
                } catch (NumberFormatException nfe) {
                    RaceLog.logWarning(
                            logLabel,
                            String.format("%s error parsing start port, %s", logPrefix, response),
                            "");
                }
            } else {
                RaceLog.logWarning(
                        logLabel,
                        String.format("%s no answer, using default start port", logPrefix),
                        "");
            }
        } else {
            RaceLog.logWarning(
                    logLabel, String.format("%s handle is not recognized", logPrefix), "");
            return PluginResponse.PLUGIN_ERROR;
        }
        userInputRequests.remove(handle);
        if (userInputRequests.size() == 0) {
            this.channelStatuses.put(Channels.bootstrapQrWifiChannelGid, ChannelStatus.CHANNEL_AVAILABLE);
            jSdk.onChannelStatusChanged(
                    RaceHandle.NULL_RACE_HANDLE,
                    Channels.bootstrapQrWifiChannelGid,
                    ChannelStatus.CHANNEL_AVAILABLE,
                    Channels.getDefaultChannelPropertiesForChannel(jSdk, Channels.bootstrapQrWifiChannelGid),
                    jSdk.getBlockingTimeout());
        }

        RaceLog.logDebug(logLabel, String.format("%s returned", logPrefix), "");
        return PluginResponse.PLUGIN_OK;
    }

    /**
     * Open a connection with a given type on the specified link. Additional configuration info can
     * be provided via the config param.
     *
     * @param handle The RaceHandle to use for updating package status in onPackageStatusChanged
     *     calls
     * @param connectionId The ID of the connection to use to send the package.
     * @param pkg The encrypted package to send.
     * @param timeoutTimestamp The time the package must be sent by. Measured in seconds since epoch
     * @param batchId The batch ID used to group encrypted packages so that they can be flushed at
     *     the same time when flushChannel is called. If this value is zero then it can safely be
     *     ignored.
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse sendPackage(
            RaceHandle handle,
            String connectionId,
            JEncPkg pkg,
            double timeoutTimestamp,
            long batchId) {
        synchronized (this) {
            String loggingPrefix = logLabel + ": sendPackage (" + connectionId + "): ";
            RaceLog.logDebug(loggingPrefix, "sendPackage called", "");

            RaceLog.logDebug(loggingPrefix, "connectionId: " + connectionId, "");
            RaceLog.logDebug(loggingPrefix, "link: " + connections.get(connectionId).getLink(), "");
            final String linkProfile = connections.get(connectionId).getLink().getProfile();
            RaceLog.logDebug(loggingPrefix, "profile: " + linkProfile, "");

            connections.get(connectionId).getLink().sendPackage(pkg);
            jSdk.onPackageStatusChanged(
                    handle, PackageStatus.PACKAGE_SENT, JRaceSdkComms.getBlockingTimeout());
            return PluginResponse.PLUGIN_OK;
        }
    }

    @Override
    public PluginResponse flushChannel(RaceHandle handle, String channelGid, long batchId) {
        RaceLog.logError("StubCommsPlugin", "API not supported by exemplar plugin", "");
        return PluginResponse.PLUGIN_ERROR;
    }

    @Override
    public PluginResponse serveFiles(String linkId, String path) {
        RaceLog.logDebug(logLabel, "Entering serveFiles", "");

        // check path for things that may cause command injection.
        if (path.contains(";") || path.contains(" ") || path.contains(" ") || path.contains("|") || path.contains("&")) {
            RaceLog.logDebug(logLabel, "path ('" + path + "') contained things that may cause command injection, failing out", "");
            return PluginResponse.PLUGIN_ERROR;
        }

//        try {
            // create race folder, move everything from path into that folder
            // String[] full_cmd;
            // String cmd = "cd " + path + " && mkdir placeholder_folder && mv !(placeholder_folder) placeholder_folder && mv placeholder_folder race";
            // full_cmd = new String[]{"/bin/sh", "-c", cmd};
            // Runtime.getRuntime().exec(full_cmd).waitFor(2, TimeUnit.SECONDS);
            //// // copy apk into path
            //// Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            //// Context context = app.getApplicationContext();
            //// String apkPath = context.getApplicationInfo().sourceDir;
            //// cmd = "cp " + apkPath + " " + path + "/race.apk";
            //// full_cmd = new String[]{"/bin/sh", "-c", cmd};
            //// Runtime.getRuntime().exec(full_cmd).waitFor(10, TimeUnit.SECONDS);
            //// tar up everything in path into race.tar
            // cmd = "cd " + path + " && tar -c --dereference -C " + path + " -f race.tar race"; // race.apk";
            // full_cmd = new String[]{"/bin/sh", "-c", cmd};
            // Runtime.getRuntime().exec(full_cmd).waitFor(10, TimeUnit.SECONDS);

            // serve race.tar
            step4_startWebserver(path /*+ "/race.tar"*/, currentPassphrase);
            return PluginResponse.PLUGIN_OK;
        // } catch (ClassNotFoundException e) {
        //     e.printStackTrace();
        // } catch (NoSuchMethodException e) {
        //     e.printStackTrace();
        // } catch (IllegalAccessException e) {
        //     e.printStackTrace();
        // } catch (InvocationTargetException e) {
        //     e.printStackTrace();
  //      } catch (IOException e) {
  //          e.printStackTrace();
  //      } catch (InterruptedException e) {
  //          e.printStackTrace();
  //      }
  //      return PluginResponse.PLUGIN_ERROR;
    }


    /**
     * @brief Notify the plugin that the user acknowledged the displayed information
     *
     * @param handle The handle for asynchronously returning the status of this call.
     * @return PluginResponse the status of the Plugin in response to this call
     */
    @Override
    public PluginResponse onUserAcknowledgementReceived(RaceHandle handle) {


        return PluginResponse.PLUGIN_OK;
    }



    private void step1_startHotspot(RaceHandle handle, String channelGid, String passphrase) {
        RaceLog.logDebug(logLabel, "entering step1_startHotspot()", "");

        try {
            Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            Context context = app.getApplicationContext();
            wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);

            RaceLog.logDebug(logLabel, "wifi state: " + wifiManager.getWifiState(), "");

            // after android 10 (api 29) wificonfigurations will always return empty list due to privacy
            // List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
            // RaceLog.logDebug(logLabel, "WiFi configurations available: " + wifiConfigurations.size(),"");
            // if (wifiConfigurations.size() == 0) {
            //     displayLocationSettingsRequest(context.getApplicationContext());
            // }
            wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    hotspotReservation = reservation;
                    currentConfig = hotspotReservation.getWifiConfiguration();


                    RaceLog.logInfo(logLabel, "\nTHE PASSWORD IS: "
                            + currentConfig.preSharedKey
                            + " \nSSID is : "
                            + currentConfig.SSID,"");
                    step2_qrCodes_webserver(handle, channelGid, passphrase);
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    RaceLog.logInfo(logLabel, "Local Hotspot Stopped","");
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    RaceLog.logInfo(logLabel, "Local Hotspot failed to start: " + reason,"");
                 //   finish();
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
            RaceLog.logError(logLabel, "Caught SecurityException. Please check permissions and try again. If permissions are granted, most likely Location Services are turned off.", "");
        }
    }

    private void step2_qrCodes_webserver(RaceHandle handle, String channelGid, String passphrase) {
        RaceLog.logDebug(logLabel, "entering step2_qrCodes_webserver()", "");

        String qrCreds = generateWifiQrCodeString();

        // set button to continue to next screen
        setButtonContinue();

        // make instruction 1 visible
   /* waiting for R -->
        ((TextView) findViewById(R.id.instructions)).setText("1. Scan QR code to authenticate to WiFi hotspot.");
        ((TextView) findViewById(R.id.instructions)).setVisibility(View.VISIBLE);
        ((ImageView) findViewById(R.id.qr_image)).setVisibility(View.VISIBLE);
*/
        // create QR code with wifi connection details
        Bitmap qr_wifi = createQrCode(qrCreds);

        jSdk.displayBootstrapInfoToUser(qrCreds, UserDisplayType.UD_QR_CODE, BootstrapActionType.BS_NETWORK_CONNECT);

        // set QR code to image in activity
  /* waiting for R -->
        ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qr_wifi);
*/
        // go to step 3
        step3_waitForConnection(handle, channelGid, passphrase);
    }

    private void step3_waitForConnection(RaceHandle handle, String channelGid, String passphrase) {
        RaceLog.logDebug(logLabel, "entering step3_waitForConnection()", "");

        JLinkProperties linkProps = Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid);

        RaceLog.logDebug(
            logLabel, String.format(" Creating Bootstrap Direct Link"), "");
     // get local IP address
        String localIp = getLocalIpAddress();
        // get random available ephemeral port
        int randomHighPort = getAvailableEphemeralPort();

        String linkAddress = String.format(
            "{\"hostname\":\"%s\",\"port\":%d}",
            localIp, randomHighPort);

        DirectLinkProfileParser parser = new DirectLinkProfileParser(linkAddress);
        linkProps.linkType = LinkType.LT_BIDI;
        linkProps.channelGid = channelGid;

        JLinkConfig linkConfig = new JLinkConfig();
        linkConfig.linkProfile = linkAddress;
        linkConfig.linkProps = linkProps;
        Link link = parser.createLink(this, linkConfig, channelGid);
        if (link == null) {
            RaceLog.logDebug(
                    logLabel, "failed to create direct link.", "");
            jSdk.onLinkStatusChanged(
                    handle,
                    "",
                    LinkStatus.LINK_DESTROYED,
                    Channels.getDefaultLinkPropertiesForChannel(jSdk, channelGid),
                    jSdk.getBlockingTimeout());
            return;
        }
        linkProps.linkAddress = linkAddress;

        links.put(link.linkId, link);
        RaceLog.logDebug(
                logLabel,
                String.format(
                        "calling onLinkStatusChanged with channel GID: %s",
                        linkProps.channelGid),
                "");

        // loop while number of peers == 0
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //if (newLocalPeer()) {
                if (true) {
                    currentPassphrase = passphrase;
                    // step4_startWebserver();
                    // TODO call onLinkStatusChanged
                    RaceLog.logDebug(logLabel, "step3_waitForConnection() --> calling onLinkStatusChanged (LINK_CREATED)", "");
                    jSdk.onLinkStatusChanged(
                            handle,
                            link.linkId,
                            LinkStatus.LINK_CREATED,
                            linkProps,
                            jSdk.getBlockingTimeout());
                    jSdk.updateLinkProperties(link.linkId, linkProps, jSdk.getBlockingTimeout());
                }
                else {
                    step3_waitForConnection(handle, channelGid, passphrase);
                }
            }
        }, 10000);
    }

    private void step4_startWebserver(String fileToServe, String passphrase) {
        RaceLog.logDebug(logLabel, "entering step4_startWebserver()", "");

        try {
            Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            Context context = app.getApplicationContext();
            // get local IP address
            String localIp = getLocalIpAddress();
            RaceLog.logDebug(logLabel, "localIp: " + localIp, "");

            // start server on localIp and random ephemeral port
            int randomHighPort = getAvailableEphemeralPort();
            RaceLog.logDebug(logLabel, "randomHighPort: " + randomHighPort, "");
            // String fileToServe = context.getApplicationInfo().sourceDir;

            RaceLog.logDebug(logLabel, "Calling TinyWebServer.startServer( " + localIp + ", " + randomHighPort + ", " + fileToServe + ", " + passphrase + ")", "");
            TinyWebServer.startServer(localIp, randomHighPort, fileToServe, passphrase);

            // set button to done to next screen
            setButtonDone();

            String downloadLink = "http://" + localIp + ":" + randomHighPort + "/" + passphrase;
            RaceLog.logDebug(logLabel, "Download bootstrap package at: " + downloadLink, "");

            jSdk.displayBootstrapInfoToUser(downloadLink, UserDisplayType.UD_QR_CODE, BootstrapActionType.BS_NETWORK_CONNECT);


            // Make instruction #2 visible
            /* needs R -->
            ((TextView) findViewById(R.id.instructions)).setText("2. Scan QR code to download app.");
            ((TextView) findViewById(R.id.instructions)).setVisibility(View.VISIBLE);
            ((ImageView) findViewById(R.id.qr_image)).setVisibility(View.VISIBLE);

            // create QR code link to APK
            Bitmap qr_apk = createQrCode(downloadLink);
            ((ImageView) findViewById(R.id.qr_image)).setImageBitmap(qr_apk);
             */
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void step5_finish() {
        RaceLog.logDebug(logLabel, "entering step5_finish()", "");

        TinyWebServer.stopServer();
        stopHotspot();

        // this function is used as a callback in TinyWebServer
    //    finish();
    }

    private void setButtonDone() {
        RaceLog.logDebug(logLabel, "entering setButtonDone()", "");
        /* need race R -->
        // finish activity when "DONE" button is pushed
        ((Button) findViewById(R.id.button)).setText("DONE");
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                finish();
                step5_finish();
                Log.d(TAG, "Done button clicked. Finishing PluginCommsQrWifiJava.");
            }
        });
         */
    }

    private void setButtonContinue() {
        RaceLog.logDebug(logLabel, "entering setButtonContinue()", "");
        /* needs race R
        // go to step 2 when "CONTINUE" button is pushed
        ((Button) findViewById(R.id.button)).setText("CONTINUE");
        ((Button) findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                step4_startWebserver();
                Log.d(TAG, "Continue button clicked. Starting webserver.");
            }
        });
         */
    }

    private boolean newLocalPeer() {
        RaceLog.logDebug(logLabel, "entering newLocalPeer()", "");

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            RaceLog.logDebug(logLabel, "Failed to open /proc/net/arp.", "");
            return false;
        }

        String line;
        try {
            int peerCounter = 0;
            while ((line = br.readLine()) != null) {
                // Log.d(TAG, line);
                String[] splitted = line.split(" +");

                if ((splitted != null) && (splitted.length >= 4)) {
                    // Basic sanity check
                    String mac = splitted[3];
                    String ip = splitted[0];

                    // return true if MAC address is valid and IP is in 192.168.43.0/24
                    if (mac.matches("..:..:..:..:..:..") && ip.startsWith("192.168.43.")) {
                        RaceLog.logDebug(logLabel,  "Found reachable MAC: " + mac, "");
                        peerCounter += 1;
                    }
                }
            }
            // Log.d(TAG, "numPeers=" + numPeers);
            if (numPeers == -1) {
                // if first time initializing numPeers from -1, dont return true
                numPeers = peerCounter;
            } else {
                if (peerCounter > numPeers) {
                    // if there's a new peer, return true
                    return true;
                } else {
                    // if there isn't a new peer, set numPeers to latest peerCounter
                    numPeers = peerCounter;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            RaceLog.logDebug(logLabel,  "Failed to read from /proc/net/arp.", "");
            return false;
        }
        RaceLog.logDebug(logLabel,  "Found no reachable MAC addresses", "");
        return false;
    }

    private String getLocalIpAddress() {
        RaceLog.logDebug(logLabel, "entering getLocalIpAddress()", "");

/*
        try {
            String addressToReturn = "";
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (inetAddress.isSiteLocalAddress() && inetAddress.getHostAddress().startsWith("192.168.255.")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
            RaceLog.logDebug(logLabel, "getLocalIpAddress returning: " + addressToReturn, "");
            return addressToReturn;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
*/



        try {
            String addressToReturn = "";
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    RaceLog.logDebug(logLabel, "found local INetAddress: " + inetAddress.getHostAddress(), "");
                    // should be a sitelocal address (10.*/8, 172.16.*/12, or 192.168.*/16, or fc00::/7)
                    // should not be loopback address
                    // should be IPv4
                     if (inetAddress.isSiteLocalAddress() &&
                         !inetAddress.isLoopbackAddress() &&
                         (inetAddress instanceof Inet4Address)) {
                        if (addressToReturn == "") {
                            RaceLog.logDebug(logLabel, "setting (low  certainty) addressToReturn=" + inetAddress.getHostAddress(), "");
                            addressToReturn = inetAddress.getHostAddress();
                        }
                     }
                    // should be 192.168.43 address
                    if (inetAddress.getHostAddress().startsWith("192.168.")) {
                        if (!addressToReturn.startsWith("192.168.")) {
                            // if address starts with 192.168., it is likely a tunnel (for emulator)
                            // and most likely to be the address. set it to this
                            RaceLog.logDebug(logLabel, "setting (mediu  certainty) addressToReturn=" + inetAddress.getHostAddress(), "");
                            addressToReturn = inetAddress.getHostAddress();
                        }
                        if (inetAddress.getHostAddress().startsWith("192.168.53.")) {
                            RaceLog.logDebug(logLabel, "setting (high certainty) addressToReturn=" + inetAddress.getHostAddress(), "");
                            addressToReturn = inetAddress.getHostAddress();
                        }
                    }
                }
            }
            RaceLog.logDebug(logLabel, "getLocalIpAddress returning: " + addressToReturn, "");
            return addressToReturn;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;

    }

    private int getAvailableEphemeralPort() {
        int randPort = generator.nextInt(16383) + 49152;
        while (!isAvailable(randPort)) {
            randPort = generator.nextInt(16383) + 49152;
        }
        return randPort;
    }

    private boolean isAvailable(int portNr) {
        boolean portFree;
        try (ServerSocket ignored = new ServerSocket(portNr)) {
            ignored.close();
            portFree = true;
        } catch (IOException e) {
            portFree = false;
        }
        return portFree;
    }

    private Bitmap createQrCode(String content) {
        RaceLog.logDebug(logLabel, "entering createQrCode()", "");

        // need com.google.zxing
        /*
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            Log.d(TAG, "Created QR code with data: `" + content + "`");
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
        }
         */
        return null;
    }

    private void stopHotspot() {
        RaceLog.logDebug(logLabel, "entering stopHotspot()", "");

        if (hotspotReservation != null){
            hotspotReservation.close();
        }
    }

    private String generateWifiQrCodeString() {
        RaceLog.logDebug(logLabel, "entering generateWifiQrCodeString()", "");

        String hiddenString = currentConfig.hiddenSSID ? "True" : "False";
        String qrCreds = "WIFI:T:WPA;S:" + currentConfig.SSID + ";P:" + currentConfig.preSharedKey + ";H:" + hiddenString + ";";
        Log.d(TAG, qrCreds);
        return qrCreds;
    }

    public Activity getActivity(Context context)
    {
        if (context == null)
        {
            return null;
        }
        else if (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            else
            {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }

    private void requestPermissionsRationale(String permission, String rationale) {
        RaceLog.logDebug(logLabel, "entering requestPermissionsRationale(" + permission + ", " + rationale + ")", "");

        try {
            Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            Context context = app.getApplicationContext();
            Activity activity = getActivity(context);

            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // shouldShowRequestPermissionRationale(rationale);
                // request the permission
                RaceLog.logError(logLabel, "permission not granted: " + permission, "");
                if (activity != null) {
                    RaceLog.logDebug(logLabel, "requesting permission: " + permission, "");
                    activity.requestPermissions(new String[]{permission}, 0);
                } else {
                    RaceLog.logError(logLabel, "could not get activity, can not request permission: " + permission, "");
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

//    public void checkLocationAccess() {
//        RaceLog.logDebug(logLabel, "entering checkLocationAccess()", "");
//
//        try {
//
//            Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
//            Context context = app.getApplicationContext();
//            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//            if (!manager.isLocationEnabled()) {
//                RaceLog.logError(logLabel, "location disabled, asking for access", "");
//                Log.d(TAG, "Location access disabled. Asking for access.");
//                displayLocationSettingsRequest(context.getApplicationContext());
//            } else {
//                RaceLog.logError(logLabel, "location enabled", "");
//                Log.d(TAG, "Location access enabled");
//                step1_startHotspot();
//            }
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//
//    }

//    private void displayLocationSettingsRequest(Context context) {
//        RaceLog.logDebug(logLabel, "entering displayLocationSettingsRequest()", "");
//
//        ActivityResultLauncher<String[]> locationPermissionRequest =
//                ((AppCompatActivity)getActivity(context)).registerForActivityResult(new ActivityResultContracts
//                .RequestMultiplePermissions(), result -> {
//                    Boolean fineLocationGranted = result.getOrDefault(
//                            Manifest.permission.ACCESS_FINE_LOCATION, false);
//                    Boolean coarseLocationGranted = result.getOrDefault(
//                            Manifest.permission.ACCESS_COARSE_LOCATION,false);
//                    if (fineLocationGranted != null && fineLocationGranted) {
//                        // Precise location access granted.
//                        RaceLog.logDebug(logLabel, "Precise location access granted.", "");
//                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
//                        // Only approximate location access granted.
//                        RaceLog.logDebug(logLabel, "Only coarse location access granted.", "");
//                    } else {
//                        // No location access granted.
//                        RaceLog.logDebug(logLabel, "No location access granted.", "");
//                    }
//                }
//            );
//
//        // Before you perform the actual permission request, check whether your app
//        // already has the permissions, and whether your app needs to show a permission
//        // rationale dialog. For more details, see Request permissions.
//        locationPermissionRequest.launch(new String[] {
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//        });
//
//        ///// import GoogleApiClient
//        /*
//
//        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
//                .addApi(LocationServices.API).build();
//        googleApiClient.connect();
//
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locationRequest.setInterval(10000);
//        locationRequest.setFastestInterval(10000 / 2);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
//        builder.setAlwaysShow(true);
//
//        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult result) {
//                final Status status = result.getStatus();
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        Log.i(TAG, "All location settings are satisfied.");
//                        step1_startHotspot();
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
//                        try {
//                            // Show the dialog by calling startResolutionForResult(), and check the result
//                            // in onActivityResult().
//                            status.startResolutionForResult(PluginCommsQrWifiJava.this, REQUEST_CHECK_SETTINGS);
//                        } catch (IntentSender.SendIntentException e) {
//                            Log.i(TAG, "PendingIntent unable to execute request.");
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
//                        finish();
//                        break;
//                    default:
//                        Log.i(TAG, "Location settings not turned on");
//                        finish();
//                        break;
//                }
//            }
//        });
//      */
//    }

    public class FinishActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Log.d(TAG, "finishBroadcastReceiver recieved intent: " + intent.getAction());
            if (intent.getAction().equals("finishPluginCommsQrWifiJava")) {
                TinyWebServer.stopServer();
                stopHotspot();
                ((Activity) arg0).finish();
            }
        }
    }


    @Override
    public PluginResponse createBootstrapLink(
            RaceHandle handle, String channelGid, String passphrase) {
        RaceLog.logDebug(logLabel, "entering createBootstrapLink", "");

        try {

            Application app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null, (Object[]) null);
            Context context = app.getApplicationContext();

            // setButtonDone(); // needs com.twosix.race.R

            // create broadcastreceiver so webserver can kill activity after one download
            FinishActivityBroadcastReceiver finishBroadcastReceiver = new FinishActivityBroadcastReceiver();
            context.registerReceiver(finishBroadcastReceiver, new IntentFilter("finishPluginCommsQrWifiJava"));

            // request permissions
            requestPermissionsRationale(Manifest.permission.ACCESS_WIFI_STATE, "Wifi state is needed to start a hotspot for sharing the RACE app.");
            requestPermissionsRationale(Manifest.permission.CHANGE_WIFI_STATE, "Setting wifi state is needed to start a hotspot for sharing the RACE app.");
            requestPermissionsRationale(Manifest.permission.ACCESS_NETWORK_STATE, "Network state is needed to start a hotspot for sharing the RACE app.");
            requestPermissionsRationale(Manifest.permission.CHANGE_NETWORK_STATE, "Setting network state is needed to start a hotspot for sharing the RACE app.");
            requestPermissionsRationale(Manifest.permission.ACCESS_COARSE_LOCATION, "Coarse location is needed to start a hotspot for sharing the RACE app.");
            requestPermissionsRationale(Manifest.permission.ACCESS_FINE_LOCATION, "Fine location is needed to start a hotspot for sharing the RACE app.");

            // checkLocationAccess();
            // checkLocationAccess calls step1_startHotspot() when location access is granted
            // displayLocationSettingsRequest(context.getApplicationContext());
            step1_startHotspot(handle, channelGid, passphrase);
            // if location access is not granted, it calls finish()

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();

        }

        return PluginResponse.PLUGIN_OK;
    }



    /**
     * @param key
     * @param value
     * @return boolean
     */
    boolean saveValue(String key, int value) {
        String valueString = "" + value;
        String filename = pluginConfigFilePath + "/" + racePersona + ":" + key;
        try {
            Files.write(
                    Paths.get(filename),
                    valueString.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            // exception handling left as an exercise for the reader
        }
        return false;
    }

    /**
     * @param key
     * @param defaultValue
     * @return int
     */
    int readValue(String key, int defaultValue) {
        BufferedReader valueData = null;
        if (!readBytes(key, valueData)) {
            return defaultValue;
        }

        String input = "";
        String line;
        try {
            while ((line = valueData.readLine()) != null) {
                input = input + line;
            }

            JSONParser parser = new JSONParser();
            JSONObject values = (JSONObject) parser.parse(input);
            return (int) values.get(key);
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * @param key
     * @param reader
     * @return boolean
     */
    boolean readBytes(String key, BufferedReader reader) {
        String filename = pluginConfigFilePath + racePersona + ":" + key;
        try {
            File file = new File(filename);
            reader = new BufferedReader(new FileReader(file));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    void parseChannelSettings(String channelGid, String channelConfigFilePath) {
        JSONParser parser = new JSONParser();
        Object obj;
        try {
            obj = parser.parse(new FileReader(channelConfigFilePath));
        } catch (Exception e) {
            RaceLog.logError(logLabel, e.getMessage(), "");
            return;
        }

        JSONObject settingsJson = (JSONObject) ((JSONObject) obj).get("settings");

        if (channelGid.equals(Channels.indirectChannelGid)) {
            if (settingsJson != null) {
                JSONObject whiteboard = (JSONObject) settingsJson.get("whiteboard");
                if (whiteboard != null && whiteboard.get("hostname") != null) {
                    this.whiteboardHostname = (String) whiteboard.get("hostname");
                    RaceLog.logDebug(
                            logLabel,
                            "parseChannelSettings: "
                                    + channelGid
                                    + ": setting whiteboard hostname: "
                                    + String.valueOf(this.whiteboardHostname),
                            "");
                }
                if (whiteboard != null && whiteboard.get("port") != null) {
                    this.whiteboardPort = (int) (long) whiteboard.get("port");
                    RaceLog.logDebug(
                            logLabel,
                            "parseChannelSettings: "
                                    + channelGid
                                    + ": setting whiteboard port: "
                                    + String.valueOf(this.whiteboardPort),
                            "");
                }
            } else {
                RaceLog.logDebug(logLabel, "parseChannelSettings: no settings section found", "");
            }
        }
    }
}

