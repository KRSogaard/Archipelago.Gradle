package build.archipelago.common.github.models.github;

import build.archipelago.common.git.models.GitCommit;
import lombok.Value;

import java.time.Instant;

@Value
public class GithubCommit {
    private String sha;
    private String node_id;
    private Commit commit;
    private String url;
    private String html_url;
    private String comments_url;
    private Parent[] parents;

    public static GitCommit toInternal(GithubCommit githubCommit) {
        return GitCommit.builder()
                .sha(githubCommit.getSha())
                .message(githubCommit.getCommit().getMessage())
                .author(githubCommit.getCommit().getAuthor().getName())
                .created(Instant.parse(githubCommit.getCommit().getAuthor().getDate()))
                .build();
    }

    public static class Commit {
        private String message;
        private String url;
        private Author author;
        private Author committer;
        private Integer comment_count;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(Author author) {
            this.author = author;
        }

        public Author getCommitter() {
            return committer;
        }

        public void setCommitter(Author committer) {
            this.committer = committer;
        }

        public Integer getComment_count() {
            return comment_count;
        }

        public void setComment_count(Integer comment_count) {
            this.comment_count = comment_count;
        }
    }

    public static class Author {
        private String name;
        private String email;
        private String date;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    public static class Parent {
        private String sha;
        private String url;
        private String html_url;

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getHtml_url() {
            return html_url;
        }

        public void setHtml_url(String html_url) {
            this.html_url = html_url;
        }
    }
}
