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
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(unique = true, canBeNull = false)
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
//
//    private List<String> publishers;
//
//    private List<String> developers;

    @DatabaseField
    private String giantBombApiDetailUrl;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] thumbnailData;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] posterData;

    GameInfoEntity() {
        // For ORM.
    }

    public GameInfoEntity(String path,
                          String name,
                          String description,
                          GamePlatform platform,
                          Date releaseDate,
                          Double criticScore,
                          Double userScore,
                          String genres,
                          String giantBombApiDetailUrl,
                          byte[] thumbnailData,
                          byte[] posterData) {
        this.path = Objects.requireNonNull(path);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.platform = Objects.requireNonNull(platform);
        this.releaseDate = releaseDate;
        this.criticScore = criticScore;
        this.userScore = userScore;
        this.genres = genres;
        this.giantBombApiDetailUrl = giantBombApiDetailUrl;
        this.thumbnailData = thumbnailData;
        this.posterData = posterData;
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GamePlatform getPlatform() {
        return platform;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public Double getCriticScore() {
        return criticScore;
    }

    public Double getUserScore() {
        return userScore;
    }

    public String getGenres() {
        return genres;
    }

    public String getGiantBombApiDetailUrl() {
        return giantBombApiDetailUrl;
    }

    public byte[] getThumbnailData() {
        return thumbnailData;
    }

    public byte[] getPosterData() {
        return posterData;
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
