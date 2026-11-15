import com.pooyan.namakadetv.OpenFilms;
import com.pooyan.namakadetv.StreamDetector;
import java.util.HashSet;
public final class OpenFilmsTest {
    public static void main(String[] args){
        HashSet<String> ids = new HashSet<>();
        int count=0;
        for(OpenFilms.Film film:OpenFilms.all()){
            if(!StreamDetector.isDirectMedia(film.url))throw new AssertionError(film.title);
            if(!ids.add(film.id))throw new AssertionError("duplicate ID");
            if(!film.license.contains("CC BY"))throw new AssertionError("missing credit");
            if(!film.url.startsWith("https://video.blender.org/"))throw new AssertionError("unexpected host");
            count++;
        }
        if(count<5)throw new AssertionError("missing films");
        System.out.println("PASS OpenFilms " + count + " checks");
    }
}
