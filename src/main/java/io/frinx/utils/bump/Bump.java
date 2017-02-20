// m2:com.google.guava:guava:jar:19.0
// m2:junit:junit:jar:4.12
// m2:org.eclipse.jgit:org.eclipse.jgit:jar:4.3.1.201605051710-r
// m2:org.eclipse.jgit:org.eclipse.jgit.archive:jar:4.3.1.201605051710-r
// m2:commons-io:commons-io:jar:2.5
// m2:org.slf4j:slf4j-simple:jar:1.7.2
package io.frinx.utils.bump;

import static io.frinx.utils.bump.transformer.util.LoggingUtil.fatal;

import io.frinx.utils.bump.transformer.FlipLastCommitMainRunner;
import io.frinx.utils.bump.transformer.MainRunner;
import io.frinx.utils.bump.transformer.simple.SimpleMainRunner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bump {

    public static final boolean ignoreErrors;
    private static final Map<String, MainRunner> namesToMainRunners;

    static {
        ignoreErrors = Boolean.parseBoolean(System.getProperty("ignoreErrors"));
        Map<String, MainRunner> map = new HashMap<>();

        map.put("flipLastCommit", new FlipLastCommitMainRunner());
        map.put("simple", new SimpleMainRunner());
        namesToMainRunners = Collections.unmodifiableMap(map);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw fatal("Not enough arguments, try -h");
        }
        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            namesToMainRunners.keySet().stream().sorted().forEach(System.out::println);
            System.exit(0);
        }
        String name = args[0];
        MainRunner mainRunner = namesToMainRunners.get(name);
        if (mainRunner == null) {
            throw fatal(String.format("Main runner '%s' not found, try -h", name));
        }
        List<String> shifted = new ArrayList<>(Arrays.asList(args));
        shifted.remove(0);
        mainRunner.run(shifted);
    }
}
