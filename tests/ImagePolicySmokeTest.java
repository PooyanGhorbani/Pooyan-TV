import com.pooyan.namakadetv.ImageUrlPolicy;
public class ImagePolicySmokeTest {
 static int passes=0;
 static void check(boolean ok){if(!ok)throw new AssertionError("check "+passes);passes++;}
 public static void main(String[] args){
  check(ImageUrlPolicy.allowedFor("https://namakadeh.com/serieses/meymoone-mast","https://media.negahestan.com/cover.jpg"));
  check(!ImageUrlPolicy.allowedFor("https://farsiplex.com/movie/test/","https://media.negahestan.com/cover.jpg"));
  check(!ImageUrlPolicy.allowed("https://farsiplex.com.evil.test/cover.png"));
  check(!ImageUrlPolicy.allowed("http://namakadeh.com/cover.jpg"));
  check(!ImageUrlPolicy.allowed("https://127.0.0.1/a.jpg"));
  check(!ImageUrlPolicy.allowed("https://namakadeh.com:8080/a.png"));
  System.out.println("PASS image policy "+passes+" checks");
 }
}
