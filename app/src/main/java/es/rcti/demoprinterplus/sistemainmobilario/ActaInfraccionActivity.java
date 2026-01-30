package es.rcti.demoprinterplus.sistemainmobilario;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import retrofit2.Callback;
import retrofit2.Response;
import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.material.textfield.TextInputLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import es.rcti.demoprinterplus.sistemainmobilario.R;
import es.rcti.demoprinterplus.sistemainmobilario.adapters.ImageAdapter;
import es.rcti.demoprinterplus.sistemainmobilario.ActaInfraccionData;
import es.rcti.demoprinterplus.sistemainmobilario.ApiClient;
import es.rcti.demoprinterplus.sistemainmobilario.utils.BluetoothPrinterConnector;
import es.rcti.demoprinterplus.sistemainmobilario.ImageHelper;
import es.rcti.demoprinterplus.sistemainmobilario.OracleApiService;
import es.rcti.demoprinterplus.sistemainmobilario.PrinterHelper;
import es.rcti.demoprinterplus.sistemainmobilario.RespuestaSubirFirma;
import es.rcti.demoprinterplus.sistemainmobilario.RespuestaSubirImagen;
import es.rcti.demoprinterplus.sistemainmobilario.RetrofitClient;
import es.rcti.demoprinterplus.sistemainmobilario.SignatureView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActaInfraccionActivity extends AppCompatActivity {

    private String nombreInspector;

    private String apellidoInspector;
    private String legajoInspector;
    private String inspectorId;
    private CheckBox cbIncumplimiento, cbClausuraPreventiva;
    private CheckBox cbMismoLugar; // 👈 Same address checkbox
    
    // UI Refactor - Toggle Mode
    private Button btnModeInfraccion, btnModeInspeccion;
    private LinearLayout layoutContainerInfraccion, layoutContainerInspeccion;
    private boolean isModeInspeccion = false; // Default: Infraccion

    private Button btnSeleccionarCausasInspeccion;      // 👈 NUEVO
    private TextView tvCausasSeleccionadasInspeccion;   // 👈 NUEVO
    private TextView btnLimpiarInfraccion, btnLimpiarInspeccion; // 👈 CLEAR BUTTONS (TEXT)
    private TextView tvErrorSelection; // 👈 Error label

    private LinearLayout layoutCausasCheckBoxes;
    // btnSeleccionarCausas restored
    private Button btnSeleccionarCausas; 
    private TextView tvCausasSeleccionadas;
    private Button btnExpandSignature, btnClearSignature;
    private Bitmap firmaInfractorBitmap;

    private static final String TAG = "ActaInfraccion";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int LOGO_RESOURCE_ID = R.drawable.ic_launcher;
    private static final int MAX_IMAGES = 2;
    private byte[] comandoFirma;

    private EditText etPropietario, etDomicilio, etLugarInfraccion, etSeccion, etChacra, etManzana,
            etParcela, etLote, etPartida, etObservaciones, etBoletaInspeccion;
    private TextInputLayout tilPropietario, tilDomicilio, tilLugarInfraccion;

    private CheckBox cbCartelObra, cbDispositivosSeguridad, cbNumeroPermiso, cbMaterialesVereda,
            cbCercoObra, cbPlanosAprobados, cbDirectorObra, cbVarios;
    //private Button btnGuardarImprimir, btnClearSignature, btnAddImages;
    private Button btnGuardarImprimir, btnAddImages, btnFirma, btnCancelar;
    private ImageButton btnCancelHeader;
    private SignatureView signatureView;
    private RecyclerView rvImages;
    private ImageAdapter imageAdapter;
    private List<Uri> imageList = new ArrayList<>();
    private Uri photoURI;

    private String parcelaData;
    private ActaInfraccionData ultimaActaImpresa = null;
    private BluetoothPrinterConnector printerConnector;
    private OutputStream outputStream;

    private ApiClient apiClient;
    private OracleApiService apiService;
    private boolean actaYaGuardada = false;

    private ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        // Múltiples imágenes seleccionadas
                        int count = result.getData().getClipData().getItemCount();
                        // Agregar solo hasta el número máximo permitido
                        for (int i = 0; i < count && imageList.size() < MAX_IMAGES; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            imageList.add(imageUri);
                        }

                        if (count > MAX_IMAGES - imageList.size() + count) {
                            showToast("Solo se agregaron " + (MAX_IMAGES - imageList.size() + count) +
                                    " imágenes. Máximo permitido: " + MAX_IMAGES);
                        }
                    } else if (result.getData().getData() != null) {
                        // Una sola imagen seleccionada
                        if (imageList.size() < MAX_IMAGES) {
                            Uri imageUri = result.getData().getData();
                            imageList.add(imageUri);
                        } else {
                            showToast("Solo se permiten " + MAX_IMAGES + " imágenes");
                        }
                    }
                    imageAdapter.notifyDataSetChanged();
                }
            }
    );

    private ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (photoURI != null) {
                        imageList.add(photoURI);
                        imageAdapter.notifyDataSetChanged();
                    }
                }
            }
    );

    // ===============================
