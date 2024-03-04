package race;

import ShimsJava.JEncPkg;
import ShimsJava.JLinkProperties;
import ShimsJava.LinkType;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public abstract class Link {
    final PluginCommsQrWifiJava plugin;
    final String linkId;
    LinkType linkType;
    final String profile;
    final Vector<String> personas;
    final JLinkProperties linkProperties;
    protected Map<String, Connection> connections = new HashMap<>();

    Link(
            PluginCommsQrWifiJava plugin,
            String linkId,
            String profile,
            Vector<String> personas,
            JLinkProperties linkProperties) {
        this.plugin = plugin;
        this.linkId = linkId;
        this.profile = profile;
        this.personas = personas;
        this.linkProperties = linkProperties;
    }

    /** @return the LinkId */
    String getLinkId() {
        return linkId;
    }

    /** @return the profile */
    String getProfile() {
        return profile;
    }

    /** @return the personas */
    public Vector<String> getPersonas() {
        return personas;
    }

    /** @return the linkProperties */
    public JLinkProperties getLinkProperties() {
        return linkProperties;
    }

    /** @return the linkConnections */
    public Vector<Connection> getLinkConnections() {
        return (Vector<Connection>) connections.values();
    }

    abstract Connection openConnection(
            LinkType linkType, final String connectionId, final String linkHints);

    abstract void closeConnection(String connectionId);

    abstract void sendPackage(JEncPkg pkg);
}
