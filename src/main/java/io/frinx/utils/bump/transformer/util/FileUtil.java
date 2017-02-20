package io.frinx.utils.bump.transformer.util;

import static java.lang.String.format;

import io.frinx.utils.bump.transformer.FileTransformer;
import io.frinx.utils.bump.transformer.FileTransformer.TransformFileResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class FileUtil {

        public static void transformRecursively(File folder, FileTransformer function) throws IOException {
            final int[] notMatched_notChanged_changed = new int[3];
            java.nio.file.Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File file = path.toFile();
                    TransformFileResult transformFileResult = function.transformFile(file);
                    Objects.requireNonNull(transformFileResult);
                    if (transformFileResult == TransformFileResult.NOT_MATCHED) {
                        notMatched_notChanged_changed[0]++;
                    } else if (transformFileResult == TransformFileResult.NOT_CHANGED) {
                        notMatched_notChanged_changed[1]++;
                    } else {
                        notMatched_notChanged_changed[2]++;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println(format("Changed %d, Not changed %d, Not matched %d", notMatched_notChanged_changed[2],
                    notMatched_notChanged_changed[1], notMatched_notChanged_changed[0]));
        }
}
