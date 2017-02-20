package io.frinx.utils.bump.transformer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ArgsParsingUtil {

    public static List<String> parse(List<String> inputArgs, Map<String, Consumer<String>> parsers) {
        List<String> remainingArgs = new ArrayList<>(inputArgs);

        Consumer<String> parser;
        while (remainingArgs.size() >= 2 && (parser = parsers.get(remainingArgs.get(0))) != null) {
            remainingArgs.remove(0);
            String value = remainingArgs.remove(0);
            parser.accept(value);
        }
        return remainingArgs;
    }

}
