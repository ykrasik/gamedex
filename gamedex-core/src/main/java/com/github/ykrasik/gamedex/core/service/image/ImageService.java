package com.github.ykrasik.gamedex.core.service.image;

import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Yevgeny Krasik
 */
public interface ImageService {
    // TODO: Return an AsyncImage with a cancel() method on it? Less confusing.
    Task<Image> fetchThumbnail(Id<Game> id, ImageView imageView);

    Task<Image> fetchPoster(Id<Game> id, ImageView imageView);
}
