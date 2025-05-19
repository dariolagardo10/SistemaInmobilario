package es.rcti.demoprinterplus.sistemainmobilario;

public class PrinterHelper {
    public static class Commands {
        public static final byte[] ESC_INIT = {0x1B, 0x40};
        public static final byte[] LF = {0x0A};
        public static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
        public static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
        public static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};
        public static final byte[] FEED_LINE = {0x0A};
        public static final byte[] FEED_PAPER_AND_CUT = {0x1D, 0x56, 0x01};
        public static final byte[] TEXT_SIZE_NORMAL = {0x1B, 0x21, 0x00};
        public static final byte[] TEXT_SIZE_LARGE = {0x1B, 0x21, 0x30};
        public static final byte[] TEXT_FONT_A = {0x1B, 0x4D, 0x00};
        public static final byte[] TEXT_FONT_B = {0x1B, 0x4D, 0x01};
        public static final byte[] TEXT_BOLD_ON = {0x1B, 0x45, 0x01};
        public static final byte[] TEXT_BOLD_OFF = {0x1B, 0x45, 0x00};
        public static final byte[] TEXT_UNDERLINE_ON = {0x1B, 0x2D, 0x01};
        public static final byte[] TEXT_UNDERLINE_OFF = {0x1B, 0x2D, 0x00};
    }

    public static byte[] decodeBitmap(android.graphics.Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Redimensionar si es necesario
        if (width > 384) {
            float scale = 384f / width;
            int newHeight = (int)(height * scale);
            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 384, newHeight, true);
            width = 384;
            height = newHeight;
        }

        // Calcular ancho en bytes (8 píxeles = 1 byte)
        int widthBytes = (width + 7) / 8;
        int bufferSize = 8 + (widthBytes * height); // Cabecera + datos

        byte[] result = new byte[bufferSize];

        // Cabecera para comando de gráficos raster (GS v 0)
        result[0] = 0x1D; // GS
        result[1] = 0x76; // v
        result[2] = 0x30; // 0
        result[3] = 0x00; // normal

        // Ancho en bytes
        result[4] = (byte)(widthBytes % 256);
        result[5] = (byte)(widthBytes / 256);

        // Alto en píxeles
        result[6] = (byte)(height % 256);
        result[7] = (byte)(height / 256);

        // Convertir pixels a formato binario (0=blanco, 1=negro)
        int pos = 8;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < widthBytes; x++) {
                for (int b = 0; b < 8; b++) {
                    int pixelX = x * 8 + b;
                    byte bit = 0;

                    if (pixelX < width) {
                        int pixel = bitmap.getPixel(pixelX, y);
                        // Calcular luminosidad (0-255)
                        int luminance = (int)(0.299 * android.graphics.Color.red(pixel) +
                                0.587 * android.graphics.Color.green(pixel) +
                                0.114 * android.graphics.Color.blue(pixel));
                        // Blanco y negro (umbral 128)
                        bit = (luminance < 128) ? (byte)1 : (byte)0;
                    }

                    // Bit en posición correcta
                    result[pos] |= (bit << (7 - b));
                }
                pos++;
            }
        }

        return result;
    }
}
