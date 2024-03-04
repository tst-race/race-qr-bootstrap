package race;

import ShimsJava.LinkType;

public class Connection {
    protected Link link;

    final String connectionId;
    final LinkType linkType;

    Connection(String connectionId, LinkType linkType, Link link) {
        this.link = link;
        this.connectionId = connectionId;
        this.linkType = linkType;
    }

    /** @return Link */
    Link getLink() {
        return link;
    }
}
