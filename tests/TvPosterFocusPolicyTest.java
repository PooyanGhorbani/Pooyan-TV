import com.pooyan.namakadetv.TvPosterFocusPolicy;
public final class TvPosterFocusPolicyTest {
    private static int checks=0;
    static void eq(int value,int expected){checks++; if(value!=expected)throw new AssertionError(value+" != "+expected);}
    public static void main(String[] args){
        eq(TvPosterFocusPolicy.neighbor(0,5,true),1);
        eq(TvPosterFocusPolicy.neighbor(4,5,true),-1);
        eq(TvPosterFocusPolicy.neighbor(0,5,false),-1);
        eq(TvPosterFocusPolicy.neighbor(3,5,false),2);
        eq(TvPosterFocusPolicy.neighbor(0,0,true),-1);
        eq(TvPosterFocusPolicy.column(3,2),1);
        eq(TvPosterFocusPolicy.column(0,2),0);
        eq(TvPosterFocusPolicy.column(5,0),-1);
        System.out.println("PASS TvPosterFocusPolicyTest checks="+checks);
    }
}
