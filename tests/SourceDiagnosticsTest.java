import com.pooyan.namakadetv.SourceDiagnostics;
public final class SourceDiagnosticsTest {
    private static int checks;
    private static void check(boolean truth, String message) {
        if (!truth) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        check(SourceDiagnostics.category(200).contains("200"), "200: reachable");
        check(SourceDiagnostics.category(206).contains("206"), "206: partial content");
        check(SourceDiagnostics.category(302).contains("انتقال"), "302: redirect");
        check(SourceDiagnostics.category(403).contains("محدودیت"), "403: restriction");
        check(SourceDiagnostics.category(404).contains("404"), "404: missing");
        check(SourceDiagnostics.category(503).contains("خطا"), "503: server error");
        System.out.println("PASS SourceDiagnosticsTest: " + checks + " checks");
    }
}
