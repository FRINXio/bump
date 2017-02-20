package io.frinx.utils.bump.transformer;

import java.io.File;
import java.io.IOException;

@FunctionalInterface
public interface FileTransformer {
    enum TransformFileResult {
        NOT_MATCHED, NOT_CHANGED, CHANGED
    }

    TransformFileResult transformFile(File file) throws IOException;

}
