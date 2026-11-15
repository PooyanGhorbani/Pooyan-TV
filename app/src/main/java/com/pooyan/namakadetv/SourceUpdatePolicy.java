package com.pooyan.namakadetv;

import java.net.URI;

/** A remote manifest may change paths on known public providers, never add unknown domains. */
public final class SourceUpdatePolicy {
    private SourceUpdatePolicy() {}
    public static boolean accept(String siteId, String url) {
        SiteCatalog.Site site = SiteCatalog.byId(siteId);
        if (siteId == null || !site.id.equals(siteId) || SiteCatalog.PERSONAL.equals(site.group)) return false;
        if (!SiteCatalog.matches(site, url)) return false;
        try {
            URI u = new URI(url);
            return (u.getPort() == -1 || u.getPort() == 443) &&
                    u.getRawQuery() == null && u.getRawFragment() == null;
        } catch (Exception e) { return false; }
    }
}
