package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class ParcelDetailActivity extends AppCompatActivity {

    private static final String TAG = "ParcelDetailActivity";
    private JSONObject parcelJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_detail);

        TextView textView = findViewById(R.id.parcelDataTextView);
        Button btnGenerateActa = findViewById(R.id.btnGenerateActa);
        Button btnBackToMap = findViewById(R.id.btnBackToMap);
        android.widget.ImageButton btnBack = findViewById(R.id.btnBack);

        // 🔹 Botones de navegación atrás
        btnBackToMap.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());

        // ✅ Recuperar datos de la parcela
        String parcelData = getIntent().getStringExtra("PARCEL_DATA");

        // ✅ Recuperar datos del inspector desde el Intent
        String nombreInspector = getIntent().getStringExtra("NOMBRE_INSPECTOR");
        String apellidoInspector = getIntent().getStringExtra("APELLIDO_INSPECTOR");
        String legajoInspector = getIntent().getStringExtra("LEGAJO_INSPECTOR");
        String inspectorId = getIntent().getStringExtra("INSPECTOR_ID");

        Log.d(TAG, "✅ Datos del inspector recibidos en ParcelDetailActivity: "
                + nombreInspector + " " + apellidoInspector
                + " | Legajo: " + legajoInspector
                + " | ID: " + inspectorId);

        if (parcelData == null || parcelData.isEmpty()) {
            textView.setText("No se recibieron datos de parcela");
            btnGenerateActa.setEnabled(false);
            return;
        }

        try {
            // Analizar datos JSON
            parcelJson = new JSONObject(parcelData);
            android.text.SpannableStringBuilder info = new android.text.SpannableStringBuilder();

            // Añadir campos con formato
            addFieldWithFormat(info, parcelJson, "APYN", "Propietario");
            addFieldWithFormat(info, parcelJson, "CALLE", "Calle");
            addFieldWithFormat(info, parcelJson, "NRO", "Número");
            
            // Separador visual
            info.append("\n━━━━━━━━━━━━━━━━━\n\n");
            
            addFieldWithFormat(info, parcelJson, "SEC", "Sección");
            addFieldWithFormat(info, parcelJson, "CHA", "Chacra");
            addFieldWithFormat(info, parcelJson, "MAN", "Manzana");
            addFieldWithFormat(info, parcelJson, "PAR", "Parcela");
            addFieldWithFormat(info, parcelJson, "LOTE", "Lote");
            addFieldWithFormat(info, parcelJson, "PART", "Partida");

            textView.setText(info);

            // ✅ Configurar botón para generar acta
            btnGenerateActa.setOnClickListener(v -> {
                Intent intent = new Intent(ParcelDetailActivity.this, ActaInfraccionActivity.class);
                intent.putExtra("PARCEL_DATA", parcelData);

                // 🔹 Enviar también los datos del inspector al Acta
                intent.putExtra("NOMBRE_INSPECTOR", nombreInspector);
                intent.putExtra("APELLIDO_INSPECTOR", apellidoInspector);
                intent.putExtra("LEGAJO_INSPECTOR", legajoInspector);
                intent.putExtra("INSPECTOR_ID", inspectorId);

                Log.d(TAG, "📤 Enviando datos del inspector al Acta: " + legajoInspector);
                startActivity(intent);
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error al procesar JSON", e);
            textView.setText("Error al procesar datos: " + e.getMessage());
            btnGenerateActa.setEnabled(false);
            Toast.makeText(this, "Error al procesar los datos de la parcela", Toast.LENGTH_SHORT).show();
        }
    }

    private void addFieldIfExists(StringBuilder info, JSONObject json, String key, String label) {
        try {
            if (json.has(key) && !json.isNull(key)) {
                String value = json.getString(key);
                if (value != null && !value.isEmpty()) {
                    info.append(label).append(": ").append(value).append("\n\n");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error al leer campo " + key, e);
        }
    }
    
    private void addFieldWithFormat(android.text.SpannableStringBuilder builder, JSONObject json, String key, String label) {
        try {
            if (json.has(key) && !json.isNull(key)) {
                String value = json.getString(key);
                if (value != null && !value.isEmpty()) {
                    // Añadir label (gris, pequeño)
                    int labelStart = builder.length();
                    builder.append(label).append("\n");
                    int labelEnd = builder.length();
                    
                    builder.setSpan(new android.text.style.ForegroundColorSpan(0xFF666666), 
                                   labelStart, labelEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new android.text.style.RelativeSizeSpan(0.85f), 
                                   labelStart, labelEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    // Añadir valor (negro, grande, negrita)
                    int valueStart = builder.length();
                    builder.append(value).append("\n\n");
                    int valueEnd = builder.length() - 2;
                    
                    builder.setSpan(new android.text.style.ForegroundColorSpan(0xFF222222), 
                                   valueStart, valueEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 
                                   valueStart, valueEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new android.text.style.RelativeSizeSpan(1.15f), 
                                   valueStart, valueEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error al leer campo " + key, e);
        }
    }
}
