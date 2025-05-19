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

import es.rcti.demoprinterplus.sistemainmobilario.R;

public class ParcelDetailActivity extends AppCompatActivity {

    private static final String TAG = "ParcelDetailActivity";
    private JSONObject parcelJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parcel_detail);

        TextView textView = findViewById(R.id.parcelDataTextView);
        Button btnGenerateActa = findViewById(R.id.btnGenerateActa);

        // Obtener datos de parcela
        String parcelData = getIntent().getStringExtra("PARCEL_DATA");

        if (parcelData == null || parcelData.isEmpty()) {
            textView.setText("No se recibieron datos de parcela");
            btnGenerateActa.setEnabled(false);
            return;
        }

        try {
            // Analizar datos JSON
            parcelJson = new JSONObject(parcelData);
            StringBuilder info = new StringBuilder();

            // Añadir campos al texto
            addFieldIfExists(info, parcelJson, "APYN", "Propietario");
            addFieldIfExists(info, parcelJson, "CALLE", "Calle");
            addFieldIfExists(info, parcelJson, "NRO", "Número");
            addFieldIfExists(info, parcelJson, "SEC", "Sección");
            addFieldIfExists(info, parcelJson, "CHA", "Chacra");
            addFieldIfExists(info, parcelJson, "MAN", "Manzana");
            addFieldIfExists(info, parcelJson, "PAR", "Parcela");
            addFieldIfExists(info, parcelJson, "LOTE", "Lote");
            addFieldIfExists(info, parcelJson, "PART", "Partida");

            textView.setText(info.toString());

            // Configurar botón para generar acta
            btnGenerateActa.setOnClickListener(v -> {
                Intent intent = new Intent(ParcelDetailActivity.this, ActaInfraccionActivity.class);
                intent.putExtra("PARCEL_DATA", parcelData);
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
}