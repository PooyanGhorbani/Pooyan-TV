import com.pooyan.namakadetv.PlaybackResumePolicy;
public final class PlaybackResumePolicyTest {
    private static int checks;
    private static void check(boolean truth, String message) {
        if (!truth) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        check(PlaybackResumePolicy.resumeAfterBackground(63_000, 600_000, false) == 63_000, "resume midpoint");
        check(PlaybackResumePolicy.resumeAfterBackground(-12, 600_000, false) == 0, "negative position");
        check(PlaybackResumePolicy.resumeAfterBackground(800_000, 600_000, false) == 600_000, "position capped");
        check(PlaybackResumePolicy.resumeAfterBackground(600_000, 600_000, true) == 0, "completed video resets");
        check(PlaybackResumePolicy.resumeAfterBackground(0, -1, false) == 0, "live stream");
        check(PlaybackResumePolicy.resumeAfterBackground(50_000, -1, false) == 50_000, "unknown duration");
        System.out.println("PASS PlaybackResumePolicyTest: " + checks + " checks");
    }
}