// ✅ onCreate() MODIFICADO
// ===============================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acta_infraccion);

        parcelaData = getIntent().getStringExtra("PARCEL_DATA");
        printerConnector = new BluetoothPrinterConnector(this);
        apiClient = new ApiClient(this);

        // ✅ RECIBIR DATOS DEL INSPECTOR (VIENEN DESDE ParcelDetailActivity)
        nombreInspector = getIntent().getStringExtra("NOMBRE_INSPECTOR");
        apellidoInspector = getIntent().getStringExtra("APELLIDO_INSPECTOR");
        legajoInspector = getIntent().getStringExtra("LEGAJO_INSPECTOR");
        inspectorId = getIntent().getStringExtra("INSPECTOR_ID");

        Log.d(TAG, "✅ Datos del inspector recibidos en ActaInfraccionActivity: "
                + nombreInspector + " " + apellidoInspector
                + " | Legajo: " + legajoInspector
                + " | ID: " + inspectorId);

        initViews();
        fillInitialData();
        setupListeners();
        checkCameraAndStoragePermissions();
    }


    private void initViews() {

        // ✅ Layout oculto (backend de checkboxes)
        layoutCausasCheckBoxes = findViewById(R.id.layoutCausasCheckBoxes);

        // ✅ Campos de texto
        etPropietario      = findViewById(R.id.etPropietario);
        etDomicilio        = findViewById(R.id.etDomicilio);
        etLugarInfraccion  = findViewById(R.id.etLugarInfraccion);
        etSeccion          = findViewById(R.id.etSeccion);
        etChacra           = findViewById(R.id.etChacra);
        etManzana          = findViewById(R.id.etManzana);
        etParcela          = findViewById(R.id.etParcela);
        etLote             = findViewById(R.id.etLote);
        etPartida          = findViewById(R.id.etPartida);
        etPartida          = findViewById(R.id.etPartida);
        etObservaciones    = findViewById(R.id.etObservaciones);

        // ✅ TextInputLayouts for Validation
        tilPropietario     = findViewById(R.id.tilPropietario);
        tilDomicilio       = findViewById(R.id.tilDomicilio);
        tilLugarInfraccion = findViewById(R.id.tilLugarInfraccion);

        etBoletaInspeccion = findViewById(R.id.etBoletaInspeccion);
        if (etBoletaInspeccion != null) {
            etBoletaInspeccion.setEnabled(false);
        }

        // ✅ Checkboxes (backend)
        cbCartelObra            = findViewById(R.id.cbCartelObra);
        cbDispositivosSeguridad = findViewById(R.id.cbDispositivosSeguridad);
        cbNumeroPermiso         = findViewById(R.id.cbNumeroPermiso);
        cbMaterialesVereda      = findViewById(R.id.cbMaterialesVereda);
        cbCercoObra             = findViewById(R.id.cbCercoObra);
        cbPlanosAprobados       = findViewById(R.id.cbPlanosAprobados);
        cbDirectorObra          = findViewById(R.id.cbDirectorObra);
        cbVarios                = findViewById(R.id.cbVarios);

        // ✅ NUEVOS (backend)
        cbIncumplimiento        = findViewById(R.id.cbIncumplimiento);
        // ✅ NUEVOS (backend)
        cbIncumplimiento        = findViewById(R.id.cbIncumplimiento);
        cbClausuraPreventiva    = findViewById(R.id.cbClausuraPreventiva);
        
        // Checkbox "Mismo Lugar"
        cbMismoLugar = findViewById(R.id.cbMismoLugar);
        // Default state: Checked -> Hide LugarInfraccion input
        if (cbMismoLugar != null) {
            cbMismoLugar.setChecked(true);
            if (tilLugarInfraccion != null) tilLugarInfraccion.setVisibility(View.GONE);
        }



        // ✅ UI Toggle Mode Init
        btnModeInfraccion = findViewById(R.id.btnModeInfraccion);
        btnModeInspeccion = findViewById(R.id.btnModeInspeccion);
        layoutContainerInfraccion = findViewById(R.id.layoutContainerInfraccion);
        layoutContainerInspeccion = findViewById(R.id.layoutContainerInspeccion);

        // ✅ Botones + textos resumen
        btnSeleccionarCausas = findViewById(R.id.btnSeleccionarCausas);
        tvCausasSeleccionadas = findViewById(R.id.tvCausasSeleccionadas);
        btnLimpiarInfraccion = findViewById(R.id.btnLimpiarInfraccion); // 👈 Init
        
        btnSeleccionarCausasInspeccion = findViewById(R.id.btnSeleccionarCausasInspeccion);
        tvCausasSeleccionadasInspeccion = findViewById(R.id.tvCausasSeleccionadasInspeccion);
        btnLimpiarInspeccion = findViewById(R.id.btnLimpiarInspeccion); // 👈 Init
        
        tvErrorSelection = findViewById(R.id.tvErrorSelection);

        btnGuardarImprimir = findViewById(R.id.btnGuardarImprimir);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnCancelHeader = findViewById(R.id.btnCancelHeader);

        // ✅ Firma e imágenes
        signatureView = findViewById(R.id.signatureView);
        btnAddImages  = findViewById(R.id.btnAddImages);
        btnFirma      = findViewById(R.id.btnFirma);
        rvImages      = findViewById(R.id.rvImages);

        // 👉 Botones extra firma (ocultos)
        btnExpandSignature = findViewById(R.id.btnExpandSignature);
        btnClearSignature  = findViewById(R.id.btnClearSignature);

        if (btnExpandSignature != null) btnExpandSignature.setVisibility(View.GONE);
        if (btnClearSignature != null)  btnClearSignature.setVisibility(View.GONE);

        // ✅ SignatureView oculto: se usa solo como buffer interno
        if (signatureView != null) {
            signatureView.setVisibility(View.GONE);
            signatureView.setStrokeWidth(2.5f);
            signatureView.setMinWidth(1.0f);
            signatureView.setMaxWidth(4.0f);
            signatureView.setVelocityFilterWeight(0.8f);
            signatureView.setPenColor(Color.BLUE);
            signatureView.setBackgroundColor(Color.WHITE);
        }

        // ✅ IMPORTANTE: mantener oculto el layout backend
        if (layoutCausasCheckBoxes != null) {
            layoutCausasCheckBoxes.setVisibility(View.GONE);
        }

        // ✅ RecyclerView imágenes
        if (rvImages != null) {
            rvImages.setLayoutManager(new LinearLayoutManager(
                    this,
                    LinearLayoutManager.HORIZONTAL,
                    false
            ));

            imageAdapter = new ImageAdapter(this, imageList, position -> {
                imageList.remove(position);
                imageAdapter.notifyItemRemoved(position);
            });
            rvImages.setAdapter(imageAdapter);
        }

        // ✅ Retrofit
        apiService = RetrofitClient.getClient().create(OracleApiService.class);
    }


    private void fillInitialData() {
        if (parcelaData != null) {
            try {
                JSONObject json = new JSONObject(parcelaData);
                etPropietario.setText(json.optString("APYN", ""));
                String calle = json.optString("CALLE", "");
                String nro = json.optString("NRO", "");
                etDomicilio.setText(calle + (nro.isEmpty() ? "" : " " + nro));
                etLugarInfraccion.setText(calle);
                etSeccion.setText(json.optString("SEC", ""));
                etChacra.setText(json.optString("CHA", ""));
                etManzana.setText(json.optString("MAN", ""));
                etParcela.setText(json.optString("PAR", ""));
                etLote.setText(json.optString("LOTE", ""));
                etPartida.setText(json.optString("PART", ""));
                etBoletaInspeccion.setText(generateActaNumber());
            } catch (Exception e) {
                showToast("Error al cargar datos de la parcela");
                Log.e(TAG, "Error en fillInitialData: " + e.getMessage());
            }
        }
    }

    private void setupListeners() {
        // ✅ NAVIGATION: Cancel / X -> Main Map (Clean state)
        View.OnClickListener cancelListener = v -> {
            Intent intent = new Intent(ActaInfraccionActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        };

        if (btnCancelHeader != null) btnCancelHeader.setOnClickListener(cancelListener);
        if (btnCancelar != null) btnCancelar.setOnClickListener(cancelListener);

        // Guardar + Imprimir
        btnGuardarImprimir.setOnClickListener(v -> {
            if (!validateForm()) return;

            if (ultimaActaImpresa != null && actaYaGuardada) {
                new AlertDialog.Builder(this)
                        .setTitle("Reimprimir Acta")
                        .setMessage("¿Deseás volver a imprimir la última acta generada?")
                        .setPositiveButton("Sí", (dialog, which) -> checkBluetoothPermission())
                        .setNegativeButton("No", null)
                        .show();
            } else {
                guardarYImprimir();
            }
        });

        // Botón FIRMA -> abre diálogo de firma en pantalla completa
        if (btnFirma != null) {
            btnFirma.setOnClickListener(v -> showFullscreenSignatureDialog());
        }

        // Botón AGREGAR IMÁGENES (cámara / galería)
        if (btnAddImages != null) {
            btnAddImages.setOnClickListener(v -> {
                if (imageList.size() >= MAX_IMAGES) {
                    showToast("Solo se permiten " + MAX_IMAGES + " imágenes");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Agregar imagen")
                        .setItems(new CharSequence[]{"Tomar foto", "Seleccionar de galería"},
                                (dialog, which) -> {
                                    if (which == 0) {
                                        dispatchTakePictureIntent();
                                    } else {
                                        openGallery();
                                    }
                                })
                        .show();
            });
        }

        // ✅ Botón para seleccionar causas/faltas
        if (btnSeleccionarCausas != null) {
            btnSeleccionarCausas.setOnClickListener(v -> mostrarDialogoCausas());
        }
        
        // Listeners Toggle Mode
        if (btnModeInfraccion != null) {
            btnModeInfraccion.setOnClickListener(v -> setMode(false));
        }
        if (btnModeInspeccion != null) {
            btnModeInspeccion.setOnClickListener(v -> setMode(true));
        }

        if (btnSeleccionarCausasInspeccion != null) {
            btnSeleccionarCausasInspeccion.setOnClickListener(v -> mostrarDialogoCausasInspeccion());
        }

        // ✅ Clear Buttons
        if (btnLimpiarInfraccion != null) {
            btnLimpiarInfraccion.setOnClickListener(v -> limpiarInfraccion());
        }
        if (btnLimpiarInspeccion != null) {
            btnLimpiarInspeccion.setOnClickListener(v -> limpiarInspeccion());
        }

        // Toggle Lugar Infracción visibility
        if (cbMismoLugar != null) {
            cbMismoLugar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (tilLugarInfraccion != null) {
                    tilLugarInfraccion.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                    if (isChecked) {
                        // Clear error when hidden
                        tilLugarInfraccion.setError(null);
                        etLugarInfraccion.setText(""); 
                    }
                }
            });
        }
    }

    private void setMode(boolean isInspeccion) {
        // EXCLUSIVE SELECTION LOGIC
        if (isInspeccion) {
            // Trying to switch to INSPECCION. Check if INFRACCION has data.
            if (hasInfractionData()) {
                showToast("Tiene faltas de tipo Infracción seleccionadas. Elimínelas para cambiar de acta.");
                return; // BLOCK SWITCH
            }
        } else {
            // Trying to switch to INFRACCION. Check if INSPECCION has data.
            if (hasInspectionData()) {
                showToast("Tiene resultados de Inspección seleccionados. Elimínelos para cambiar de acta.");
                return; // BLOCK SWITCH
            }
        }
        
        this.isModeInspeccion = isInspeccion;
        // ... (styling logic continues below, unchanged chunks)
        // Toggle Buttons Style
        if (isInspeccion) {
            // Active: Inspeccion (Green)
            if (btnModeInspeccion != null) {
                btnModeInspeccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3EBD84")));
                btnModeInspeccion.setTextColor(Color.WHITE);
            }
            // Inactive: Infraccion (Grey)
            if (btnModeInfraccion != null) {
                btnModeInfraccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                btnModeInfraccion.setTextColor(Color.parseColor("#757575"));
            }

            // Containers
            if (layoutContainerInspeccion != null) layoutContainerInspeccion.setVisibility(View.VISIBLE);
            if (layoutContainerInfraccion != null) layoutContainerInfraccion.setVisibility(View.GONE);

        } else {
            // Active: Infraccion (Green)
            if (btnModeInfraccion != null) {
                btnModeInfraccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3EBD84")));
                btnModeInfraccion.setTextColor(Color.WHITE);
            }
            // Inactive: Inspeccion (Grey)
            if (btnModeInspeccion != null) {
                btnModeInspeccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                btnModeInspeccion.setTextColor(Color.parseColor("#757575"));
            }

            // Containers
            if (layoutContainerInfraccion != null) layoutContainerInfraccion.setVisibility(View.VISIBLE);
            if (layoutContainerInspeccion != null) layoutContainerInspeccion.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoCausas() {
        final String[] items = new String[]{
                "Cartel de Obra Reglamentario (punto 2.2.12 C.E.P.)",
                "Dispositivos de Seguridad (punto 4.14 y anexos C.E.P.)",
                "Nº Permiso (punto 2.2.6 y anexos C.E.P.)",
                "Materiales en Vereda (punto 3.5.2.4 C.E.P.)",
                "Cerco de Obra (punto 4.1 y anexos C.E.P.)",
                "Planos Aprobados (punto 2.1.8.7 C.E.P.)",
                "Director de Obra (punto 2.4.1 C.E.P.)",
                "Varios",
                "Incumplimiento",
                "Clausura preventiva"
        };

        final boolean[] checked = new boolean[]{
                cbCartelObra.isChecked(),
                cbDispositivosSeguridad.isChecked(),
                cbNumeroPermiso.isChecked(),
                cbMaterialesVereda.isChecked(),
                cbCercoObra.isChecked(),
                cbPlanosAprobados.isChecked(),
                cbDirectorObra.isChecked(),
                cbVarios.isChecked(),
                cbIncumplimiento.isChecked(),
                cbClausuraPreventiva.isChecked()
        };

        new AlertDialog.Builder(this)
                .setTitle("Seleccione causas / faltas")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Aceptar", (dialog, which) -> {

                    cbCartelObra.setChecked(checked[0]);
                    cbDispositivosSeguridad.setChecked(checked[1]);
                    cbNumeroPermiso.setChecked(checked[2]);
                    cbMaterialesVereda.setChecked(checked[3]);
                    cbCercoObra.setChecked(checked[4]);
                    cbPlanosAprobados.setChecked(checked[5]);
                    cbDirectorObra.setChecked(checked[6]);
                    cbVarios.setChecked(checked[7]);
                    cbIncumplimiento.setChecked(checked[8]);
                    cbClausuraPreventiva.setChecked(checked[9]);

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append("\n"); // New line
                            sb.append("• ").append(items[i]);     // Bullet
                        }
                    }

                    if (sb.length() == 0) {
                        tvCausasSeleccionadas.setText("Ninguna causa seleccionada");
                        if (btnLimpiarInfraccion != null) btnLimpiarInfraccion.setVisibility(View.GONE);
                    } else {
                        tvCausasSeleccionadas.setText(sb.toString());
                        if (btnLimpiarInfraccion != null) btnLimpiarInfraccion.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoCausasInspeccion() {
        // Lista de RESULTADOS para inspección (ajustá los textos como quieras)
        final String[] items = new String[]{
                "Construccion sin permiso (2.2.6 C.E.P.)",
                "Cartel de obra (2.2.12 C.E.P.)",
                "Documentacion tecnica en obra (2.1.8.7 C.E.P.)",
                "Modificaciones y ampliaciones (2.1.8.8 C.E.P.)",
                "Clausura de obra (2.3.3.4 C.E.P.)",
                "Paralizacion de la obra (2.3.5 C.E.P.)",
                "Direccion de Obra (2.4.1 C.E.P.)",
                "Materiales y obstaculos en la vía publica (3.1.2 - 4.2.3 C.E.P.)",
                "Acceso para discapacitados (3.10.4 C.E.P.)",
                "Permiso de vallado (4.1.1.1 C.E.P.)",
                "Canaletas de desagüe (4.12.2 C.E.P.)",
                "Medidas de proteccion y seguridad (4.14 C.E.P.)",
                "Proteccion a la via publica (4.14.2 C.E.P.)",
                "Instalaciones electricas (3.1.11.2 C.E.P.)",
                "Ascensores y montacargas (3.4.8.4 C.E.P.)",
                "Servicio de salubridad (2.5.2 C.E.P.)",
                "Instalaciones que produzcan molestias (3.7.4 C.E.P.)",
                "Planos contra incendio (3.9.1.5 C.E.P.)",
                "Servicios sanitarios para discapacitados (3.10.7 C.E.P.)",
                "Seguridad instalaciones electricas (5.5.1.1 C.E.P.)",
                "VARIOS"

        };

        // Guardamos selección en memoria local (no hay checkboxes ocultos acá)
        final boolean[] checked = new boolean[items.length];

        new AlertDialog.Builder(this)
                .setTitle("Seleccione resultado de inspección")
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Aceptar", (dialog, which) -> {

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("• ").append(items[i]);
                        }
                    }

                    if (sb.length() == 0) {
                        tvCausasSeleccionadasInspeccion.setText("Ningún resultado seleccionado");
                        if (btnLimpiarInspeccion != null) btnLimpiarInspeccion.setVisibility(View.GONE);
                    } else {
                        tvCausasSeleccionadasInspeccion.setText(sb.toString());
                        if (btnLimpiarInspeccion != null) btnLimpiarInspeccion.setVisibility(View.VISIBLE);
                    }

                    // ✅ Marcar tipo de actuación automáticamente: INSPECCIÓN

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    // ==========================================
    //  EXTRAS: CLEAR & CHECK METHODS
    // ==========================================

    private void limpiarInfraccion() {
        cbCartelObra.setChecked(false);
        cbDispositivosSeguridad.setChecked(false);
        cbNumeroPermiso.setChecked(false);
        cbMaterialesVereda.setChecked(false);
        cbCercoObra.setChecked(false);
        cbPlanosAprobados.setChecked(false);
        cbDirectorObra.setChecked(false);
        cbVarios.setChecked(false);
        cbIncumplimiento.setChecked(false);
        cbClausuraPreventiva.setChecked(false);
        
        tvCausasSeleccionadas.setText("Ninguna causa seleccionada");
        if (btnLimpiarInfraccion != null) btnLimpiarInfraccion.setVisibility(View.GONE);
    }

    private void limpiarInspeccion() {
        tvCausasSeleccionadasInspeccion.setText("Ningún resultado seleccionado");
        if (btnLimpiarInspeccion != null) btnLimpiarInspeccion.setVisibility(View.GONE);
    }

    private boolean hasInfractionData() {
        return cbCartelObra.isChecked() ||
               cbDispositivosSeguridad.isChecked() ||
               cbNumeroPermiso.isChecked() ||
               cbMaterialesVereda.isChecked() ||
               cbCercoObra.isChecked() ||
               cbPlanosAprobados.isChecked() ||
               cbDirectorObra.isChecked() ||
               cbVarios.isChecked() ||
               cbIncumplimiento.isChecked() ||
               cbClausuraPreventiva.isChecked();
    }

    private boolean hasInspectionData() {
        if (tvCausasSeleccionadasInspeccion == null) return false;
        String text = tvCausasSeleccionadasInspeccion.getText().toString().trim();
        return !text.isEmpty() && !text.equals("Ningún resultado seleccionado");
    }

    private void resetForm() {
        firmaInfractorBitmap = null;
        if (signatureView != null) {
            signatureView.clear();
        }
        // Limpiar textos
        etPropietario.setText("");
        etDomicilio.setText("");
        etLugarInfraccion.setText("");
        etSeccion.setText("");
        etChacra.setText("");
        etManzana.setText("");
        etParcela.setText("");
        etLote.setText("");
        etPartida.setText("");
        etObservaciones.setText("");

        // Generar nuevo número de acta
        String nuevoNumeroActa = generateActaNumber();
        etBoletaInspeccion.setText(nuevoNumeroActa);

        // Checkboxes
        cbCartelObra.setChecked(false);
        cbDispositivosSeguridad.setChecked(false);
        cbNumeroPermiso.setChecked(false);
        cbMaterialesVereda.setChecked(false);
        cbCercoObra.setChecked(false);
        cbPlanosAprobados.setChecked(false);
        cbDirectorObra.setChecked(false);
        cbVarios.setChecked(false);
        cbIncumplimiento.setChecked(false);
        cbClausuraPreventiva.setChecked(false);

        // Tipo de actuación: por defecto "Infracción"


        // Limpiar firma
        if (signatureView != null) {
            signatureView.clear();
        }

        // Limpiar imágenes
        if (imageList != null) {
            imageList.clear();
        }
        if (imageAdapter != null) {
            imageAdapter.notifyDataSetChanged();
        }

        // Resetear estado de acta guardada / última acta
        ultimaActaImpresa = null;
        actaYaGuardada = false;
    }
    private void showFullscreenSignatureDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_signature_fullscreen);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        SignatureView fullScreenSignature = dialog.findViewById(R.id.fullscreenSignatureView);
        Button btnClear  = dialog.findViewById(R.id.btnClearFullscreenSignature);
        Button btnSave   = dialog.findViewById(R.id.btnSaveSignature);
        Button btnCancel = dialog.findViewById(R.id.btnCancelSignature);

        // Configuración visual
        fullScreenSignature.setStrokeWidth(7f);
        fullScreenSignature.setPenColor(Color.BLUE);
        fullScreenSignature.setBackgroundColor(Color.WHITE);

        // Si ya había una firma guardada antes, la mostramos
        if (firmaInfractorBitmap != null) {
            fullScreenSignature.setBitmap(firmaInfractorBitmap);
        }

        btnSave.setOnClickListener(v -> {
            if (!fullScreenSignature.hasSignature()) {
                Toast.makeText(this, "Por favor firme antes de guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Bitmap signature = fullScreenSignature.getBitmap();
                if (signature != null) {
                    // Guardamos la firma directamente en la Activity
                    firmaInfractorBitmap = signature;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al obtener la firma del diálogo: " + e.getMessage(), e);
            }

            // 👇 Y LISTO: solo cierra el diálogo, nada de guardar/imprimir todavía
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClear.setOnClickListener(v -> fullScreenSignature.clear());

        dialog.show();
    }



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePicker.launch(intent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // CAMBIO #1: No usar resolveActivity() en Android 11+ ya que puede fallar
        boolean hasCameraApp = true;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            hasCameraApp = takePictureIntent.resolveActivity(getPackageManager()) != null;
        }

        if (hasCameraApp) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d(TAG, "Archivo de foto creado: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                Log.e(TAG, "Error al crear archivo de imagen: " + ex.getMessage(), ex);
                showToast("Error al crear archivo de imagen");
                return;
            }

            if (photoFile != null) {
                try {
                    // CAMBIO #2: Usar directamente la autoridad configurada en el Manifest
                    photoURI = FileProvider.getUriForFile(this,
                            "es.rcti.demoprinterplus.sistemainmobilario.fileprovider",
                            photoFile);

                    // CAMBIO #3: Otorgar permisos de URI temporales
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                    // CAMBIO #4: Agregar logs para diagnóstico
                    Log.d(TAG, "Lanzando intent de cámara con URI: " + photoURI);

                    cameraLauncher.launch(takePictureIntent);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error al crear URI para la cámara: " + e.getMessage(), e);
                    showToast("Error al configurar la cámara. Verifique que exista file_paths.xml");
                }
            }
        } else {
            Log.e(TAG, "No se encontró aplicación de cámara");
            showToast("No hay aplicación de cámara disponible");

            // CAMBIO #5: Listar aplicaciones de cámara disponibles para diagnóstico
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> cameraApps = packageManager.queryIntentActivities(
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    PackageManager.MATCH_DEFAULT_ONLY);

            Log.d(TAG, "Aplicaciones de cámara disponibles: " + cameraApps.size());
            for (ResolveInfo app : cameraApps) {
                Log.d(TAG, "App de cámara: " + app.activityInfo.packageName);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Crear un nombre de archivo de imagen
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefijo */
                ".jpg",         /* sufijo */
                storageDir      /* directorio */
        );
        return image;
    }




    private boolean validateForm() {
        boolean esValido = true;

        // 1) Campos básicos obligatorios
        if (etPropietario.getText().toString().trim().isEmpty()) {
            tilPropietario.setError("Campo requerido");
            esValido = false;
        } else {
            tilPropietario.setError(null);
        }

        if (etDomicilio.getText().toString().trim().isEmpty()) {
             tilDomicilio.setError("Campo requerido");
             esValido = false;
        } else {
             tilDomicilio.setError(null);
        }

        // Validate LugarInfraccion ONLY if Same Address is NOT checked
        if (cbMismoLugar != null && !cbMismoLugar.isChecked()) {
             if (etLugarInfraccion.getText().toString().trim().isEmpty()) {
                tilLugarInfraccion.setError("Campo requerido");
                esValido = false;
            } else {
                tilLugarInfraccion.setError(null);
            }
        }

        // 2) ¿Hay alguna causa de INFRACCIÓN marcada? (checkboxes)
        boolean hayCausaInfraccion =
                cbCartelObra.isChecked() ||
                        cbDispositivosSeguridad.isChecked() ||
                        cbNumeroPermiso.isChecked() ||
                        cbMaterialesVereda.isChecked() ||
                        cbCercoObra.isChecked() ||
                        cbPlanosAprobados.isChecked() ||
                        cbDirectorObra.isChecked() ||
                        cbVarios.isChecked() ||
                        // ✅ NUEVOS
                        cbIncumplimiento.isChecked() ||
                        cbClausuraPreventiva.isChecked();

        // 3) ¿Hay algún resultado de INSPECCIÓN seleccionado? (texto del resumen)
        boolean hayCausaInspeccion = false;
        if (tvCausasSeleccionadasInspeccion != null) {
            String texto = tvCausasSeleccionadasInspeccion.getText().toString().trim();
            hayCausaInspeccion = !texto.isEmpty()
                    && !texto.equals("Ningún resultado seleccionado");
        }

        // 4) Validar Selección según MODO ACTIVO
        boolean seleccionValida = false;

        if (isModeInspeccion) {
             // Modo Inspección: Verificar texto
             if (tvCausasSeleccionadasInspeccion != null) {
                String texto = tvCausasSeleccionadasInspeccion.getText().toString().trim();
                if (!texto.isEmpty() && !texto.equals("Ningún resultado seleccionado")) {
                    seleccionValida = true;
                }
            }
        } else {
            // Modo Infracción: Verificar checkboxes
            // NOTA: layoutCausasCheckBoxes ahora es visible siempre en este modo
            seleccionValida = 
                    cbCartelObra.isChecked() ||
                    cbDispositivosSeguridad.isChecked() ||
                    cbNumeroPermiso.isChecked() ||
                    cbMaterialesVereda.isChecked() ||
                    cbCercoObra.isChecked() ||
                    cbPlanosAprobados.isChecked() ||
                    cbDirectorObra.isChecked() ||
                    cbVarios.isChecked() ||
                    cbIncumplimiento.isChecked() ||
                    cbClausuraPreventiva.isChecked();
        }

        if (!seleccionValida) {
            if (tvErrorSelection != null) {
                tvErrorSelection.setVisibility(View.VISIBLE);
                tvErrorSelection.setText("Debe seleccionar una opción");
            }
            esValido = false;
        } else {
            if (tvErrorSelection != null) {
                tvErrorSelection.setVisibility(View.GONE);
            }
        }

        if (!esValido) {
            showToast("Por favor, corrija los errores marcados");
        }

        return esValido;
    }

    private void guardarYImprimir() {
        if (!validateForm()) return;

        actaYaGuardada = false;

        final ActaInfraccionData acta = generateActaData();
        acta.setImagenesPrueba(imageList);
        ultimaActaImpresa = acta;

        // Inspector ID
        SharedPreferences preferences = getSharedPreferences("ActasInfraccionApp", MODE_PRIVATE);
        String inspectorId = preferences.getString("inspector_id", "1");
        acta.setInspectorId(inspectorId);

        // ✅ SI NO HAY INTERNET => OFFLINE + IMPRIMIR
        if (!isNetworkAvailable()) {
            // Guardar OFFLINE (Room/SQLite local)
            guardarOffline(acta);

            // Imprimir igual (si querés imprimir sin internet)
            actaYaGuardada = true; // ya quedó guardada localmente
            showToast("Acta guardada OFFLINE. Imprimiendo...");
            runOnUiThread(this::checkBluetoothPermission);
            return;
        }

        // ✅ SI HAY INTERNET => FLUJO NORMAL ONLINE
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Guardando acta...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        // Timeout de seguridad
        new Handler().postDelayed(() -> {
            if (progressDialog.isShowing()) {
                try {
                    progressDialog.dismiss();
                    showToast("La operación ha excedido el tiempo límite. Intente nuevamente.");
                } catch (Exception e) {
                    Log.e(TAG, "Error al cerrar el diálogo: " + e.getMessage());
                }
            }
        }, 30000);

        final long startTime = System.currentTimeMillis();
        Log.d(TAG, "Iniciando guardar acta (ONLINE): " + startTime);

        apiClient.insertarActaInfraccion(acta, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Tiempo de respuesta: " + (endTime - startTime) + "ms");

                    if (response.has("success") && response.getBoolean("success")) {
                        int actaId = response.optInt("acta_id", 0);

                        if (actaId > 0) {
                            // ✅ Subir firma -> luego sube imágenes -> luego imprime
                            subirFirma(String.valueOf(actaId), acta.getFirmaInfractor(), progressDialog, true);
                        } else {
                            // Caso raro: success true pero sin id
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            actaYaGuardada = true;
                            showToast("Acta guardada. Imprimiendo...");
                            runOnUiThread(() -> checkBluetoothPermission());
                        }

                    } else {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        String error = response.optString("error", "Error desconocido");
                        showToast("Error al guardar: " + error);
                        Log.e(TAG, "Error al guardar acta: " + error);
                    }

                } catch (JSONException e) {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    showToast("Error al procesar la respuesta del servidor");
                    Log.e(TAG, "Error JSON: " + e.getMessage(), e);
                }
            }

            @Override
            public void onError(String error) {
                long endTime = System.currentTimeMillis();
                Log.e(TAG, "Error ONLINE en " + (endTime - startTime) + "ms: " + error);

                // ✅ Si falla la comunicación, guardamos offline para no perder el acta
                if (progressDialog.isShowing()) progressDialog.dismiss();

                showToast("No se pudo guardar online. Se guarda OFFLINE y se sincroniza luego.");
                guardarOffline(acta);

                // Imprimir igual
                actaYaGuardada = true;
                runOnUiThread(() -> checkBluetoothPermission());
            }
        });
    }

    private void saveActa(boolean imprimirLuego) {
        if (!validateForm()) {
            return;
        }

        if (!isNetworkAvailable()) {
            showToast("No hay conexión a internet. No se puede guardar el acta.");
            return;
        }

        SharedPreferences preferences = getSharedPreferences("ActasInfraccionApp", MODE_PRIVATE);
        String inspectorId = preferences.getString("inspector_id", "1"); // Valor provisional: 1

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Guardando acta...");
        progressDialog.setCancelable(true); // Permitir cancelar si es necesario
        progressDialog.show();

        // Añadir timeout al ProgressDialog
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                        showToast("La operación ha excedido el tiempo límite. Intente nuevamente.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error al cerrar el diálogo: " + e.getMessage());
                    }
                }
            }
        }, 30000); // 30 segundos de timeout

        ActaInfraccionData acta = generateActaData();
        acta.setInspectorId(inspectorId);
