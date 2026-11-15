import com.pooyan.namakadetv.CatalogPolicy;
public class CatalogPolicyTest {
  public static void main(String[] args) {
    int n=0;
    assertTrue(CatalogPolicy.catalog("movie", "").equals("https://v3-cinemeta.strem.io/catalog/movie/top.json")); n++;
    assertTrue(CatalogPolicy.catalog("series", "Sci-Fi").contains("genre=Sci-Fi")); n++;
    assertTrue(CatalogPolicy.search("movie", "فیلم ایرانی").contains("%D9%81")); n++;
    assertTrue(CatalogPolicy.search("series", "Harry & Sally").contains("%26")); n++;
    assertTrue(CatalogPolicy.meta("movie", "tt0111161").endsWith("tt0111161.json")); n++;
    try { CatalogPolicy.meta("movie", "../../secret"); throw new AssertionError(); } catch (IllegalArgumentException expected) { n++; }
    try { CatalogPolicy.catalog("../pwn", ""); throw new AssertionError(); } catch (IllegalArgumentException expected) { n++; }
    assertTrue(CatalogPolicy.isImageUrl("https://images.metahub.space/poster/medium/tt1234567/img"));n++;
    assertTrue(!CatalogPolicy.isImageUrl("http://192.168.1.2/private"));n++;
    assertTrue(!CatalogPolicy.isImageUrl("https://images.metahub.space.evil.invalid/a"));n++;
    System.out.println("PASS CatalogPolicy " + n + " checks");
  }
  private static void assertTrue(boolean v) { if(!v) throw new AssertionError(); }
}
