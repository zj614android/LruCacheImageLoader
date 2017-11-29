package zj.lrucache.load;

import android.graphics.Bitmap;

/**
 * Created by Administrator on 2017/5/31.
 */

public interface ImageLoaderListener {

    void onImageLoader(Bitmap bitmap, String url);

}
