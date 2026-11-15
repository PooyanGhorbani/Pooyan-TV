import com.pooyan.namakadetv.PosterPolicy;
import com.pooyan.namakadetv.CatalogPolicy;
public final class PosterPolicyTest {
    public static void main(String[] args) {
        int n = 0;
        check(PosterPolicy.accept("https://x.example/a", "https://images.metahub.space/poster/small/tt0111161/img")); n++;
        check(!PosterPolicy.accept("https://x.example/a", "https://evil.example/p.jpg")); n++;
        check(!PosterPolicy.accept("https://x.example/a", null)); n++;
        check(!PosterPolicy.accept(null, "https://images.metahub.space/p")); n++;
        try { CatalogPolicy.search("movie", "  "); throw new AssertionError("blank query"); }
        catch (IllegalArgumentException expected) { n++; }
        System.out.println("PASS PosterPolicy " + n + " checks");
    }
    private static void check(boolean v) { if (!v) throw new AssertionError(); }
}
