package es.rcti.demoprinterplus.sistemainmobilario;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import es.rcti.demoprinterplus.sistemainmobilario.R;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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