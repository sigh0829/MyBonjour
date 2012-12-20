package com.audric.bonjour;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * class found on google group.
 * defaut input stream throws error with some jpeg image& BitmapFactory.decodeStream.
 * this class patch default input stream, and works !
 *  
 * @author Audric Ackermann
 *
 */
public class PatchInputStream extends FilterInputStream {
	public PatchInputStream(InputStream in) {
		super(in);
	}
	public long skip(long n) throws IOException {
		long m = 0L;
		while (m < n) {
			long _m = in.skip(n-m);
			if (_m == 0L) break;
			m += _m;
		}
		return m;
	}
}