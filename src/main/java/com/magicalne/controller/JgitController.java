package com.magicalne.controller;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Author: zehui.lv on 4/27/17.
 */
@RestController
public class JgitController {

    private final URL repoUrl = getClass().getClassLoader().getResource("repo");
    private final String repoPath = (repoUrl != null ? repoUrl.getPath() : null) + "/files-only";
    @RequestMapping(value = "/hello")
    public String hello() {
        return "hello";
    }

    @RequestMapping(value = "/clone")
    public boolean cloneRepo() {
        final String remoteRepoPath = "https://github.com/magicalne/files-only.git";
        try {
            Git
                    .cloneRepository()
                    .setURI(remoteRepoPath)
                    .setDirectory(new File(repoPath))
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @RequestMapping(value = "/list")
    public String listFiles() throws IOException {
        return Files.list(Paths.get(repoPath))
                .map(String::valueOf)
                .filter(path -> !path.startsWith("."))
                .sorted()
                .collect(Collectors.joining("; "));
    }

    @RequestMapping(value = "/get/{fileName}")
    public String getFile(@PathVariable String fileName) throws IOException {

        return Files.lines(Paths.get(repoPath + '/' + fileName), StandardCharsets.UTF_8)
                .reduce("\r", (a, b) -> a + b);
    }

    @RequestMapping(value = "/commit/{fileName}", method = RequestMethod.PUT)
    public String commitAndPush(@RequestBody String change, @PathVariable String fileName)
            throws IOException, GitAPIException {
        final List<String> lines = Collections.singletonList(change);
        Files.write(Paths.get(repoPath + '/' + fileName), lines, StandardCharsets.UTF_8);
        final Git repo = new Git(new FileRepositoryBuilder().findGitDir(new File(repoPath)).build());
        repo.commit().setAuthor("zehui.lv", "magicalne@gmail.com");
        repo.add().addFilepattern(fileName).call();
        repo.commit().setMessage("Make changes on file: " + fileName).call();
        repo.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider("magicalne", "qwe4416736")).call();
        return getFile(fileName);
    }

    @RequestMapping(value = "/pull")
    public void pull() throws IOException, GitAPIException {
        final Git repo = new Git(new FileRepositoryBuilder().findGitDir(new File(repoPath)).build());
        repo.pull().setRebase(true).call();
    }

    @RequestMapping(value = "/checkout/{fileName}/{commitId}")
    public String checkoutFile(@PathVariable String fileName, @PathVariable String commitId)
            throws IOException, GitAPIException {
        final Git repo = new Git(new FileRepositoryBuilder().findGitDir(new File(repoPath)).build());
        final Ref ref = repo.checkout().addPath(fileName).setName(commitId).call();
        return ref.getName();
    }

    @RequestMapping(value = "/git-log")
    public List<String> gitLog() throws IOException, GitAPIException {
        final Git repo = new Git(new FileRepositoryBuilder().findGitDir(new File(repoPath)).build());

        final Iterable<RevCommit> iterable = repo.log().call();
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(commit -> {
                    final PersonIdent authorIdent = commit.getAuthorIdent();
                    final String fullMessage = commit.getFullMessage();
                    final String commitId = commit.getName();
                    return authorIdent.toString() + ' ' + fullMessage + ' ' + commitId;
                })
                .collect(Collectors.toList());
    }
}
