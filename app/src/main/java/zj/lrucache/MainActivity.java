package zj.lrucache;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.BaseQuickAdapter;
import com.joanzapata.android.QuickAdapter;

import zj.lrucache.load.ImageLoader;
import zj.lrucache.load.ImageLoaderListener;
import zj.lrucache.res.Images;

public class MainActivity extends Activity {

    private ImageLoader imageLoader = null;
    private ListView mBitmapListView = null;
    private BaseQuickAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        imageLoader = new ImageLoader(MainActivity.this);

        mBitmapListView = (ListView) findViewById(R.id.bitmaplistview);

        mBitmapListView.setAdapter(mAdapter = new QuickAdapter<Bitmap>(this, R.layout.imgitemview) {

            @Override
            protected void convert(BaseAdapterHelper helper, Bitmap item) {
                helper.setImageBitmap(R.id.img_item,item);
            }

        });

        findViewById(R.id.bbt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (int i = 0; i < Images.imageThumbUrls.length; i++) {
                    imageLoader.loadImage(Images.imageThumbUrls[i], new ImageLoaderListener() {//imageLoader.loadImage包含了缓存的策略
                        @Override
                        public void onImageLoader(final Bitmap bitmap, String url) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(null != bitmap){
                                        mAdapter.add(bitmap);
                                    }
                                }
                            });
                        }
                    });
                }

            }

        });
    }

}
