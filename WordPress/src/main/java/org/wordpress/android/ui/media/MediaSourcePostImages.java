package org.wordpress.android.ui.media;

import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.mediapicker.MediaItem;

import java.util.ArrayList;

public class MediaSourcePostImages extends MediaSourceWPImages {
    public MediaSourcePostImages(ArrayList<WPImageSpan> imageSource) {
        for (WPImageSpan image : imageSource) {
            MediaItem mediaItem = new MediaItem();
            mediaItem.setSource(image.getImageSource());
            getmMediaItems().add(mediaItem);
        }
    }
}
