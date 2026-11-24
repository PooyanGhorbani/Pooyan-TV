package com.pooyan.namakadetv;

import org.jsoup.Jsoup;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PublicHtml5MediaTest {
    private static final String PAGE="https://farsiplex.com/movie/example/";
    @Test public void acceptsVisibleDirectHttpsHtml5Video() {
        String html="<video><source src='https://media.example.org/videos/test.mp4'></video>";
        assertEquals("https://media.example.org/videos/test.mp4", PublicHtml5Media.find(Jsoup.parse(html),PAGE));
    }
    @Test public void acceptsRelativeHlsButNoOtherProtocols() {
        String html="<video src='/clips/test.m3u8?quality=auto'></video>";
        assertEquals("https://farsiplex.com/clips/test.m3u8?quality=auto", PublicHtml5Media.find(Jsoup.parse(html),PAGE));
        assertEquals("",PublicHtml5Media.find(Jsoup.parse("<video src='http://bad.example/video.mp4'></video>"),PAGE));
    }
    @Test public void doesNotInspectIframesOrScripts() {
        String html="<iframe src='https://example.org/embed'></iframe>"+
          "<script>var protectedStream='https://example.org/secret.mp4';</script>";
        assertEquals("",PublicHtml5Media.find(Jsoup.parse(html),PAGE));
    }
    @Test public void rejectsHtmlPagesAndJavascriptUris() {
        String html="<video src='javascript:alert(1)'></video><video src='https://farsiplex.com/watch/123'></video>";
        assertEquals("",PublicHtml5Media.find(Jsoup.parse(html),PAGE));
    }
}
