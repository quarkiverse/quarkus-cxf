package io.quarkiverse.cxf.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.ehcache.sizeof.impl.AgentLoader")
final class Target_org_ehcache_sizeof_impl_AgentLoader {

    @Substitute
    static boolean loadAgent() {
        /* Loading the agent through the Attach API would not work either */
        return false;
    }

    static {
        // do nothing
    }
}

public class EhcacheSubstitutions {

}
