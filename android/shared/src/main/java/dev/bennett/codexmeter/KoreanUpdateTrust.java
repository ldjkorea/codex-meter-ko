package dev.bennett.codexmeter;
import java.net.URI;
/** Public release metadata is only a source hint; the pinned signing key is the trust anchor. */
public final class KoreanUpdateTrust {
    public static final String CERTIFICATE="6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3";
    public static boolean official(String url){try{URI u=URI.create(url);return "https".equals(u.getScheme())&&"github.com".equals(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1
        &&u.getQuery()==null&&u.getFragment()==null&&u.getPath().startsWith("/ldjkorea/codex-meter-ko/releases/")&&!u.getPath().contains("..")&&!u.getRawPath().contains("%");}catch(Exception e){return false;}}
    public static boolean compatible(String pkg,String cert,long installed,long downloaded){return "dev.bennett.codexmeter".equals(pkg)&&CERTIFICATE.equals(cert)&&downloaded>installed;}
}
