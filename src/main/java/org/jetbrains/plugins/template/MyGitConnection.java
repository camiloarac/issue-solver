package org.jetbrains.plugins.template;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;

public class MyGitConnection {
    private GitHub github;
    private GHRepository repo;
    public MyGitConnection() {
        try {
            github = new GitHubBuilder().withOAuthToken("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","camiloarac").build();
            repo = github.getRepository("camiloarac/issue-solver");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public GHRepository getRepo() {
        return repo;
    }

}
