import com.pooyan.namakadetv.CatalogIdentity;
import com.pooyan.namakadetv.CatalogMerge;
import com.pooyan.namakadetv.FarsiPlexTitle;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class CatalogMergeSmokeTest {
 static int passes=0;
 static void check(boolean ok){if(!ok)throw new AssertionError("check "+passes);passes++;}
 public static void main(String[] args){
  check(CatalogIdentity.normalized("ميمون، مست").equals("میمون مست"));
  FarsiPlexTitle first=new FarsiPlexTitle("https://farsiplex.com/tvshow/meymoone-mast/","series","میمون مست");
  FarsiPlexTitle second=new FarsiPlexTitle("https://namakadeh.com/serieses/meymoone-mast","series","میمون، مست");
  first.year="2026";second.year="2026";
  Map<String,List<FarsiPlexTitle>> variants=new HashMap<>();
  List<FarsiPlexTitle> joined=CatalogMerge.join(Arrays.asList(first),Arrays.asList(second),variants);
  check(joined.size()==1);check(joined.get(0).url.equals(first.url));
  check(variants.containsKey(first.url));check(variants.get(first.url).get(0).url.equals(second.url));
  check(CatalogMerge.join(Arrays.asList(first),new ArrayList<>(),new HashMap<>()).size()==1);
  System.out.println("PASS catalog merge "+passes+" checks");
 }
}
