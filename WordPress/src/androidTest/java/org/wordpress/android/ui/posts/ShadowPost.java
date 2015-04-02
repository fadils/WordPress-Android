package org.wordpress.android.ui.posts;

import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.wordpress.android.models.Post;

@Implements(Post.class)
public class ShadowPost {
    @RealObject private Post mPost;

    public ShadowPost() {

    }

    public Post getPost() {
        return mPost;
    }
}
