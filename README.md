# 什么是LruCache
- **算法的角度**
LRU(Least Recently Used)，即最近最少使用算法。

- **Android的角度**
LruCache是Android提供的一个缓存工具类

- **应用途径**
缓存策略的一种手段，我们都知道**三级缓存策略**，即：**内存—>本地（磁盘/sqlite）—>网络**。这里LruCache就是针对三级缓存中的第一级(内存)而提供的一个手段。

# LRU(Least Recently Used——最近最少使用算法)，LinkedHashMap
我对算法这块还不是很精通，最近最少使用算法可以参考这篇博客：
https://www.cnblogs.com/work115/p/5585341.html
大致的意思就是说，一个队列，当占满了指定的空间后，队头入队的新元素会致使队尾的元素出队以维持被设定的空间大小内存。

且LinkedHashMap则是java里实现了这个算法的实现类。而LinkedHashMap是HashMap的子类，它相对于HashMap来讲是**有序的**，并且能够按照访问顺序进行排序。

**LinkedHashMap有序的例子：**
```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
public class TestLinkedHashMap {
 
  public static void main(String args[])
  {
   System.out.println("*************************LinkedHashMap*************");
   Map<Integer,String> map = new LinkedHashMap<Integer,String>();
   map.put(6, "apple");
   map.put(3, "banana");
   map.put(2,"pear");
   
   for (Iterator it =  map.keySet().iterator();it.hasNext();)
   {
    Object key = it.next();
    System.out.println( key+"="+ map.get(key));
   }
   
   System.out.println("*************************HashMap*************");
   Map<Integer,String> map1 = new  HashMap<Integer,String>();
   map1.put(6, "apple");
   map1.put(3, "banana");
   map1.put(2,"pear");
   
   for (Iterator it =  map1.keySet().iterator();it.hasNext();)
   {
    Object key = it.next();
    System.out.println( key+"="+ map1.get(key));
   }
  }
}

```

**运行结果如下：**

**>>>>>>>>>>>>>>>>>>LinkedHashMap<<<<<<<<<<<<<<<<<<<<<**
6=apple
3=banana
2=pear
**>>>>>>>>>>>>>>>>>>HashMap<<<<<<<<<<<<<<<<<<<<<**
2=pear
6=apple
3=banana

**分析:**
LinkedHashmap 的特点是put进去的对象位置未发生变化,而HashMap会发生变化，因此可见LinkedHashMap是有序的。

# LruCache的应用：用LruCache来封装一个ImageLoader
ImageLoader图片加载一般都会涉及到三级缓存策略，那么我们就来手写一个ImageLoader，并用LruCache充当该ImageLoader的一级缓存吧。

## 1.需求分析
ImageLoader牵涉到的功能，在这里缕缕。

- 内存存储
LruCache用来作为三级缓存的第一级，内存缓存。

- 本地硬盘存储
封装一个FileUtil用于图片的本地图片的增删改查操作。

- 下载并存到缓存里(内存/硬盘)
封装一个DownLoader。
用到线程池。为了简单，我采用HttpUrlConnecttion最原始的方式。

## 2.代码编写

### 2.1:lruCache初始化:
```java
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
```

### 2.2:loadImage核心逻辑代码:
```java
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

        final String cacheName = handleUrl(url);
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
                        String fileName = handleUrl(url);

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

```

注释写的很清楚，具体就是。

首次进入调用load方法，这时候肯定是去下载，下载完毕之后放入内存和硬盘。

当下次取的时候，先在内存里读，内存里没有再去硬盘上读，硬盘上没有就去网络上下，下载好之后，并再次放入这俩个级的缓存。

### 2.3:一级缓存数据观察:
![](https://wx4.sinaimg.cn/mw690/0061ejqJgy1flypqig9r5g30a80hxnpd.gif)

### 2.4:二级缓存数据观察:
```java
root@android:/mnt/sdcard/AndroidImage # ls
httpimg5imgtnbdimgcomitu8076056792027849210fm27gp0jpg
httpstimgsabaiducomtimgimagequality80sizeb9999_10000sec1511352760558di77e465cd79
1fb53f907660a113e0ca4bimgtype0srchttp3A2F2Fimg05tooopencom2Fimages2F201404042Fsy
_58241958989jpg
```
**俩张图片分别是：**
httpimg5imgtnbdimgcomitu8076056792027849210fm27gp0jpg

httpstimgsabaiducomtimgimagequality80sizeb9999_10000sec1511352760558di77e465cd79
1fb53f907660a113e0ca4bimgtype0srchttp3A2F2Fimg05tooopencom2Fimages2F201404042Fsy
_58241958989jpg


这里缓存文件名是经过处理过的：
```java
    /**
     * 处理网址
     * @param url
     * @return
     */
    private String makeFileName(String url) {
        return url.replaceAll("[^\\w]", "");
    }
```

# Demo下载
[利用httpUrlConnection下载图片的Demo](https://github.com/zj614android/HttpUrlConnectionDownLoadDemo)
[本文的Demo](https://github.com/zj614android/LruCacheImageLoader)






