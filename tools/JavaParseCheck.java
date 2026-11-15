import javax.tools.*;
import java.util.*;
import java.nio.file.*;
import com.sun.source.util.*;

public class JavaParseCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager mgr = compiler.getStandardFileManager(diagnostics, null, null);
        List<String> paths = new ArrayList<>();
        Files.walk(Path.of(args[0])).filter(x -> x.toString().endsWith(".java"))
                .forEach(x -> paths.add(x.toString()));
        var sources = mgr.getJavaFileObjectsFromStrings(paths);
        JavacTask task = (JavacTask) compiler.getTask(null,mgr,diagnostics,List.of("-proc:none"),null,sources);
        task.parse();
        boolean ok = true;
        for (var d: diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println(d); ok = false;
            }
        }
        if (!ok) System.exit(1);
        System.out.println("PASS Java syntax parsing: " + paths.size() + " files");
    }
}
