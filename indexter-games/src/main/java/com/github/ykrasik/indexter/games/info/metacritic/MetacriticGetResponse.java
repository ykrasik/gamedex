package com.github.ykrasik.indexter.games.info.metacritic;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticGetResponse {
    public static class Result {
        private int score;
        private String rlsdate;
        private String thumbnail;
        private String name;
        private String genre;
        private String rating;
        private double userScore;
        private String publisher;
        private String developer;
        private String platform;
        private String url;

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

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public String getRating() {
            return rating;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }

        public double getUserScore() {
            return userScore;
        }

        public void setUserScore(double userScore) {
            this.userScore = userScore;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getDeveloper() {
            return developer;
        }

        public void setDeveloper(String developer) {
            this.developer = developer;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    private Result result;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
