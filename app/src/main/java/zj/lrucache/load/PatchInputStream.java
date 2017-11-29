package zj.lrucache.load;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by thinkpad on 2017/11/28.
 */

public class PatchInputStream extends FilterInputStream {
    protected PatchInputStream(InputStream in) {
        super(in);
        // TODO Auto-generated constructor stub
    }

    public long skip(long n) throws IOException {
        long m = 0l;
        while (m < n) {
            long _m = in.skip(n - m);
            if (_m == 0l) {
                break;
            }
            m += _m;
        }
        return m;
    }
}
