package zj.lrucache.load;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.R.id.input;

/**
 * Created by Administrator on 2017/5/31.
 */

public class DownLoader {

    /**
     * 下载Image的线程池
     */
    private ExecutorService mImageThreadPool = null;

    public DownLoader() {
        super();
        init();
    }

    private void init() {
        //线程池初始化
        mImageThreadPool = Executors.newFixedThreadPool(5);
    }


    /**
     * 下载图片
     * @param url
     * @param listener
     */
    public void downLoadBitmap(final String url, final ImageLoaderListener listener) {
        mImageThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = getPicture(url);
                listener.onImageLoader(bitmap, url);
            }
        });
    }

    /**
     * 从Url中获取Bitmap
     *
     * @param url
     * @return
     */
    private Bitmap downLoadBitmapTask(String url) {

        Bitmap bitmap = null;
        HttpURLConnection con = null;
        try {
//                        // 创建URL
//                        URL url = new URL(urlPath);
//                        Log.i(TAG, "startDowload  url -------->" + url);
//                        // 创建HttpURLConnection对象
//                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                        // 设置连接方式
//                        conn.setRequestMethod("GET");
//                        // 设置连接超时
//                        conn.setConnectTimeout(3000);
//                        Log.i(TAG, "startDowload  conn.getResponseCode() -------->"+ conn.getResponseCode());
//                        Log.i(TAG, "startDowload  conn -------->" + conn);

            URL mImageUrl = new URL(url);
            con = (HttpURLConnection) mImageUrl.openConnection();
            con.setConnectTimeout(10 * 1000);
            con.setReadTimeout(10 * 1000);
//            con.setDoInput(true);
//            con.setDoOutput(true);
            InputStream inputStream = con.getInputStream();
            con.setRequestMethod("GET");
            if(null != inputStream){
//                new PatchInputStream(input)
                bitmap = BitmapFactory.decodeStream(new PatchInputStream(con.getInputStream()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return bitmap;
    }


    /**
     * 获取图片 返回Bitmap
     * @return
     */
    private Bitmap getPicture(String transUrl) {

        URL urlObj = null;
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            //构建图片的url地址
            urlObj = new URL(transUrl);

            //开启连接
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            //设置超时的时间，5000毫秒即5秒
            conn.setConnectTimeout(5000);

            //设置获取图片的方式为GET
            conn.setRequestMethod("GET");

            //响应码为200，则访问成功
            if (conn.getResponseCode() == 200) {
                //获取连接的输入流，这个输入流就是图片的输入流
                is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //在最后，将各种流关闭
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
//                handler.sendEmptyMessage(LOAD_ERROR);
                e.printStackTrace();
            }
        }

        return null;
    }

}
