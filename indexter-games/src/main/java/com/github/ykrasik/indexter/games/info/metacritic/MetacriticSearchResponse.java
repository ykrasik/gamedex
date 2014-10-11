package com.github.ykrasik.indexter.games.info.metacritic;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticSearchResponse {
    public static class Result {
        private int score;
        private String rlsdate;
        private String name;
        private String rating;
        private String publisher;
        private String url;
        private String platform;

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getRlsdate() {
            return rlsdate;
        }

        public void setRlsdate(String rlsdate) {
            this.rlsdate = rlsdate;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }
    }

    private int count;
    private int maxPages;
    private List<Result> results;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }
}
