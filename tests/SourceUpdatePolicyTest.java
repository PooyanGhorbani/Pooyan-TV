import com.pooyan.namakadetv.SourceUpdatePolicy;

public class SourceUpdatePolicyTest {
    static int count;
    static void ok(boolean v) { count++; if (!v) throw new AssertionError("Test " + count); }
    public static void main(String[] args) {
        ok(SourceUpdatePolicy.accept("parsimondo", "https://parsimondo.de/fa/"));
        ok(SourceUpdatePolicy.accept("babak", "https://www.babakfilm.com/"));
        ok(!SourceUpdatePolicy.accept("namakade", "https://namakade.com/"));
        ok(!SourceUpdatePolicy.accept("unknown", "https://parsimondo.de/"));
        ok(!SourceUpdatePolicy.accept("babak", "https://evil.example/"));
        ok(!SourceUpdatePolicy.accept("babak", "https://babakfilm.com.evil.example/"));
        ok(!SourceUpdatePolicy.accept("babak", "http://babakfilm.com/"));
        ok(!SourceUpdatePolicy.accept("babak", "https://babakfilm.com:9090/"));
        ok(!SourceUpdatePolicy.accept("babak", "https://babakfilm.com/?token=secret"));
        ok(!SourceUpdatePolicy.accept("babak", "https://babakfilm.com/#id"));
        System.out.println("PASS SourceUpdatePolicyTest: " + count + " checks");
    }
}
