package es.rcti.demoprinterplus.sistemainmobilario;



import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class OfflineFiles {

    // ✅ copia cualquier Uri (cámara o galería) a un archivo propio
    public static String saveImageToAppStorage(Context ctx, Uri srcUri, String actaLocalId) throws Exception {

        File dir = new File(ctx.getFilesDir(), "imagenes_inmo");
        if (!dir.exists()) dir.mkdirs();

        String name = "img_" + actaLocalId + "_" + System.currentTimeMillis() + ".jpg";
        File dst = new File(dir, name);

        try (InputStream in = ctx.getContentResolver().openInputStream(srcUri);
             OutputStream out = new FileOutputStream(dst)) {

            if (in == null) throw new Exception("No se pudo abrir InputStream del Uri");

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }

        return dst.getAbsolutePath();
    }
}

