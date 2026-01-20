package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ContentUri {

    public static byte[] readAllBytes(Context ctx, Uri uri) throws Exception {
        if (ctx == null) throw new IllegalArgumentException("Context null");
        if (uri == null) throw new IllegalArgumentException("Uri null");

        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try {
            is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) throw new Exception("No se pudo abrir el InputStream: " + uri);

            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();

        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
            try { if (baos != null) baos.close(); } catch (Exception ignored) {}
        }
    }
}
