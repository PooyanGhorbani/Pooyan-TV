import com.pooyan.namakadetv.SiteCatalog;
import java.util.HashSet;
import java.util.Set;

public final class SiteCatalogTest {
    private static int passed;
    private static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
        passed++;
    }
    public static void main(String[] args) {
        check(SiteCatalog.all().size() == 7, "seven sources");
        Set<String> ids = new HashSet<>();
        for (SiteCatalog.Site site : SiteCatalog.all()) {
            check(ids.add(site.id), "unique id: " + site.id);
            check(SiteCatalog.matches(site, site.url), "own URL matches: " + site.id);
            check(SiteCatalog.fromUrl(site.url) == site, "resolve source: " + site.id);
            check(SiteCatalog.byId(site.id) == site, "lookup source: " + site.id);
            check(!SiteCatalog.matches(site, "http://" + site.host), "reject http");
            check(!SiteCatalog.matches(site, "https://" + site.host + ".evil.example"), "reject spoofed hostname");
        }
        check(SiteCatalog.group(SiteCatalog.VERIFIED).size() == 2, "verified sources");
        check(SiteCatalog.group(SiteCatalog.PUBLIC).size() == 3, "public sources");
        check(SiteCatalog.group(SiteCatalog.REVIEW).size() == 1, "review source");
        check(SiteCatalog.group(SiteCatalog.PERSONAL).size() == 1, "personal source");
        check(SiteCatalog.matches(SiteCatalog.byId("namakade"),
                "https://mobile.namakade.com:8443/serieses/rivals?private"), "private URL shape");
        check(!SiteCatalog.matches(SiteCatalog.byId("namakade"),
                "https://notnamakade.com"), "reject lookalike");
        check(SiteCatalog.fromUrl("https://www.parsimondo.de/fa/") != null, "subdomain match");
        check(SiteCatalog.fromUrl("https://unknown.example/") == null, "unknown URL");
        System.out.println("PASS SiteCatalogTest: " + passed + " checks");
    }
}
