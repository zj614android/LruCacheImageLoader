package zj.lrucache.load;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * 自己封装的自带缓存的ImageLoader类
 */
public class ImageLoader {

    /**
     * 缓存Image的类，当存储Image的大小大于LruCache设定的值，系统自动释放内存
     */
    private LruCache<String, Bitmap> mMemoryCache = null;//LruCache缓存实例
    private FileUtil mFileUtil;//文件管理工具类对象  增删改查
    private DownLoader mDownLoader;//图片加载器
    private Context mContext = null;//上下文
    private Handler lruCacheSaveHandler = null;//LruCache缓存的handler

    /**
     * 构造函数
     */
    public ImageLoader(Context context) {

        mContext = context;
        initLruCacheSpace();
        initUtil();

//        Looper.prepare();

        lruCacheSaveHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.e("Lru", "ImageLoader lruCacheSaveHandler handleMessage");
                ImageHandlerBean imb = (ImageHandlerBean) msg.obj;
                mMemoryCache.put(imb.urlname, imb.image);
            }
        };
//        Looper.loop();

    }

    /**
     * 初始化图片加载工具类
     */
    private void initUtil() {
        mFileUtil = new FileUtil(mContext);
        mDownLoader = new DownLoader();
    }

    /**
     * 初始化LruCache的内存大小
     */
    private void initLruCacheSpace() {
        //获取系统分配给每个应用程序的最大内存，每个应用系统分配32M（3.0之前）
        int maxMemory = (int) Runtime.getRuntime().maxMemory();

        //给LruCache分配1/8 4M
        int mCacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    /**
     * 核心方法
     * 先从内存缓存中获取Bitmap,如果没有就从SD卡或者手机缓存中获取，SD卡或者手机缓存
     * 没有就去下载
     *
     * @param url
     * @param resultListener
     * @return
     */
    public void loadImage(String url, final ImageLoaderListener resultListener) {

        final String cacheName = makeFileName(url);
        Log.e("Lru","     replaceUrl  == " + cacheName);
        String path = mFileUtil.getStorageDirectory();
        Log.e("Lru", "File path == " + path);

        //先去内存中拿  >>> 一级缓存
        Bitmap bitmap = loadFromMem(cacheName);

        //若内存中没有，就去磁盘上拿  >>> 二级缓存
        if (bitmap == null) {
            bitmap = loadFromDisk(cacheName);
        } else {
            resultListener.onImageLoader(bitmap, cacheName);
            Toast.makeText(mContext,"From > 一级缓存（内存）>  >> cacheName == " + cacheName,Toast.LENGTH_SHORT).show();
            return;
        }

        //若磁盘上没有就去网络上下载，并存入内存和磁盘  >>> 三级伪缓存
        if (bitmap == null) {
            Toast.makeText(mContext, "From > 三级缓存（伪缓存）> 去网络上下载  >>  cacheName == " + cacheName, Toast.LENGTH_SHORT).show();
            mDownLoader.downLoadBitmap(url, new ImageLoaderListener() {

                @Override
                public void onImageLoader(Bitmap bitmap, String url) {
                    if (null != bitmap) {
                        String fileName = makeFileName(url);

                        /**
                         * 放入内存 这儿因为是在子线程 所以不能直接对全局变量赋值
                         * */
                        //mMemoryCache.put(handleUrlName, bitmap);
                        //必须要切到主线程去队全局Cache赋值
                        saveToLruCache(bitmap, url);

                        //存入磁盘
                        try {
                            if (!mFileUtil.isFileExists(fileName)) {
                                mFileUtil.saveToDisk(fileName, bitmap);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        resultListener.onImageLoader(bitmap, cacheName);
                        return;
                    }
                }
            });
        } else {
            Toast.makeText(mContext,"From > 二级缓存（磁盘）   >> " + cacheName,Toast.LENGTH_SHORT).show();
            Log.e("Lru", "loadFromDisk not null");
            Log.e("Lru", "-------------------");
            resultListener.onImageLoader(bitmap, cacheName);
            return;
        }
    }

    /**
     * 存储到Lru内存里去
     * @param bitmap
     * @param url
     */
    private void saveToLruCache(Bitmap bitmap, String url) {
        Message message = lruCacheSaveHandler.obtainMessage();
        ImageHandlerBean imageHandlerBean = new ImageHandlerBean();
        imageHandlerBean.image = bitmap;
        imageHandlerBean.urlname = makeFileName(url);
        message.obj = imageHandlerBean;
        lruCacheSaveHandler.sendMessage(message);
    }


    /**
     * 处理网址
     * @param url
     * @return
     */
    private String makeFileName(String url) {
        return url.replaceAll("[^\\w]", "");
    }

    /**
     * 去磁盘上拿
     *
     * @param bitmapPath
     * @return
     */
    private Bitmap loadFromDisk(String bitmapPath) {

        if (TextUtils.isEmpty(bitmapPath)) {
            return null;
        }

        Bitmap bitmap = null;
        Log.e("Lru", "loadFromDisk");
        if (mFileUtil.isFileExists(bitmapPath)) {
            bitmap = mFileUtil.getBitmap(bitmapPath);
        }

        return bitmap;
    }

    /**
     * 去内存中拿
     *
     * @param bitmapKey
     * @return
     */
    private Bitmap loadFromMem(String bitmapKey) {
        if (TextUtils.isEmpty(bitmapKey)) {
            return null;
        }
        Log.e("Lru", "loadFromMem");
        return mMemoryCache.get(bitmapKey);
    }

    class ImageHandlerBean {
        public Bitmap image = null;
        public String urlname = null;
    }

}
