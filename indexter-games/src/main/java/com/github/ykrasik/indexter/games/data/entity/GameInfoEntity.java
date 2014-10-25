package com.github.ykrasik.indexter.games.data.entity;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.google.common.base.MoreObjects;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@DatabaseTable(tableName = "game_infos")
public class GameInfoEntity {
    @DatabaseField(id = true)
    private String name;

    @DatabaseField
    private String description;

    @DatabaseField(canBeNull = false)
    private GamePlatform gamePlatform;

    @DatabaseField
    private Date releaseDate;

    @DatabaseField(throwIfNull = true)
    private double criticScore;

    @DatabaseField(throwIfNull = true)
    private double userScore;

//    private List<String> genres;
//
//    private List<String> publishers;
//
//    private List<String> developers;

    @DatabaseField(canBeNull = false)
    private String url;

    @DatabaseField(dataType = DataType.BYTE_ARRAY, canBeNull = false)
    private byte[] thumbnailData;

    GameInfoEntity() {
        // For ORM.
    }

    public GameInfoEntity(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public GameInfoEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public GameInfoEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public GamePlatform getGamePlatform() {
        return gamePlatform;
    }

    public GameInfoEntity setGamePlatform(GamePlatform gamePlatform) {
        this.gamePlatform = gamePlatform;
        return this;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public GameInfoEntity setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
        return this;
    }

    public double getCriticScore() {
        return criticScore;
    }

    public GameInfoEntity setCriticScore(double criticScore) {
        this.criticScore = criticScore;
        return this;
    }

    public double getUserScore() {
        return userScore;
    }

    public GameInfoEntity setUserScore(double userScore) {
        this.userScore = userScore;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public GameInfoEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public GameInfoEntity setThumbnailData(byte[] thumbnailData) {
        this.thumbnailData = thumbnailData;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .toString();
    }
}
