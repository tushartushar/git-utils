package java.org.example;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Commits {
    String repoPath;
    int curCounter;
    Git git;
    Repository repository;
    List<RevCommit> commits = new ArrayList<>();
    HashMap<String, RevCommit> commitMap = new HashMap<>();
    private int totalCommits = 0;

    public static List<String> getAllBranches(String repoPath) {
        List<String> branches = new ArrayList<>();
        try (Git git = Git.open(new File(repoPath))) {
            List<Ref> branchRefs = git.branchList().call();
            for (Ref branchRef : branchRefs) {
                branches.add(branchRef.getName());
            }
        } catch (IOException ex) {
            System.exit(6);
        } catch (GitAPIException ex) {
            System.exit(7);
        }
        return branches;
    }

    public Commits(String repoPath, String branchName) {
        curCounter = 0;
        try {
            this.repoPath = repoPath;
            git = Git.open(new File(repoPath));
            repository = git.getRepository();

            validateGitInstallation();
            validateBranch(repoPath, branchName);
            populateCommits(branchName);

        } catch (IOException ex) {
            System.exit(6);
        } catch (GitAPIException ex) {
            System.exit(7);
        }
    }

    public static void validateGitInstallation() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("git", "--version");
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode > 0) {
                System.exit(11);
            }
        } catch (IOException | InterruptedException ex) {
        }
    }

    public static String getLatestCommitHash(String repoPath) {
        Path path = Paths.get(repoPath, ".git");
        String command = "git " + "--git-dir " + path + " rev-parse HEAD";
        //Logger.log(command);
        try {
            Process process = Runtime.getRuntime().exec(command);
            return getHash(process);
        } catch (IOException e) {
            return "";
        }
    }

    private static String getHash(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        if (line != null) {
            return line.strip();
        }
        return "";
    }


    private void validateBranch(String repoPath, String branchName) {
        List<String> allBranches = getAllBranches(repoPath);
        if (!allBranches.contains(branchName)) {
            System.exit(10);
        }
    }

    private void populateCommits(String branch_tree) throws GitAPIException, IOException {
        int commitCount = 0;
        for (RevCommit commit : git.log().add(repository.resolve(branch_tree)).call()) {
            commitCount += 1;
            commits.add(commit);
            commitMap.put(commit.getName(), commit);
        }
        this.totalCommits = commitCount;
        Collections.reverse(commits);
    }

    private int getCommitIndex(String commitHash, boolean direction) {
        int index = (direction) ? 0 : totalCommits - 1;
        if (commitHash != null) {
            RevCommit commit = commitMap.get(commitHash);
            if (commit == null)
                index = (direction) ? 0 : totalCommits - 1;
            else {
                index = commits.indexOf(commit);
                if (index < 0)
                    index = 0;
                if (index >= totalCommits)
                    index = totalCommits - 1;
            }
        }
        return index;
    }

    public int getTotalCommits() {
        return totalCommits;
    }

    public static String copyProjectToTempDir(String sourceFolderPath, String projectName, String tempLocation) {
        String temporaryFolderLocationFromSystem = getTempFolder(tempLocation);

        File srcDir = new File(sourceFolderPath);
        File destDir = new File(temporaryFolderLocationFromSystem, projectName);
        try {
            if (destDir.exists()) {
                FileUtils.deleteDirectory(destDir);
            }
            FileUtils.copyDirectory(srcDir, destDir);
        } catch (IOException e) {
        }
        return destDir.getAbsolutePath();
    }
    private static String getTempFolder(String tempLocation) {
        String tempDir = tempLocation;
        if (tempDir == null)
            tempDir = System.getProperty("java.io.tmpdir");
        else
            createTemp(tempDir);
        return tempDir;
    }
    private static void createTemp(String tempDir) {
        File f = new File(tempDir);
        if (f.exists() && f.isDirectory()) {
            return;
        }
        try{
            if (!f.mkdirs()){
                System.exit(12);
            }
        }
        catch (SecurityException ex){
            System.exit(13);
        }
    }
}