import com.pooyan.namakadetv.PosterPolicy;
public final class PosterPolicyTest {
    static int n=0;static void check(boolean ok){if(!ok)throw new AssertionError(n);n++;}
    public static void main(String[] args){
        String movie="https://farsiplex.com/movie/harchi-to-begi/";
        check(PosterPolicy.accept(movie,"https://farsiplex.com/wp-content/uploads/2026/10/harchi-to-begi-185x278.jpg"));
        check(!PosterPolicy.accept(movie,"https://bad.site/poster.jpg"));
        check(!PosterPolicy.accept("https://other.example/movie/foo/","https://farsiplex.com/a.jpg"));
        check(!PosterPolicy.accept(movie,null));
        System.out.println("PASS Poster policy "+n+" checks");
    }
}
