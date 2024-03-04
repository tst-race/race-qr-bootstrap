package race;

import ShimsJava.ConnectionType;
import ShimsJava.JLinkConfig;
import ShimsJava.JLinkProperties;
import ShimsJava.RaceLog;
import ShimsJava.SendType;
import ShimsJava.TransmissionType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.Iterator;
import java.util.Vector;

public class ConfigParser {
    private static String logLabel = "ConfigParser";
    private String profilesPath = "";
    Vector<JLinkConfig> links = new Vector<JLinkConfig>();

    ConfigParser(String profilesPath) {
        this.profilesPath = profilesPath;
    }

    /**
     * @param activePersona
     * @return boolean
     */
    boolean parse(String activePersona) {
        if (activePersona.length() == 0) {
            RaceLog.logError(logLabel, "invalid persona", "");
            return false;
        }

        try {
            JSONParser parser = new JSONParser();

            Object obj = parser.parse(new FileReader(profilesPath + "/link-profiles.json"));

            JSONObject jsonObject = (JSONObject) obj;
            JSONArray linksJsonArray = (JSONArray) jsonObject.get("links");

            Iterator<JSONObject> iterator = linksJsonArray.iterator();
            while (iterator.hasNext()) {
                try {
                    links.add(parseLink(iterator.next(), activePersona));
                    RaceLog.logDebug(logLabel, "Succesfully added link", "");
                } catch (IllegalArgumentException e) {
                    RaceLog.logDebug(logLabel, e.getMessage(), "");
                    continue;
                }
            }
        } catch (Exception e) {
            RaceLog.logError(logLabel, e.getMessage(), "");
        }
        return true;
    }

    /**
     * @param link
     * @param activePersona
     * @return JLinkConfig
     * @throws IllegalArgumentException
     */
    JLinkConfig parseLink(JSONObject link, String activePersona) throws IllegalArgumentException {
        // TODO add check for is link valid

        // First check if this link is for the current node
        String utilizedBy = link.get("utilizedBy").toString();
        if (!utilizedBy.contains(activePersona)) {
            RaceLog.logDebug(logLabel, "Active Persona: " + activePersona, "");
            RaceLog.logDebug(logLabel, "Utitlized by: " + utilizedBy, "");
            throw new IllegalArgumentException("link profile not intended for this persona");
        }

        // Set new link profile
        JLinkConfig currentLinkConfig = new JLinkConfig();
        currentLinkConfig.linkProfile = link.get("profile").toString();

        // Set all the personas that this link can connect to.
        JSONArray personasConnectedToArray = (JSONArray) link.get("connectedTo");
        for (Object o : personasConnectedToArray) {
            String persona = (String) o;
            currentLinkConfig.personas.add(persona);
        }

        /**************************
         ** Set the properties
         ***************************/
        JSONObject props = ((JSONObject) link.get("properties"));

        // set link type (send/receive/bidi)
        String linkType = (String) props.get("type");
        currentLinkConfig.linkProps.linkType = JLinkProperties.linkTypeStringToEnum(linkType);

        // Set the transmissionType.
        if (props.get("multicast") != null && (Boolean) props.get("multicast")) {
            currentLinkConfig.linkProps.transmissionType = TransmissionType.TT_MULTICAST;
            currentLinkConfig.linkProps.connectionType = ConnectionType.CT_INDIRECT;
            currentLinkConfig.linkProps.sendType = SendType.ST_STORED_ASYNC;
        } else {
            currentLinkConfig.linkProps.transmissionType = TransmissionType.TT_UNICAST;
            currentLinkConfig.linkProps.connectionType = ConnectionType.CT_DIRECT;
            currentLinkConfig.linkProps.sendType = SendType.ST_EPHEM_SYNC;
        }

        // set best
        currentLinkConfig.linkProps.best =
                LinkProfileParser.parseLinkPropertyPair(
                        (JSONObject) ((JSONObject) link.get("properties")).get("best"));

        // set worst
        currentLinkConfig.linkProps.worst =
                LinkProfileParser.parseLinkPropertyPair(
                        (JSONObject) ((JSONObject) link.get("properties")).get("worst"));

        // set expected
        currentLinkConfig.linkProps.expected =
                LinkProfileParser.parseLinkPropertyPair(
                        (JSONObject) ((JSONObject) link.get("properties")).get("expected"));

        // reliable
        if (((JSONObject) link.get("properties")).get("reliable") != null) {
            currentLinkConfig.linkProps.reliable =
                    (boolean) ((JSONObject) link.get("properties")).get("reliable");
        }

        // hints
        if (((JSONObject) link.get("properties")).get("supported_hints") != null) {

            JSONArray hints =
                    (JSONArray) ((JSONObject) link.get("properties")).get("supported_hints");
            for (Object o : hints) {
                String hint = (String) o;
                currentLinkConfig.linkProps.supportedHints.add(hint);
            }
        }

        return currentLinkConfig;
    }
}
