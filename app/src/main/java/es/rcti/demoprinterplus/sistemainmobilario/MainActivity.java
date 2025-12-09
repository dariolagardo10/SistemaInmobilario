package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "MainActivity";

    // 🔹 Variables globales para mantener los datos del inspector
    private String nombreInspector;
    private String apellidoInspector;
    private String legajoInspector;
    private String inspectorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Recuperar datos del login
        Intent loginIntent = getIntent();
        nombreInspector = loginIntent.getStringExtra("NOMBRE_INSPECTOR");
        apellidoInspector = loginIntent.getStringExtra("APELLIDO_INSPECTOR");
        legajoInspector = loginIntent.getStringExtra("LEGAJO_INSPECTOR");
        inspectorId = loginIntent.getStringExtra("INSPECTOR_ID");

        Log.d(TAG, "✅ Datos del inspector cargados en MainActivity: " +
                nombreInspector + " " + apellidoInspector + " | Legajo: " + legajoInspector +
                " | ID: " + inspectorId);

        webView = findViewById(R.id.webView);
        setupWebView();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        WebView.setWebContentsDebuggingEnabled(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Página cargada exitosamente");
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class WebAppInterface {

        @JavascriptInterface
        public String getGeoJsonData(String section) {
            Log.d(TAG, "Solicitando GeoJSON para sección: " + section);
            try {
                return loadGeoJsonFromAssets("SEC_" + section + ".geojson");
            } catch (Exception e) {
                Log.e(TAG, "Error al cargar GeoJSON: " + e.getMessage(), e);
                return "{}";
            }
        }

        @JavascriptInterface
        public void sendParcelData(String parcelData) {
            Log.d(TAG, "Datos de parcela recibidos: " + parcelData);

            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, ParcelDetailActivity.class);
                intent.putExtra("PARCEL_DATA", parcelData);

                // ✅ Pasar también los datos del inspector al detalle de la parcela
                intent.putExtra("NOMBRE_INSPECTOR", nombreInspector);
                intent.putExtra("APELLIDO_INSPECTOR", apellidoInspector);
                intent.putExtra("LEGAJO_INSPECTOR", legajoInspector);
                intent.putExtra("INSPECTOR_ID", inspectorId);

                startActivity(intent);
            });
        }
    }

    private String loadGeoJsonFromAssets(String fileName) {
        StringBuilder jsonString = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName)));

            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line).append("\n");
            }
            reader.close();
            return jsonString.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error al leer archivo " + fileName, e);
            return "{}";
        }
    }
}
