import com.pooyan.namakadetv.PosterPolicy;

public class PosterPolicyTest {
    static int count;
    static void ok(boolean v) { count++; if (!v) throw new AssertionError("Test " + count); }
    public static void main(String[] args) {
        ok(PosterPolicy.accept("https://babakfilm.com/movie/test", "https://babakfilm.com/img/test.jpg"));
        ok(PosterPolicy.accept("https://tmk.ir/title/abc", "https://cdn.tmk.ir/img/a.webp"));
        ok(!PosterPolicy.accept("https://example.com/a", "https://example.com/a.jpg"));
        ok(!PosterPolicy.accept("https://babakfilm.com/a", "https://evil.example/img.jpg"));
        ok(!PosterPolicy.accept("https://babakfilm.com/a", "http://babakfilm.com/a.png"));
        ok(!PosterPolicy.accept("https://babakfilm.com/a", "https://babakfilm.com:8080/a.png"));
        ok(!PosterPolicy.accept("https://babakfilm.com/a", "https://babakfilm.com/a.svg"));
        ok(!PosterPolicy.accept("https://babakfilm.com/a", "https://babakfilm.com.evil/a.jpg"));
        System.out.println("PASS PosterPolicyTest: " + count + " checks");
    }
}
