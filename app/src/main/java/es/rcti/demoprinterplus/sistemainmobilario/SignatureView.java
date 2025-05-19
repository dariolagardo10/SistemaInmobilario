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

        // Dibuja todos los trazos completados
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }

        // Dibuja el trazo actual en progreso
        canvas.drawPath(currentPath, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // INICIO DEL TRAZO: cuando el dedo toca la pantalla
                currentPath = new Path();
                currentPath.moveTo(x, y);
                isEmpty = false;
                if (signatureListener != null) {
                    signatureListener.onSignatureDrawn(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // CONTINUACIÓN DEL TRAZO: mientras el dedo se desliza
                currentPath.lineTo(x, y);
                break;

            case MotionEvent.ACTION_UP:
                // FINALIZACIÓN DEL TRAZO: cuando el dedo se levanta
                paths.add(currentPath);
                currentPath = new Path();
                break;
        }

        // Solicita redibujar la vista con el nuevo trazo
        invalidate();
        return true;
    }

    public void clear() {
        paths.clear();
        currentPath = new Path();
        isEmpty = true;
        if (signatureListener != null) {
            signatureListener.onSignatureDrawn(false);
        }
        invalidate();
    }

    public boolean hasSignature() {
        return !isEmpty;
    }

    public Bitmap getBitmap() {
        if (isEmpty) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // Dibuja todos los trazos en el bitmap
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }
        canvas.drawPath(currentPath, paint);

        return bitmap;
    }

    // Métodos adicionales para compatibilidad

    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
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

    // Métodos vacíos para compatibilidad

    public void setMinWidth(float minWidth) {
        // No utilizado
    }

    public void setMaxWidth(float maxWidth) {
        // No utilizado
    }

    public void setVelocityFilterWeight(float weight) {
        // No utilizado
    }

    public void setBackgroundColor(int color) {
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Solicitar que el padre no intercepte eventos táctiles
        getParent().requestDisallowInterceptTouchEvent(true);

        // Para mejor rendimiento en pantallas de alta densidad
        float density = getResources().getDisplayMetrics().density;
        event.setLocation(event.getX() * density, event.getY() * density);

        return super.dispatchTouchEvent(event);
    }
}