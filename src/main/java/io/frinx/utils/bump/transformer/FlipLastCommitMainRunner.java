package io.frinx.utils.bump.transformer;

import static com.google.common.base.Preconditions.checkState;
import static io.frinx.utils.bump.transformer.util.LoggingUtil.fatal;
import static io.frinx.utils.bump.transformer.util.ProcessUtil.runProcess;

import com.google.common.io.Files;
import io.frinx.utils.bump.transformer.FileTransformer.TransformFileResult;
import io.frinx.utils.bump.transformer.VersionTransformationStrategyFactory.VersionTransformationStrategy;
import io.frinx.utils.bump.transformer.simple.BumpSnapshotPatchTransformerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Workflow:
 * 1. revert release commit
 * git revert HEAD
 * 2. create patch of this reverted commit
 * git format-patch HEAD~1
 * 3. drop that reverted commit
 * git reset --hard HEAD~1
 * 4. modify the patch file
 * 5. apply and commit the patch as bump to new SNAPSHOT
 * git apply 0001-Revert-Release-0.1.6.1-frinx.patch
 * git commit
 */
public class FlipLastCommitMainRunner implements MainRunner {

    @Override
    public void run(List<String> args) throws Exception {

        if (args.size() == 0) {
            throw fatal("Not enough arguments, try -h");
        }
        if (args.size() == 1 && "-h".equals(args.get(0))) {
            VersionTransformationStrategyFactory.printHelp();
            System.out.println("Required: -m <commit message>");
            return;
        }

        Entry<VersionTransformationStrategy, List<String>> entry = VersionTransformationStrategyFactory.parseArgs(args);
        args = entry.getValue();
        if (args.size() != 2 || "-m".equals(args.get(0)) == false) {
            throw fatal("Parameter -m <commit message> not found" + entry.getValue());
        }
        String commitMessage = args.get(1);

        VersionTransformationStrategy strategy = entry.getKey();

        FileTransformer fileTransformer = BumpSnapshotPatchTransformerFactory.create(strategy);
        new BumpSnapshotWorkflowTransformer(fileTransformer, commitMessage).run();
    }


    static class BumpSnapshotWorkflowTransformer {
        private final FileTransformer patchFileTransformer;
        private final String commitMessage;

        public BumpSnapshotWorkflowTransformer(FileTransformer patchFileTransformer, String commitMessage) {
            this.patchFileTransformer = patchFileTransformer;
            this.commitMessage = commitMessage;
        }

        public void run() throws Exception {
            File currentFolder = new File(".");
            GitPatchWorkflow workflow = new GitPatchWorkflow(patchFileTransformer, currentFolder, commitMessage);
            TransformFileResult result = workflow.doIt();
            System.out.println("Result: " + result);
        }

        static class GitPatchWorkflow {
            private final FileTransformer patchFileTransformer;
            private final File gitDir;
            private final String commitMessage;

            public GitPatchWorkflow(FileTransformer patchFileTransformer, File gitDir, String commitMessage) throws IOException {
                this.patchFileTransformer = patchFileTransformer;
                this.gitDir = gitDir;
                this.commitMessage = commitMessage;
            }

            public TransformFileResult doIt() throws IOException, GitAPIException, InterruptedException {
                System.out.println("1. revert release commit");
                runProcess(gitDir, Arrays.asList("git", "revert", "HEAD", "--no-edit")).getStdOut();
                System.out.println("2. create patch of this reverted commit");
                File originalPatch = getLastCommitAsPatch();
                File modifiedPatch = new File(originalPatch.getParentFile(),
                        originalPatch.getName().replaceAll(".patch", "-modified.patch"));
                Files.copy(originalPatch, modifiedPatch);
                System.out.println("3. drop that reverted commit");
                runProcess(gitDir, Arrays.asList("git", "reset", "--hard", "HEAD~1"));
                System.out.println("4. modify the patch file");
                TransformFileResult transformFileResult = patchFileTransformer.transformFile(modifiedPatch);
                System.out.println("5. apply and commit the patch as bump to new SNAPSHOT");
                runProcess(gitDir, Arrays.asList("git", "apply", modifiedPatch.getAbsolutePath()));
                runProcess(gitDir, Arrays.asList("git", "commit", "-am", commitMessage));
                checkState(originalPatch.delete());
                checkState(modifiedPatch.delete());
                return transformFileResult;
            }

            private File getLastCommitAsPatch() throws IOException, InterruptedException {
                String stdOut = runProcess(gitDir, Arrays.asList("git", "format-patch", "HEAD~1")).getStdOut();
                stdOut = stdOut.trim();
                return new File(gitDir, stdOut);
            }
        }
    }

}
