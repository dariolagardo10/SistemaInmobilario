package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageHelper {
    private static final String TAG = "ImageHelper";
    private static final int MAX_IMAGE_SIZE = 800; // tamaño máximo para redimensionar

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        // Calcular el tamaño inicial del bitmap
        int initialSize = bitmap.getWidth() * bitmap.getHeight() * 4; // Aproximación en bytes
        int quality = 80;

        // Para imágenes muy grandes, reducir calidad progresivamente
        if (initialSize > 4 * 1024 * 1024) quality = 50;
        if (initialSize > 8 * 1024 * 1024) quality = 30;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

        // Si el resultado es muy grande, redimensionar
        byte[] bytes = byteArrayOutputStream.toByteArray();
        if (bytes.length > 1024 * 1024) { // Si es mayor a 1MB
            // Redimensionar bitmap
            int scale = 2;
            Bitmap resized = Bitmap.createScaledBitmap(
                    bitmap,
                    bitmap.getWidth() / scale,
                    bitmap.getHeight() / scale,
                    true);

            byteArrayOutputStream.reset();
            resized.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            bytes = byteArrayOutputStream.toByteArray();
        }

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static List<String> urisToBase64List(Context context, List<Uri> uris) {
        List<String> base64Images = new ArrayList<>();

        for (Uri uri : uris) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Redimensionar la imagen si es necesario
                bitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

                // Convertir a base64
                String base64 = bitmapToBase64(bitmap);
                base64Images.add(base64);

            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error al abrir imagen: " + e.getMessage());
            }
        }

        return base64Images;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / (float) height;

        if (width > height) {
            if (width > maxSize) {
                width = maxSize;
                height = (int) (width / ratio);
            }
        } else {
            if (height > maxSize) {
                height = maxSize;
                width = (int) (height * ratio);
            }
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}