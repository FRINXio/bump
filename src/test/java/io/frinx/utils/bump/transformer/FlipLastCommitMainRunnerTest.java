package io.frinx.utils.bump.transformer;

import static io.frinx.utils.bump.transformer.util.ProcessUtil.runProcess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import io.frinx.utils.bump.transformer.FlipLastCommitMainRunner.BumpSnapshotWorkflowTransformer.GitPatchWorkflow;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

public class FlipLastCommitMainRunnerTest {


    @Test
    public void test() throws Exception {
        File gitDir = File.createTempFile("gitinit", ".test");
        assertTrue(gitDir.delete());
        try {
            runProcess(gitDir.getParentFile(), Arrays.asList("git", "init", gitDir.getName()));
            try (Git git = Git.open(gitDir)) {
                System.out.println("Created a new repository at " + git.getRepository().getDirectory());

                GitPatchWorkflow tested = new GitPatchWorkflow(new TestingFileTransformer(), gitDir, "some commit message");
                File pomFile = new File(gitDir, "pom.xml");
                File featuresFile = new File(gitDir, "features.xml");
                addCommit(gitDir, "init", ImmutableMap.of(
                        pomFile, "thisartifact:1-SNAPSHOT\n" +
                                "depA:2-SNAPSHOT\n" +
                                "depExternal:9",
                        featuresFile, "depA:2-SNAPSHOT"));
                addCommit(gitDir, "release 1", ImmutableMap.of(
                        pomFile, "thisartifact:1\ndepA:2\ndepExternal:9",
                        featuresFile, "depA:2"));
                assertLogSize(git, 2);
                tested.doIt();

                assertLogSize(git, 3);
                String pomContent = Files.toString(pomFile, StandardCharsets.UTF_8);
                assertEquals("thisartifact:2-SNAPSHOT\n" +
                        "depA:3-SNAPSHOT\n" +
                        "depExternal:9", pomContent);
                String featuresContent = Files.toString(featuresFile, StandardCharsets.UTF_8);
                assertEquals("depA:3-SNAPSHOT", featuresContent);
            }
        } finally {
            FileUtils.deleteDirectory(gitDir);
        }
    }


    private static class TestingFileTransformer implements FileTransformer {
        @Override
        public TransformFileResult transformFile(File file) throws IOException {
            final String content = Files.toString(file, StandardCharsets.UTF_8);
            String modifiedContent = content.replace("+depA:2-SNAPSHOT", "+depA:3-SNAPSHOT")
                    .replace("thisartifact:1-SNAPSHOT", "thisartifact:2-SNAPSHOT");
            Files.write(modifiedContent, file, StandardCharsets.UTF_8);
            if (content.equals(modifiedContent) == false) {
                return TransformFileResult.CHANGED;
            }
            return TransformFileResult.NOT_CHANGED;
        }

    }

    private static void addCommit(File gitDir, String message, Map<File, String> fileNameToContent) throws Exception {
        for (Entry<File, String> entry : fileNameToContent.entrySet()) {
            String content = entry.getValue();
            File file = entry.getKey();
            Files.write(content, file, Charsets.UTF_8);
        }
        runProcess(gitDir, Arrays.asList("git", "add", "-A"));
        runProcess(gitDir, Arrays.asList("git", "commit", "-m", message));
    }

    private static void assertLogSize(Git git, int size) throws Exception {
        List<RevCommit> logs = Lists.newArrayList(git.log().call());
        assertEquals(size, logs.size());
    }

}
