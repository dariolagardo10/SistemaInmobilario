package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class SignatureView extends View {
    private ArrayList<Path> paths;
    private Path currentPath;
    private Paint paint;
    private boolean isEmpty = true;
    private Bitmap savedBitmap = null;

    // Variables para rastrear el último punto
    private float lastTouchX;
    private float lastTouchY;

    // Para compatibilidad con tu interfaz
    public interface OnSignatureListener {
        void onSignatureDrawn(boolean hasSignature);
    }

    private OnSignatureListener signatureListener;

    public SignatureView(Context context) {
        super(context);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignatureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paths = new ArrayList<>();
        currentPath = new Path();
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public void setOnSignatureListener(OnSignatureListener listener) {
        this.signatureListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fondo blanco
        canvas.drawColor(Color.WHITE);

        // Si hay un bitmap guardado, dibújalo primero
        if (savedBitmap != null) {
            canvas.drawBitmap(savedBitmap, 0, 0, null);
        }

        // Dibuja todos los trazos completados
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }

        // Dibuja el trazo actual en progreso
        canvas.drawPath(currentPath, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Solicitar que el padre no intercepte eventos táctiles
        getParent().requestDisallowInterceptTouchEvent(true);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // INICIO DEL TRAZO: cuando el dedo toca la pantalla
                currentPath = new Path();
                currentPath.moveTo(x, y);
                lastTouchX = x;
                lastTouchY = y;
                isEmpty = false;
                if (signatureListener != null) {
                    signatureListener.onSignatureDrawn(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // CONTINUACIÓN DEL TRAZO: mientras el dedo se desliza
                // Verificar si la distancia es suficiente antes de dibujar
                float dx = Math.abs(x - lastTouchX);
                float dy = Math.abs(y - lastTouchY);

                if (dx >= 2 || dy >= 2) {
                    currentPath.lineTo(x, y);
                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;

            case MotionEvent.ACTION_UP:
                // FINALIZACIÓN DEL TRAZO: cuando el dedo se levanta
                currentPath.lineTo(x, y);  // Asegurar que se dibuje el punto final
                paths.add(currentPath);
                currentPath = new Path();
                break;

            default:
                return false;
        }

        // Solicita redibujar la vista con el nuevo trazo
        invalidate();
        return true;
    }

    public void clear() {
        paths.clear();
        currentPath = new Path();
        isEmpty = true;
        savedBitmap = null;
        if (signatureListener != null) {
            signatureListener.onSignatureDrawn(false);
        }
        invalidate();
    }

    public boolean hasSignature() {
        return !isEmpty || savedBitmap != null;
    }

    public Bitmap getBitmap() {
        if (isEmpty && savedBitmap == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // Si hay un bitmap guardado, dibújalo primero
        if (savedBitmap != null) {
            canvas.drawBitmap(savedBitmap, 0, 0, null);
        }

        // Dibuja todos los trazos en el bitmap
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }
        canvas.drawPath(currentPath, paint);

        return bitmap;
    }

    // Método corregido para establecer un bitmap existente
    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            // Redimensionar el bitmap si es necesario
            if (bitmap.getWidth() != getWidth() || bitmap.getHeight() != getHeight()) {
                float scale = Math.min(
                        (float) getWidth() / bitmap.getWidth(),
                        (float) getHeight() / bitmap.getHeight()
                );

                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);

                this.savedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            } else {
                this.savedBitmap = bitmap;
            }

            isEmpty = false;
            invalidate();
        }
    }

    public byte[] getBytes() {
        Bitmap bitmap = getBitmap();
        if (bitmap == null) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    // Métodos de configuración
    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }

    public void setPenColor(int color) {
        paint.setColor(color);
    }

    // Métodos para configuración de sensibilidad
    public void setMinWidth(float minWidth) {
        // Implementar si se necesita
    }

    public void setMaxWidth(float maxWidth) {
        // Implementar si se necesita
    }

    public void setVelocityFilterWeight(float weight) {
        // Implementar si se necesita
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        invalidate();
    }
}