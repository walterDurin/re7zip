/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package readsevenzip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 *
 * @author wael
 */
public class HttpIInStream implements IInStream {

    URL url;
    URLConnection conexion;
    long length;
    InputStream stream;
    long position = 0;

    public HttpIInStream(String url) throws Exception {
        this.url = new URL(url);
        conexion = this.url.openConnection();
        length = Long.parseLong(conexion.getHeaderField("Content-Length"));
    }

    private void positionStream(long pos) throws Exception {

        if (position == pos) {
            return;
        }
        if (stream != null) {
            stream.close();
        }
        position = pos;
        if (pos == length) {
            return;
        }
        conexion = url.openConnection();
        conexion.setRequestProperty("Range", "bytes=" + pos + "-");
        conexion.connect();
        stream = conexion.getInputStream();
        position = pos;
    }

    /**
     * {@inheritDoc}
     */
    public long seek(long offset, int seekOrigin) throws SevenZipException {
        try {
            switch (seekOrigin) {
                case SEEK_SET:
                    positionStream(offset);
                    break;

                case SEEK_CUR:
                    positionStream(offset + position);
                    break;

                case SEEK_END:
                    positionStream(length + offset);
                    break;

                default:
                    throw new RuntimeException("Seek: unknown origin: " + seekOrigin);
            }
//            System.out.println("POINTER >> " + position);
            return position;
        } catch (Exception e) {
            throw new SevenZipException("Error while seek operation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] data) throws SevenZipException {
        try {
            int read = stream.read(data);
            if (read == -1) {
                System.out.println("Read Only");
                return 0;
            } else {
                position += read;
                return read;
            }
        } catch (IOException e) {
            throw new SevenZipException("Error reading random access file", e);
        }
    }

    /**
     * Closes random access file. After this call no more methods should be called.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        stream.close();
    }
}