// Firma ya viene en el acta desde generateActaData()
        acta.setImagenesPrueba(imageList);

        ultimaActaImpresa = acta;
        actaYaGuardada = false;

        // Registramos el tiempo de inicio para diagnóstico
        final long startTime = System.currentTimeMillis();
        Log.d(TAG, "Iniciando guardar acta (sin imprimir): " + startTime);

        apiClient.insertarActaInfraccion(acta, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Tiempo de respuesta: " + (endTime - startTime) + "ms");

                    if (response.has("success") && response.getBoolean("success")) {
                        int actaId = response.optInt("acta_id", 0);
                        if (actaId > 0) {
                            // Guardar la firma
                            subirFirma(String.valueOf(actaId), acta.getFirmaInfractor(), progressDialog, imprimirLuego);
                        } else {
                            progressDialog.dismiss();
                            actaYaGuardada = true;

                            if (imprimirLuego) {
                                showToast("Acta guardada. Imprimiendo...");
                                runOnUiThread(() -> {
                                    checkBluetoothPermission();
                                });
                            } else {
                                new AlertDialog.Builder(ActaInfraccionActivity.this)
                                        .setTitle("Acta Guardada")
                                        .setMessage("Acta N° " + acta.getNumeroActa() + " guardada exitosamente.")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                            }
                        }
                    } else {
                        progressDialog.dismiss();
                        String error = response.optString("error", "Error desconocido");
                        showToast("Error al guardar: " + error);
                        Log.e(TAG, "Error al guardar acta: " + error);
                    }
                } catch (JSONException e) {
                    progressDialog.dismiss();
                    showToast("Error al procesar respuesta del servidor");
                    Log.e(TAG, "Error JSON: " + e.getMessage(), e);
                }
            }

            @Override
            public void onError(String error) {
                long endTime = System.currentTimeMillis();
                Log.e(TAG, "Error en " + (endTime - startTime) + "ms: " + error);

                progressDialog.dismiss();
                showToast("Error en la comunicación con el servidor: " + error);
                Log.e(TAG, "Error en la API: " + error);
            }
        });
    }


    // ===============================
