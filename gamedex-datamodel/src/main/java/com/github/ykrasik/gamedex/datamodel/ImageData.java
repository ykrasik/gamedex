package com.github.ykrasik.gamedex.datamodel;

import javafx.scene.image.Image;
import lombok.*;

import java.io.ByteArrayInputStream;

/**
 * @author Yevgeny Krasik
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"rawData", "image"})
public class ImageData {
    @NonNull private final byte[] rawData;
    @NonNull private final Image image;

    public static ImageData of(byte[] rawData) {
        // There is no need to close byte array input streams.
        return new ImageData(rawData, new Image(new ByteArrayInputStream(rawData)));
    }
}
