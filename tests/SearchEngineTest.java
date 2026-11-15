import com.pooyan.namakadetv.SearchEngine;
import com.pooyan.namakadetv.SiteCatalog;

public class SearchEngineTest {
    static int assertions;
    static void ok(boolean condition) { assertions++; if (!condition) throw new AssertionError("Test " + assertions); }
    public static void main(String[] args) {
        ok(SearchEngine.matches("فيلم", "فیلم ایرانی"));
        ok(SearchEngine.matches("كتاب", "کتاب فیلم"));
        ok(SearchEngine.matches("RIVALS", "Rivals · سریال"));
        ok(SearchEngine.matches("سريال ایرانی", "سریال جدید ایرانی"));
        ok(!SearchEngine.matches("سریال ایرانی", "سریال ترکی"));
        ok(!SearchEngine.matches("", "abc"));
        ok(SearchEngine.normalize("پخش   فیلم!").equals("پخش فیلم"));
        String all = SearchEngine.webSearchUrl("فیلم ایرانی", null);
        ok(all.startsWith("https://www.bing.com/search?q="));
        ok(all.contains("site%3Aparsimondo.de"));
        ok(!all.contains("namakade.com"));
        String one = SearchEngine.webSearchUrl("Rivals", SiteCatalog.byId("babak"));
        ok(one.contains("site%3Ababakfilm.com"));
        ok(!one.contains("site%3Aparsimondo.de"));
        ok(SearchEngine.webSearchUrl("   ", null).isEmpty());
        System.out.println("PASS SearchEngineTest: " + assertions + " checks");
    }
}