// ✅ guardarOffline() (GUARDA IMÁGENES EN GALERÍA)
// ===============================
    private void guardarOffline(ActaInfraccionData acta) {
        new Thread(() -> {
            String localId = java.util.UUID.randomUUID().toString();

            AppDb db = AppDb.get(getApplicationContext());

            ActaEntity e = new ActaEntity();

            // ===== guardar checks (INFRACCION) =====
            e.cartelObra = acta.isCartelObra() ? 1 : 0;
            e.dispositivosSeguridad = acta.isDispositivosSeguridad() ? 1 : 0;
            e.numeroPermiso = acta.isNumeroPermiso() ? 1 : 0;
            e.materialesVereda = acta.isMaterialesVereda() ? 1 : 0;
            e.cercoObra = acta.isCercoObra() ? 1 : 0;
            e.planosAprobados = acta.isPlanosAprobados() ? 1 : 0;
            e.directorObra = acta.isDirectorObra() ? 1 : 0;
            e.varios = acta.isVarios() ? 1 : 0;

            e.incumplimiento = acta.isIncumplimiento() ? 1 : 0;
            e.clausuraPreventiva = acta.isClausuraPreventiva() ? 1 : 0;

            // ===== inspección =====
            e.resultadoInspeccion = n(acta.getResultadoInspeccion());

            e.localId = localId;

            // Tipo/Acción
            String tipo = acta.getTipoActa() != null ? acta.getTipoActa().trim().toUpperCase() : "INFRACCION";
            e.tipoActa = tipo;
            e.accion = tipo.equals("INSPECCION") ? "insertarActaInspeccion" : "insertarActaInfraccion";

            // Datos acta
            e.numero = n(acta.getNumeroActa());
            e.fecha  = n(acta.getFecha());
            e.hora   = n(acta.getHora());
            e.propietario = n(acta.getPropietario());

            // Inspector
            e.tfInspectorId = n(acta.getInspectorId());

            // Infractor
            e.infractorDni = n(acta.getInfractorDni());
            e.infractorNombre = n(acta.getInfractorNombre());
            e.infractorDomicilio = n(acta.getInfractorDomicilio());

            // Ubicación / Catastro
            e.lugarInfraccion = n(acta.getLugarInfraccion());
            e.seccion = n(acta.getSeccion());
            e.chacra = n(acta.getChacra());
            e.manzana = n(acta.getManzana());
            e.parcela = n(acta.getParcela());
            e.lote = n(acta.getLote());
            e.partida = n(acta.getPartida());

            e.boletaInspeccion = n(acta.getBoletaInspeccion());
            e.observaciones = n(acta.getObservaciones());

            // INFRACCION / INSPECCION
            if ("INSPECCION".equals(tipo)) {
                e.resultadoInspeccion = n(acta.getResultadoInspeccion());
                e.infraccionDesc = "";
            } else {
                e.infraccionDesc = generarTextoFaltas(acta);
                e.resultadoInspeccion = "";
            }

            // Estado sync
            e.serverId = 0;
            e.synced = 0;
            e.createdAt = System.currentTimeMillis();

            // Guardar acta
            db.actaDao().upsert(e);

            // Guardar firma (Bitmap -> PNG bytes)
            Bitmap firma = acta.getFirmaInfractor();
            if (firma != null) {
                byte[] firmaBytes = bitmapToPngBytes(firma);
                if (firmaBytes != null && firmaBytes.length > 0) {
                    FirmaEntity f = new FirmaEntity();
                    f.localId = localId;
                    f.firmaBytes = firmaBytes;
                    db.firmaDao().upsert(f);
                }
            }

            // ===============================
            // ✅ Guardar imágenes en GALERÍA (MediaStore)
            // ===============================
            List<Uri> imgs = acta.getImagenesPrueba();
            if (imgs != null) {
                for (Uri uri : imgs) {
                    if (uri == null) continue;

                    try {
                        Uri uriGaleria = guardarImagenUriEnGaleria(uri); // devuelve content://...

                        ImagenEntity img = new ImagenEntity();
                        img.localId = localId;
                        img.uriString = uriGaleria.toString();
                        img.synced = 0;
                        db.imagenDao().insert(img);

                        Log.d(TAG, "✅ Imagen guardada en galería: " + uriGaleria);

                    } catch (Exception ex) {
                        Log.e(TAG, "❌ Error guardando imagen en galería, guardo URI original: " + uri, ex);

                        // fallback: guardo igual el uri original
                        ImagenEntity img = new ImagenEntity();
                        img.localId = localId;
                        img.uriString = uri.toString();
                        img.synced = 0;
                        db.imagenDao().insert(img);
                    }
                }
            }

            // Encolar sincronización automática
            SyncScheduler.enqueue(getApplicationContext());

            runOnUiThread(() -> showToast("Acta guardada OFFLINE. Se sincroniza automáticamente al volver internet."));
        }).start();
    }

    // ===============================
