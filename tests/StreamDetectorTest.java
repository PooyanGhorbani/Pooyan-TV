import com.pooyan.namakadetv.StreamDetector;

public class StreamDetectorTest {
    private static int count = 0;
    private static void check(boolean condition, String message) {
        count++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(StreamDetector.isHttpsUrl("https://mobile.namakade.com:8443/serieses/rivals?private"), "HTTPS special port");
        check(StreamDetector.isDirectMedia("https://cdn.example.com/hls/index.m3u8?token=abc"), "HLS query token");
        check(StreamDetector.isDirectMedia("https://cdn.example.com/video.mpd"), "DASH");
        check(StreamDetector.isDirectMedia("https://cdn.example.com/video.mp4"), "MP4");
        check(StreamDetector.isDirectMedia("https://cdn.example.com/video.webm"), "WebM");
        check(!StreamDetector.isDirectMedia("https://cdn.example.com/video.m3u8.jpg"), "Avoid false positives");
        check(!StreamDetector.isDirectMedia("https://cdn.example.com/series/rivals"), "Ordinary page is not media");
        check(!StreamDetector.isDirectMedia("blob:https://cdn.example.com/id"), "No blob extraction");
        check(!StreamDetector.isHttpsUrl("http://cdn.example.com/video.mp4"), "No cleartext");
        check(!StreamDetector.isHttpsUrl("https://x:y@cdn.example.com/a.m3u8"), "No credentials in URL");
        check(!StreamDetector.isHttpsUrl("javascript:alert(1)"), "No JS scheme");
        check(!StreamDetector.isHttpsUrl("https://"), "No missing domain");
        check(StreamDetector.safeName("https://cdn.example.com/folder/episode01.mp4?token=1").equals("episode01.mp4"), "Friendly label");
        System.out.println("PASS " + count + " StreamDetector tests");
    }
}
