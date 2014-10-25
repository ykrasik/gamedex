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

    @DatabaseField
    private String url;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] thumbnailData;

    GameInfoEntity() {
        // For ORM.
    }

    public GameInfoEntity(String name,
                          String description,
                          GamePlatform gamePlatform,
                          Date releaseDate,
                          double criticScore,
                          double userScore,
                          String url,
                          byte[] thumbnailData) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.gamePlatform = Objects.requireNonNull(gamePlatform);
        this.releaseDate = releaseDate;
        this.criticScore = criticScore;
        this.userScore = userScore;
        this.url = url;
        this.thumbnailData = thumbnailData;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GamePlatform getGamePlatform() {
        return gamePlatform;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public double getCriticScore() {
        return criticScore;
    }

    public double getUserScore() {
        return userScore;
    }

    public String getUrl() {
        return url;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .toString();
    }
}