// ✅ Copia un Uri (content:// o file://) a la GALERÍA
// Guarda en Pictures/Actas y devuelve el Uri final (content://)
// ===============================
    private Uri guardarImagenUriEnGaleria(Uri sourceUri) throws Exception {
        String fileName = "ACTA_" + System.currentTimeMillis() + ".jpg";

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_PICTURES + "/Actas");

        Uri collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri destUri = getContentResolver().insert(collection, values);
        if (destUri == null) throw new IllegalStateException("No se pudo insertar en MediaStore");

        try (java.io.InputStream in = getContentResolver().openInputStream(sourceUri);
             java.io.OutputStream out = getContentResolver().openOutputStream(destUri)) {

            if (in == null || out == null) throw new IllegalStateException("No se pudo abrir streams");

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }

        return destUri;
    }

    // ===============================
// helper
// ===============================


    private Uri guardarImagenEnGaleria(Bitmap bitmap) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "ACTA_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Actas");

        Uri uri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (uri == null) throw new IOException("No se pudo crear URI en galería");

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                throw new IOException("No se pudo escribir imagen");
            }
        }

        return uri; // content://...
    }


    private String n(String s) { return (s == null) ? "" : s.trim(); }

    private byte[] bitmapToPngBytes(Bitmap bmp) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }


    private String generarTextoFaltas(ActaInfraccionData a) {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();

        if (a.isCartelObra()) items.add("Cartel de Obra Reglamentario");
        if (a.isDispositivosSeguridad()) items.add("Dispositivos de Seguridad");
        if (a.isNumeroPermiso()) items.add("N° Permiso");
        if (a.isMaterialesVereda()) items.add("Materiales en Vereda");
        if (a.isCercoObra()) items.add("Cerco de Obra");
        if (a.isPlanosAprobados()) items.add("Planos Aprobados");
        if (a.isDirectorObra()) items.add("Director de Obra");
        if (a.isVarios()) items.add("Varios");

        // Nuevos (si querés incluirlos en el texto)
        if (a.isIncumplimiento()) items.add("Incumplimiento");
        if (a.isClausuraPreventiva()) items.add("Clausura Preventiva");

        if (items.isEmpty()) return "ACTA SIN DETALLE DEFINIDO";
        return android.text.TextUtils.join(" - ", items);
    }


    private String safeText(android.widget.TextView tv) {
        if (tv == null) return "";
        CharSequence cs = tv.getText();
        return cs == null ? "" : cs.toString().trim();
    }


    // Método subirFirma mejorado para evitar bloqueos
    private void subirFirma(String actaId, Bitmap firma, ProgressDialog progressDialog, boolean imprimirLuego) {
        // Verificamos si hay firma y es válida
        if (firma == null || firma.isRecycled() ||
                firma.getWidth() <= 0 || firma.getHeight() <= 0) {

            Log.d(TAG, "No hay firma válida para subir o firma inválida, continuando con el proceso");

            // Pasar al siguiente paso sin intentar subir la firma
            if (imageList.size() > 0) {
                subirImagenes(actaId, progressDialog, imprimirLuego);
            } else {
                progressDialog.dismiss();
                actaYaGuardada = true;

                if (imprimirLuego) {
                    showToast("Acta guardada. Imprimiendo...");
                    runOnUiThread(() -> {
                        checkBluetoothPermission();
                    });
                } else {
                    new AlertDialog.Builder(ActaInfraccionActivity.this)
                            .setTitle("Acta Guardada")
                            .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                }
            }
            return;
        }

        try {
            // Compresión más segura para firmas grandes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 80; // Comprimir al 80% de calidad

            // Primero convertimos la firma a JPEG para reducir tamaño
            firma.compress(Bitmap.CompressFormat.JPEG, quality, baos);

            // Comprobar tamaño - si es demasiado grande, comprimir más
            while (baos.size() > 500 * 1024 && quality > 20) { // Limitar a 500KB
                baos.reset();
                quality -= 10;
                firma.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }

            String firmaBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            Log.d(TAG, "Subiendo firma para acta ID: " + actaId + " (tamaño: " + firmaBase64.length() + " bytes)");

            // Timeout para esta operación específica
            new Handler().postDelayed(() -> {
                if (progressDialog.isShowing()) {
                    // Si todavía estamos en esta operación después de 15 segundos, seguir adelante
                    Log.d(TAG, "Timeout al subir firma, continuando con el proceso");
                    if (imageList.size() > 0) {
                        subirImagenes(actaId, progressDialog, imprimirLuego);
                    } else {
                        progressDialog.dismiss();
                        actaYaGuardada = true;
                        if (imprimirLuego) {
                            showToast("Acta guardada. Continuando con impresión...");
                            runOnUiThread(() -> checkBluetoothPermission());
                        } else {
                            new AlertDialog.Builder(ActaInfraccionActivity.this)
                                    .setTitle("Acta Guardada")
                                    .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        }
                    }
                }
            }, 15000); // 15 segundos

            Call<RespuestaSubirFirma> call = apiService.subirFirmaInfractor("subirFirmaInfractor", actaId, firmaBase64);
            call.enqueue(new Callback<RespuestaSubirFirma>() {
                @Override
                public void onResponse(Call<RespuestaSubirFirma> call, Response<RespuestaSubirFirma> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d(TAG, "Firma subida exitosamente");
                        // Firma subida correctamente, ahora subir imágenes si hay
                        if (imageList.size() > 0) {
                            subirImagenes(actaId, progressDialog, imprimirLuego);
                        } else {
                            progressDialog.dismiss();
                            actaYaGuardada = true;

                            if (imprimirLuego) {
                                showToast("Acta guardada con firma. Imprimiendo...");
                                runOnUiThread(() -> {
                                    checkBluetoothPermission();
                                });
                            } else {
                                new AlertDialog.Builder(ActaInfraccionActivity.this)
                                        .setTitle("Acta Guardada")
                                        .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente con firma.")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al subir firma: " + (response.body() != null ? response.body().getMessage() : "Error desconocido"));

                        // Continuar con imágenes a pesar del error
                        if (imageList.size() > 0) {
                            subirImagenes(actaId, progressDialog, imprimirLuego);
                        } else {
                            progressDialog.dismiss();
                            actaYaGuardada = true;

                            if (imprimirLuego) {
                                showToast("Acta guardada, pero hubo un error al subir la firma. Imprimiendo...");
                                runOnUiThread(() -> {
                                    checkBluetoothPermission();
                                });
                            } else {
                                new AlertDialog.Builder(ActaInfraccionActivity.this)
                                        .setTitle("Acta Guardada")
                                        .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error al subir la firma.")
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .setCancelable(false)
                                        .show();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<RespuestaSubirFirma> call, Throwable t) {
                    Log.e(TAG, "Error de conexión al subir firma: " + t.getMessage(), t);

                    // Continuar con imágenes a pesar del error
                    if (imageList.size() > 0) {
                        subirImagenes(actaId, progressDialog, imprimirLuego);
                    } else {
                        progressDialog.dismiss();
                        actaYaGuardada = true;

                        if (imprimirLuego) {
                            showToast("Acta guardada, pero error de conexión al subir la firma. Imprimiendo...");
                            runOnUiThread(() -> {
                                checkBluetoothPermission();
                            });
                        } else {
                            new AlertDialog.Builder(ActaInfraccionActivity.this)
                                    .setTitle("Acta Guardada")
                                    .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error de conexión al subir la firma.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al preparar o subir la firma: " + e.getMessage(), e);

            // Continuar a pesar del error
            if (imageList.size() > 0) {
                subirImagenes(actaId, progressDialog, imprimirLuego);
            } else {
                progressDialog.dismiss();
                actaYaGuardada = true;

                if (imprimirLuego) {
                    showToast("Acta guardada, pero hubo un error al procesar la firma. Imprimiendo...");
                    runOnUiThread(() -> {
                        checkBluetoothPermission();
                    });
                } else {
                    new AlertDialog.Builder(ActaInfraccionActivity.this)
                            .setTitle("Acta Guardada")
                            .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error al procesar la firma.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                }
            }
        }
    }
    private boolean isNetworkAvailableCustom() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }


    private String obtenerUrlFirma(String legajo) {
        return "http://31.97.172.185/test_firma.php?legajo=" + legajo;
    }
    private void precargarFirma(String legajo, Runnable onReady) {
        if (!isNetworkAvailableCustom()) {
            Log.d("Firma", "Sin conexión: no se descarga la firma");
            // Igual avisamos que "terminó" para que el flujo pueda seguir
            if (onReady != null) onReady.run();
            return;
        }

        new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... params) {
                try {
                    String urlFirma = obtenerUrlFirma(legajo);
                    URL url = new URL(urlFirma);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setDoInput(true);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(response.toString());
                        if (json.getBoolean("success")) {
                            String base64 = json.getString("firma");
                            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            if (bitmap != null) {
                                int newWidth = 400;
                                int newHeight = (bitmap.getHeight() * newWidth) / bitmap.getWidth();
                                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                                Bitmap bwBitmap = convertirABlancoYNegro(scaled);
                                bitmap.recycle();
                                scaled.recycle();
                                return PrinterHelper.decodeBitmap(bwBitmap);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("Firma", "Error al descargar firma", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(byte[] command) {
                if (command != null) {
                    comandoFirma = command;
                    Log.d("Firma", "✅ Firma descargada correctamente");
                } else {
                    Log.e("Firma", "⚠️ No se pudo descargar la firma");
                }

                // Avisamos que terminó (con o sin éxito)
                if (onReady != null) {
                    onReady.run();
                }
            }



    private Bitmap convertirABlancoYNegro(Bitmap original) {
                int width = original.getWidth();
                int height = original.getHeight();
                Bitmap bw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                int threshold = 128;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = original.getPixel(x, y);
                        int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                        bw.setPixel(x, y, gray < threshold ? Color.BLACK : Color.WHITE);
                    }
                }
                return bw;
            }
        }.execute();
    }


    // Método optimizado para subir imágenes sin bloqueos
    private void subirImagenes(String actaId, ProgressDialog progressDialog, boolean imprimirLuego) {
        // Verificar si hay imágenes para subir
        if (imageList == null || imageList.isEmpty()) {
            Log.d(TAG, "No hay imágenes para subir, finalizando proceso");
            progressDialog.dismiss();
            actaYaGuardada = true;

            if (imprimirLuego) {
                showToast("Acta guardada. Imprimiendo...");
                runOnUiThread(() -> {
                    checkBluetoothPermission();
                });
            } else {
                new AlertDialog.Builder(ActaInfraccionActivity.this)
                        .setTitle("Acta Guardada")
                        .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente.")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
            return;
        }

        try {
            // Timeout para esta operación específica
            new Handler().postDelayed(() -> {
                if (progressDialog.isShowing()) {
                    // Si todavía estamos en esta operación después de 15 segundos, seguir adelante
                    Log.d(TAG, "Timeout al subir imágenes, continuando con el proceso");
                    progressDialog.dismiss();
                    actaYaGuardada = true;
                    if (imprimirLuego) {
                        showToast("Acta guardada. Continuando con impresión...");
                        runOnUiThread(() -> checkBluetoothPermission());
                    } else {
                        new AlertDialog.Builder(ActaInfraccionActivity.this)
                                .setTitle("Acta Guardada")
                                .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente.")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }
                }
            }, 15000); // 15 segundos

            List<String> imagenesBase64 = ImageHelper.urisToBase64List(this, imageList);
            Log.d(TAG, "Intentando subir " + imagenesBase64.size() + " imágenes para acta ID: " + actaId);

            Call<RespuestaSubirImagen> call = apiService.subirImagen("subirImagen", actaId, imagenesBase64);
            call.enqueue(new Callback<RespuestaSubirImagen>() {
                @Override
                public void onResponse(Call<RespuestaSubirImagen> call, Response<RespuestaSubirImagen> response) {
                    progressDialog.dismiss();
                    Log.d(TAG, "Respuesta completa: " + response.raw().toString());
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d(TAG, "Imágenes subidas exitosamente");
                        actaYaGuardada = true;

                        if (imprimirLuego) {
                            showToast("Acta guardada con imágenes. Imprimiendo...");
                            runOnUiThread(() -> {
                                checkBluetoothPermission();
                            });
                        } else {
                            new AlertDialog.Builder(ActaInfraccionActivity.this)
                                    .setTitle("Acta Guardada")
                                    .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada exitosamente con imágenes.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        }
                    } else {
                        Log.e(TAG, "Error al subir imágenes: " + (response.body() != null ? response.body().getMessage() : "Error desconocido"));
                        actaYaGuardada = true; // El acta se guardó aunque las imágenes fallaron

                        if (imprimirLuego) {
                            showToast("Acta guardada, pero hubo un error al subir imágenes. Imprimiendo...");
                            runOnUiThread(() -> {
                                checkBluetoothPermission();
                            });
                        } else {
                            new AlertDialog.Builder(ActaInfraccionActivity.this)
                                    .setTitle("Acta Guardada")
                                    .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error al subir imágenes.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<RespuestaSubirImagen> call, Throwable t) {
                    Log.e(TAG, "Error de conexión al subir imágenes: " + t.getMessage(), t);
                    progressDialog.dismiss();
                    actaYaGuardada = true; // El acta se guardó aunque las imágenes fallaron

                    if (imprimirLuego) {
                        showToast("Acta guardada, pero error de conexión al subir imágenes. Imprimiendo...");
                        runOnUiThread(() -> {
                            checkBluetoothPermission();
                        });
                    } else {
                        new AlertDialog.Builder(ActaInfraccionActivity.this)
                                .setTitle("Acta Guardada")
                                .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error de conexión al subir imágenes.")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al preparar o subir imágenes: " + e.getMessage(), e);
            progressDialog.dismiss();
            actaYaGuardada = true;

            if (imprimirLuego) {
                showToast("Acta guardada, pero error al procesar imágenes. Imprimiendo...");
                runOnUiThread(() -> {
                    checkBluetoothPermission();
                });
            } else {
                new AlertDialog.Builder(ActaInfraccionActivity.this)
                        .setTitle("Acta Guardada")
                        .setMessage("Acta N° " + ultimaActaImpresa.getNumeroActa() + " guardada, pero hubo un error al procesar imágenes.")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }


    private Uri copiarImagenAStoragePrivado(Uri src) throws IOException {
        File dir = new File(getFilesDir(), "offline_imgs");
        if (!dir.exists()) dir.mkdirs();

        String name = "IMG_" + System.currentTimeMillis() + ".jpg";
        File dst = new File(dir, name);

        try (java.io.InputStream in = getContentResolver().openInputStream(src);
             java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {

            if (in == null) throw new IOException("No se pudo abrir InputStream de: " + src);

            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }

        return Uri.fromFile(dst); // ✅ file://... siempre legible luego
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkCameraAndStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verificar permisos de almacenamiento y cámara
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        },
                        REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                        REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
        }

        // Si ya tenemos los permisos, continuar con la impresión
        connectAndPrint();
    }

    private void connectAndPrint() {
        if (!printerConnector.isBluetoothAvailable()) {
            showToast("Bluetooth no disponible en este dispositivo");
            return;
        }

        if (!printerConnector.isBluetoothEnabled()) {
            showToast("Por favor, active el Bluetooth para imprimir");
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Conectando con la impresora...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        printerConnector.setOnPrinterConnectionListener(new BluetoothPrinterConnector.OnPrinterConnectionListener() {
            @Override
            public void onConnected(OutputStream stream) {
                outputStream = stream;
                progressDialog.dismiss();
                showToast("Conectado a la impresora");
                printActa();
            }

            @Override
            public void onConnectionFailed(String errorMessage) {
                progressDialog.dismiss();
                showAlert("Error de conexión", errorMessage);
            }

            @Override
            public void onDisconnected() {
                outputStream = null;
                Log.d(TAG, "Desconectado de la impresora");
            }
        });

        printerConnector.connect();
    }


    // ===============================
// ✅ printActa() COMPLETO (FIX lambda final/effectively final)
// ===============================
    private void printActa() {
        final ActaInfraccionData acta = (ultimaActaImpresa != null) ? ultimaActaImpresa : generateActaData();

        new Thread(() -> {
            try {
                if (outputStream == null) {
                    runOnUiThread(() -> showToast("Sin conexión a la impresora"));
                    return;
                }

                final int lineWidth = 32;

                // 🔹 Determinar tipo de actuación
                String tipoActaStr = (acta.getTipoActa() != null) ? acta.getTipoActa().trim().toUpperCase() : "INFRACCION";
                final boolean esInspeccion = "INSPECCION".equals(tipoActaStr);

                // ============================
                // ✅ 0) DATOS INSPECTOR (PARA IMPRIMIR)
                // ============================
                final String nombreInspector   = getIntent().getStringExtra("NOMBRE_INSPECTOR");
                final String apellidoInspector = getIntent().getStringExtra("APELLIDO_INSPECTOR");
                String legajoInspectorTmp      = getIntent().getStringExtra("LEGAJO_INSPECTOR");


                // Normalizar legajo (SIN usarlo directo en lambda)
                if (legajoInspectorTmp == null || legajoInspectorTmp.trim().isEmpty()) {
                    legajoInspectorTmp = "0";
                    Log.e("Firma", "⚠️ Legajo inspector no recibido, usando 0");
                } else {
                    legajoInspectorTmp = legajoInspectorTmp.trim();
                }

                // ✅ FINAL para usar en lambda
                final String legajoInspectorFinal = legajoInspectorTmp;

                // Nombre completo inspector
                String inspectorNombreTmp = "";
                if (nombreInspector != null) inspectorNombreTmp += nombreInspector.trim();
                if (apellidoInspector != null && !apellidoInspector.trim().isEmpty()) {
                    inspectorNombreTmp += (inspectorNombreTmp.isEmpty() ? "" : " ") + apellidoInspector.trim();
                }
                if (inspectorNombreTmp.trim().isEmpty()) inspectorNombreTmp = "No informado";

                final String inspectorNombreFinal = inspectorNombreTmp;
                final String idMostrarFinal = (inspectorId != null && !inspectorId.trim().isEmpty()) ? inspectorId.trim() : "No informado";

                Log.d("Firma", "🆔 Descargando firma para legajo: " + legajoInspectorFinal);

                // ============================
                // 1️⃣ Descargar firma primero
                // ============================
                precargarFirma(legajoInspectorFinal, () -> {
                    try {
                        // ============================
                        // 2️⃣ Comenzar IMPRESIÓN
                        // ============================
                        outputStream.write(PrinterHelper.Commands.ESC_INIT);
                        outputStream.write(PrinterHelper.Commands.TEXT_FONT_B);
                        outputStream.write(PrinterHelper.Commands.ESC_ALIGN_CENTER);

                        // Logo
                        try {
                            Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), acta.getLogoResourceId());
                            byte[] logoCommand = PrinterHelper.decodeBitmap(logoBitmap);
                            outputStream.write(logoCommand);
                            outputStream.write(PrinterHelper.Commands.ESC_ALIGN_LEFT);
                            outputStream.write(PrinterHelper.Commands.FEED_LINE);
                        } catch (Exception e) {
                            Log.e(TAG, "Error imprimiendo logo: " + e.getMessage());
                        }

                        // Encabezado institucional
                        printLine("MUNICIPALIDAD DE POSADAS", "", lineWidth);
                        printLine("Secretaria de Planificacion Estrategica y Territorial", "", lineWidth);
                        printLine("Direccion de Obras Privadas", "", lineWidth);
                        printLine("--------------------------------", "", lineWidth);

                        outputStream.write(PrinterHelper.Commands.FEED_LINE);

                        // Encabezado del acta según tipo
                        String tituloActa = esInspeccion
                                ? "ACTA DE INSPECCION NUMERO: "
                                : "ACTA DE INFRACCION NUMERO: ";

                        printLine(tituloActa, acta.getNumeroActa(), lineWidth);
                        printLine("Fecha: ", acta.getFecha(), lineWidth);
                        printLine("Hora: ", acta.getHora(), lineWidth);

                        if (acta.getTipoActa() != null && !acta.getTipoActa().isEmpty()) {
                            printLine("Tipo de actuacion: ", acta.getTipoActa(), lineWidth);
                        }

                        printLine("--------------------------------", "", lineWidth);
                        outputStream.write(PrinterHelper.Commands.FEED_LINE);



                        // Datos del propietario / inmueble
                        printLine("PROPIETARIO", "", lineWidth);
                        printLine("Nombre: ", acta.getPropietario(), lineWidth);
                        printLine("Domicilio: ", acta.getDomicilio(), lineWidth);
                        printLine("Ubicacion: ", acta.getLugarInfraccion(), lineWidth);

                        outputStream.write(PrinterHelper.Commands.FEED_LINE);
                        printLine("DATOS DEL INMUEBLE", "", lineWidth);
                        printLine("Seccion: ", acta.getSeccion(), lineWidth);
                        printLine("Chacra: ", acta.getChacra(), lineWidth);
                        printLine("Manzana: ", acta.getManzana(), lineWidth);
                        printLine("Parcela: ", acta.getParcela(), lineWidth);
                        printLine("Lote: ", acta.getLote(), lineWidth);
                        printLine("Partida: ", acta.getPartida(), lineWidth);
                        printLine("--------------------------------", "", lineWidth);

                        outputStream.write(PrinterHelper.Commands.FEED_LINE);

                        // Bloque central según tipo
                        if (esInspeccion) {
                            // INSPECCIÓN
                            printLine("RESULTADO DE INSPECCION", "", lineWidth);

                            String resultado = (acta.getResultadoInspeccion() != null)
                                    ? acta.getResultadoInspeccion().trim()
                                    : "";

                            if (!resultado.isEmpty()) {
                                printLine("", resultado, lineWidth);
                            } else {
                                printLine("", "Sin detalle de resultado declarado.", lineWidth);
                            }

                            outputStream.write(PrinterHelper.Commands.FEED_LINE);

                            if (acta.getBoletaInspeccion() != null && !acta.getBoletaInspeccion().isEmpty()) {
                                printLine("Boleta de Inspeccion Numero: ", acta.getBoletaInspeccion(), lineWidth);
                            }

                        } else {
                            // INFRACCIÓN
                            printLine("CAUSAS / FALTAS", "", lineWidth);

                            if (acta.isCartelObra())             printLine("- ", "Cartel de Obra Reglamentario", lineWidth);
                            if (acta.isDispositivosSeguridad())  printLine("- ", "Dispositivos de Seguridad", lineWidth);
                            if (acta.isNumeroPermiso())          printLine("- ", "Numero Permiso", lineWidth);
                            if (acta.isMaterialesVereda())       printLine("- ", "Materiales en Vereda", lineWidth);
                            if (acta.isCercoObra())              printLine("- ", "Cerco de Obra", lineWidth);
                            if (acta.isPlanosAprobados())        printLine("- ", "Planos Aprobados", lineWidth);
                            if (acta.isDirectorObra())           printLine("- ", "Director de Obra", lineWidth);
                            if (acta.isVarios())                 printLine("- ", "Varios", lineWidth);
                            if (acta.isIncumplimiento())        printLine("- ", "Incumplimiento", lineWidth);
                            if (acta.isClausuraPreventiva())    printLine("- ", "Clausura preventiva", lineWidth);

                            outputStream.write(PrinterHelper.Commands.FEED_LINE);

                            if (acta.getBoletaInspeccion() != null && !acta.getBoletaInspeccion().isEmpty()) {
                                printLine("Boleta de Infraccion Numero: ", acta.getBoletaInspeccion(), lineWidth);
                            }
                        }

                        // Observaciones
                        if (acta.getObservaciones() != null && !acta.getObservaciones().isEmpty()) {
                            outputStream.write(PrinterHelper.Commands.FEED_LINE);
                            printLine("Observaciones:", "", lineWidth);
                            printLine("", acta.getObservaciones(), lineWidth);
                        }

                        outputStream.write(PrinterHelper.Commands.FEED_LINE);

                        // Leyenda final
                        if (esInspeccion) {
                            printLine("", "El propietario debera adecuar la obra a las normas del C.E.P. y demas disposiciones vigentes.", lineWidth);
                        } else {
                            printLine("", "El infractor debera comparecer ante el Tribunal de Faltas Municipal en los plazos establecidos.", lineWidth);
                        }

                        outputStream.write(PrinterHelper.Commands.FEED_LINE);
                        printLine("--------------------------------", "", lineWidth);
                        outputStream.write(PrinterHelper.Commands.ESC_ALIGN_CENTER);

                        // ============================
                        // 3️⃣ IMPRIMIR LA FIRMA
                        // ============================
                        if (comandoFirma != null) {
                            Log.d("Firma", "🖊️ Imprimiendo firma...");
                            outputStream.write(comandoFirma);
                            outputStream.write(PrinterHelper.Commands.FEED_LINE);
                        } else {
                            Log.e("Firma", "⚠️ NO se imprimió la firma (null)");
                        }

                        printLine("_______________________", "", lineWidth);
                        printLine("Firma del Inspector", "", lineWidth);
                        // ============================
                        // ✅ BLOQUE INSPECTOR (NUEVO)
                        // ============================
                        printLine("INSPECTOR", "", lineWidth);
                        printLine("Nombre: ", inspectorNombreFinal, lineWidth);
                        printLine("Legajo: ", legajoInspectorFinal, lineWidth);
                        printLine("--------------------------------", "", lineWidth);
                        outputStream.write(PrinterHelper.Commands.FEED_LINE);
                        outputStream.write(new byte[]{0x0A, 0x0A, 0x0A});
                        outputStream.write(PrinterHelper.Commands.FEED_PAPER_AND_CUT);

                        // Diálogo final
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(ActaInfraccionActivity.this)
                                    .setTitle("Acta impresa")
                                    .setMessage("El acta se imprimió correctamente.\n¿Deseás volver a imprimirla?")
                                    .setPositiveButton("Sí", (d, w) -> checkBluetoothPermission())
                                    .setNegativeButton("No", (d, w) -> finish())
                                    .setCancelable(false)
                                    .show();
                        });

                    } catch (Exception ex) {
                        Log.e(TAG, "Error al imprimir acta: " + ex.getMessage(), ex);
                    } finally {
                        printerConnector.close();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error general printActa: " + e.getMessage(), e);
            }
        }).start();
    }





    private void printLine(String label, String value, int lineWidth) throws IOException {
        String text = label + value;

        // Procesamos el texto para que se ajuste al ancho de la impresora
        if (text.length() > lineWidth) {
            // Si el texto es más largo que el ancho de línea, lo dividimos
            int currentPos = 0;
            while (currentPos < text.length()) {
                int endPos = Math.min(currentPos + lineWidth, text.length());
                String line = text.substring(currentPos, endPos);
                outputStream.write(line.getBytes());
                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                currentPos = endPos;
            }
        } else {
            // Si el texto es más corto, lo imprimimos directamente
            outputStream.write(text.getBytes());
            outputStream.write(PrinterHelper.Commands.FEED_LINE);
        }
    }

    private ActaInfraccionData generateActaData() {
        ActaInfraccionData acta = new ActaInfraccionData();

        // Número de acta (si está vacío, genera uno nuevo por las dudas)
        String numeroActa = etBoletaInspeccion.getText().toString().trim();
        if (numeroActa.isEmpty()) {
            numeroActa = generateActaNumber();
            etBoletaInspeccion.setText(numeroActa);
        }
        acta.setNumeroActa(numeroActa);

        acta.setPropietario(etPropietario.getText().toString());
        acta.setDomicilio(etDomicilio.getText().toString());
        
        // Handle "Same Address" logic
        if (cbMismoLugar != null && cbMismoLugar.isChecked()) {
            acta.setLugarInfraccion(etDomicilio.getText().toString());
        } else {
            acta.setLugarInfraccion(etLugarInfraccion.getText().toString());
        }

        // Fecha y hora actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat stf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();
        acta.setFecha(sdf.format(now));
        acta.setHora(stf.format(now));

        // Datos del inmueble
        acta.setSeccion(etSeccion.getText().toString());
        acta.setChacra(etChacra.getText().toString());
        acta.setManzana(etManzana.getText().toString());
        acta.setParcela(etParcela.getText().toString());
        acta.setLote(etLote.getText().toString());
        acta.setPartida(etPartida.getText().toString());

        // Por defecto, mismos datos que el propietario para el infractor
        acta.setInfractorNombre(etPropietario.getText().toString());
        acta.setInfractorDomicilio(etDomicilio.getText().toString());

        // ===============================
        //   DEDUCIR TIPO DE ACTUACIÓN
        // ===============================

        // ¿Hay causas de INFRACCIÓN? (checkboxes)
        boolean hayCausaInfraccion =
                cbCartelObra.isChecked() ||
                        cbDispositivosSeguridad.isChecked() ||
                        cbNumeroPermiso.isChecked() ||
                        cbMaterialesVereda.isChecked() ||
                        cbCercoObra.isChecked() ||
                        cbPlanosAprobados.isChecked() ||
                        cbDirectorObra.isChecked() ||
                        cbVarios.isChecked() ||
                        // ✅ NUEVOS
                        cbIncumplimiento.isChecked() ||
                        cbClausuraPreventiva.isChecked();
        // ¿Hay resultado de INSPECCIÓN? (texto del resumen)
        boolean hayCausaInspeccion = false;
        String detalleInspeccion = "";
        if (tvCausasSeleccionadasInspeccion != null) {
            detalleInspeccion = tvCausasSeleccionadasInspeccion.getText().toString().trim();
            hayCausaInspeccion =
                    !detalleInspeccion.isEmpty() &&
                            !detalleInspeccion.equalsIgnoreCase("Ningún resultado seleccionado");
        }

        String tipoActa;
        if (isModeInspeccion) {
            tipoActa = "INSPECCION";
            // Limpiar datos de infracción por seguridad (opcional, pero limpio)
            acta.setResultadoInspeccion(detalleInspeccion);
            
            // Clear infraction flags
            acta.setCartelObra(false);
            acta.setDispositivosSeguridad(false);
            // ... (etc) - Just rely on logic below
            
        } else {
            tipoActa = "INFRACCION";
            acta.setResultadoInspeccion("");
        }
        acta.setTipoActa(tipoActa);

        // Causas de infracción (checkboxes) 
        // Solo asignamos si estamos en modo Infracción, o si queremos guardarlos igual "por si acaso" el usuario cambió de tab pero dejó marcados?
        // El usuario pidió "seleccionar uno o otro". Mejor guardar solo lo del modo activo.
        
        if (!isModeInspeccion) {
            acta.setCartelObra(cbCartelObra.isChecked());
            acta.setDispositivosSeguridad(cbDispositivosSeguridad.isChecked());
            acta.setNumeroPermiso(cbNumeroPermiso.isChecked());
            acta.setMaterialesVereda(cbMaterialesVereda.isChecked());
            acta.setCercoObra(cbCercoObra.isChecked());
            acta.setPlanosAprobados(cbPlanosAprobados.isChecked());
            acta.setDirectorObra(cbDirectorObra.isChecked());
            acta.setVarios(cbVarios.isChecked());
            acta.setIncumplimiento(cbIncumplimiento.isChecked());
            acta.setClausuraPreventiva(cbClausuraPreventiva.isChecked());
        } else {
            // En modo inspeccion, todo a false
            acta.setCartelObra(false);
            acta.setDispositivosSeguridad(false);
            acta.setNumeroPermiso(false);
            acta.setMaterialesVereda(false);
            acta.setCercoObra(false);
            acta.setPlanosAprobados(false);
            acta.setDirectorObra(false);
            acta.setVarios(false);
            acta.setIncumplimiento(false);
            acta.setClausuraPreventiva(false);
        }
        acta.setObservaciones(etObservaciones.getText().toString());
        acta.setBoletaInspeccion(etBoletaInspeccion.getText().toString());
        acta.setLogoResourceId(LOGO_RESOURCE_ID);

        // Firma si existe
        if (firmaInfractorBitmap != null) {
            acta.setFirmaInfractor(firmaInfractorBitmap);
        }

        // Imágenes si existen
        if (imageList != null && !imageList.isEmpty()) {
            acta.setImagenesPrueba(imageList);
        }

        return acta;
    }





    private String generateActaNumber() {
        return new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(new Date()) +
                new Random().nextInt(90);
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void showAlert(String title, String message) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                connectAndPrint();
            } else {
                showAlert("Permisos necesarios", "Los permisos de Bluetooth son necesarios para imprimir");
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                showAlert("Permisos necesarios", "Los permisos de almacenamiento son necesarios para seleccionar imágenes");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (printerConnector != null) {
            printerConnector.close();
        }
    }
}