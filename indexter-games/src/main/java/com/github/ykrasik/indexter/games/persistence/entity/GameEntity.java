package com.github.ykrasik.indexter.games.persistence.entity;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.persistence.dao.GameDaoImpl;
import com.google.common.base.MoreObjects;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * @author Yevgeny Krasik
 */
@DatabaseTable(tableName = "games", daoClass = GameDaoImpl.class)
public class GameEntity {
    public static final String PATH_COLUMN_NAME = "path";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(unique = true, canBeNull = false, columnName = PATH_COLUMN_NAME)
    private String path;

    @DatabaseField(canBeNull = false)
    private String name;

    @DatabaseField(width = 4000)
    private String description;

    @DatabaseField(canBeNull = false)
    private GamePlatform platform;

    @DatabaseField
    private Date releaseDate;

    @DatabaseField
    private Double criticScore;

    @DatabaseField
    private Double userScore;

    @DatabaseField
    private String genres;

    @DatabaseField
    private String giantBombApiDetailUrl;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] thumbnailData;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] posterData;

    // FIXME: Add URL

    @DatabaseField(version = true, dataType = DataType.DATE_LONG)
    private Date lastModified;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GamePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(GamePlatform platform) {
        this.platform = platform;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Double getCriticScore() {
        return criticScore;
    }

    public void setCriticScore(Double criticScore) {
        this.criticScore = criticScore;
    }

    public Double getUserScore() {
        return userScore;
    }

    public void setUserScore(Double userScore) {
        this.userScore = userScore;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getGiantBombApiDetailUrl() {
        return giantBombApiDetailUrl;
    }

    public void setGiantBombApiDetailUrl(String giantBombApiDetailUrl) {
        this.giantBombApiDetailUrl = giantBombApiDetailUrl;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    public byte[] getPosterData() {
        return posterData;
    }

    public void setPosterData(byte[] posterData) {
        this.posterData = posterData;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("path", path)
            .add("name", name)
            .toString();
    }
}
