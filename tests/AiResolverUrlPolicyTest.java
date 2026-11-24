import com.pooyan.namakadetv.AiResolverUrlPolicy;
public final class AiResolverUrlPolicyTest {
    private static int checks;
    private static void yes(boolean b){checks++;if(!b)throw new AssertionError("policy check failed #"+checks);}
    public static void main(String[]args){
        yes("https://my-worker.workers.dev".equals(AiResolverUrlPolicy.normalize(" https://MY-WORKER.workers.dev/ ")));
        yes(AiResolverUrlPolicy.normalize("http://my-worker.workers.dev").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://127.0.0.1").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://localhost").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://x.internal").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://secret@example.com").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://worker.workers.dev/path").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://worker.workers.dev/?key=secret").isEmpty());
        yes(AiResolverUrlPolicy.normalize("https://worker.workers.dev:8443").isEmpty());
        System.out.println("PASS AiResolverUrlPolicyTest checks="+checks);
    }
}
