import com.pooyan.namakadetv.FarsiPlexUrlPolicy;
public final class FarsiPlexUrlPolicyTest {
    static int n=0;
    private static void yes(boolean ok){if(!ok)throw new AssertionError("check "+n);n++;}
    private static void no(String url){yes(FarsiPlexUrlPolicy.normalize(FarsiPlexUrlPolicy.SITE,url).isEmpty());}
    public static void main(String[] args){
        String root=FarsiPlexUrlPolicy.SITE;
        yes(FarsiPlexUrlPolicy.normalize(root,"/movie/harchi-to-begi/").equals(root+"/movie/harchi-to-begi/"));
        yes(FarsiPlexUrlPolicy.normalize(root+"/movie/","../tvshow/taxi/").equals(root+"/tvshow/taxi/"));
        yes("movie".equals(FarsiPlexUrlPolicy.mediaType(root+"/movie/harchi-to-begi/")));
        yes("series".equals(FarsiPlexUrlPolicy.mediaType(root+"/tvshow/taxi-series-7d33ee0b/")));
        yes("episode".equals(FarsiPlexUrlPolicy.mediaType(root+"/episode/eshghe-abadi-se03-ep95/")));
        yes("".equals(FarsiPlexUrlPolicy.mediaType(root+"/wp-json/")));
        no("https://evil.example/movie/fake/");no("http://farsiplex.com/movie/a/");
        no("https://farsiplex.com.evil.example/movie/a/");
        no("https://user:password@farsiplex.com/movie/a/");
        no("javascript:alert(1)");
        no("https://farsiplex.com:8080/movie/a/");
        yes(FarsiPlexUrlPolicy.publicList("movie",1).equals(root+"/movie/"));
        yes(FarsiPlexUrlPolicy.publicList("series",2).equals(root+"/tvshow/page/2/"));
        try{FarsiPlexUrlPolicy.publicList("series",11);throw new AssertionError("limit");}catch(IllegalArgumentException expected){n++;}
        System.out.println("PASS FarsiPlex URL policy "+n+" checks");
    }
}
