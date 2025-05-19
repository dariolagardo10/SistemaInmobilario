package es.rcti.demoprinterplus.sistemainmobilario;

import retrofit2.Call;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

    private static final String TAG = "ActaInfraccion";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int LOGO_RESOURCE_ID = R.drawable.ic_launcher;
    private static final int MAX_IMAGES = 2;

    private EditText etPropietario, etDomicilio, etLugarInfraccion, etSeccion, etChacra, etManzana,
            etParcela, etLote, etPartida, etObservaciones, etBoletaInspeccion;
    private CheckBox cbCartelObra, cbDispositivosSeguridad, cbNumeroPermiso, cbMaterialesVereda,
            cbCercoObra, cbPlanosAprobados, cbDirectorObra, cbVarios;
    private Button btnImprimir, btnGuardar, btnClearSignature, btnAddImages;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acta_infraccion);

        parcelaData = getIntent().getStringExtra("PARCEL_DATA");
        printerConnector = new BluetoothPrinterConnector(this);
        apiClient = new ApiClient(this);

        initViews();
        fillInitialData();
        setupListeners();
        checkCameraAndStoragePermissions();
    }

    private void initViews() {
        etPropietario = findViewById(R.id.etPropietario);
        etDomicilio = findViewById(R.id.etDomicilio);
        etLugarInfraccion = findViewById(R.id.etLugarInfraccion);
        etSeccion = findViewById(R.id.etSeccion);
        etChacra = findViewById(R.id.etChacra);
        etManzana = findViewById(R.id.etManzana);
        etParcela = findViewById(R.id.etParcela);
        etLote = findViewById(R.id.etLote);
        etPartida = findViewById(R.id.etPartida);
        etObservaciones = findViewById(R.id.etObservaciones);
        etBoletaInspeccion = findViewById(R.id.etBoletaInspeccion);

        cbCartelObra = findViewById(R.id.cbCartelObra);
        cbDispositivosSeguridad = findViewById(R.id.cbDispositivosSeguridad);
        cbNumeroPermiso = findViewById(R.id.cbNumeroPermiso);
        cbMaterialesVereda = findViewById(R.id.cbMaterialesVereda);
        cbCercoObra = findViewById(R.id.cbCercoObra);
        cbPlanosAprobados = findViewById(R.id.cbPlanosAprobados);
        cbDirectorObra = findViewById(R.id.cbDirectorObra);
        cbVarios = findViewById(R.id.cbVarios);

        btnImprimir = findViewById(R.id.btnImprimir);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Nuevos componentes para firma e imágenes
        signatureView = findViewById(R.id.signatureView);
        btnClearSignature = findViewById(R.id.btnClearSignature);
        btnAddImages = findViewById(R.id.btnAddImages);
        rvImages = findViewById(R.id.rvImages);

        // Configurar RecyclerView
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(this, imageList, position -> {
            imageList.remove(position);
            imageAdapter.notifyItemRemoved(position);
        });
        rvImages.setAdapter(imageAdapter);

        // Inicializar servicio Retrofit
        apiService = RetrofitClient.getClient().create(OracleApiService.class);

        // Configurar el SignatureView para mayor sensibilidad
        if (signatureView != null) {
            // Aumentar la sensibilidad del SignatureView
            signatureView.setStrokeWidth(2.5f); // Línea más fina para mayor precisión
            signatureView.setMinWidth(1.0f);  // Ancho mínimo reducido aún más
            signatureView.setMaxWidth(4.0f);    // Ancho máximo reducido para mayor precisión
            signatureView.setVelocityFilterWeight(0.8f); // Valor más bajo para mayor sensibilidad

            // Configurar colores
            signatureView.setPenColor(Color.BLUE);
            signatureView.setBackgroundColor(Color.WHITE);

            // CRÍTICO: Desactivar el scrolling durante la firma
            signatureView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Esta línea es crucial - evita que el ScrollView intercepte los eventos táctiles
                    v.getParent().requestDisallowInterceptTouchEvent(true);

                    // Solo permitir scroll nuevamente cuando se levanta el dedo
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }

                    // Devolver false para que el evento continúe procesándose por el SignatureView
                    return false;
                }
            });
        }
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
        btnImprimir.setOnClickListener(v -> {
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

        btnGuardar.setOnClickListener(v -> {
            if (validateForm()) {
                saveActa(false); // guardar sin imprimir
            }
        });

        btnClearSignature.setOnClickListener(v -> {
            signatureView.clear();
        });

        // Nuevo listener para el botón de expandir firma
        Button btnExpandSignature = findViewById(R.id.btnExpandSignature);
        btnExpandSignature.setOnClickListener(v -> {
            showFullscreenSignatureDialog();
        });

        // Modificar para abrir la opción de cámara o galería
        btnAddImages.setOnClickListener(v -> {
            // Verificar si ya alcanzamos el límite de imágenes
            if (imageList.size() >= MAX_IMAGES) {
                showToast("Solo se permiten " + MAX_IMAGES + " imágenes");
                return;
            }

            // Mostrar opciones
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Agregar imagen")
                    .setItems(new CharSequence[]{"Tomar foto", "Seleccionar de galería"},
                            (dialog, which) -> {
                                if (which == 0) {
                                    // Opción de cámara
                                    dispatchTakePictureIntent();
                                } else {
                                    // Opción de galería
                                    openGallery();
                                }
                            })
                    .show();
        });

        // Configurar el clic en el SignatureView para abrir en pantalla completa
        signatureView.setOnClickListener(v -> {
            showFullscreenSignatureDialog();
        });
    }

    private void showFullscreenSignatureDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_signature_fullscreen);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            // Configuración adicional para asegurar pantalla completa
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        SignatureView fullScreenSignature = dialog.findViewById(R.id.fullscreenSignatureView);
        Button btnClear = dialog.findViewById(R.id.btnClearFullscreenSignature);
        Button btnSave = dialog.findViewById(R.id.btnSaveSignature);
        Button btnCancel = dialog.findViewById(R.id.btnCancelSignature);

        // Configuración óptima para mejor sensibilidad y precisión
        fullScreenSignature.setStrokeWidth(7f);  // Línea más gruesa para mejor visibilidad
        fullScreenSignature.setPenColor(Color.BLUE);

        // Si ya hay una firma, transferirla al control de pantalla completa
        if (signatureView.hasSignature()) {
            Bitmap currentSignature = signatureView.getBitmap();
            if (currentSignature != null) {
                fullScreenSignature.setBitmap(currentSignature);
            }
        }

        btnSave.setOnClickListener(v -> {
            if (!fullScreenSignature.hasSignature()) {
                Toast.makeText(this, "Por favor firme antes de guardar", Toast.LENGTH_SHORT).show();
                return;
            }

            // Transferir la firma al control original
            Bitmap signature = fullScreenSignature.getBitmap();
            if (signature != null) {
                signatureView.clear(); // Limpiar firma anterior
                signatureView.setBitmap(signature);
            }
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
        if (etPropietario.getText().toString().trim().isEmpty() ||
                etLugarInfraccion.getText().toString().trim().isEmpty()) {
            showToast("Complete los campos obligatorios");
            return false;
        }
        if (!cbCartelObra.isChecked() && !cbDispositivosSeguridad.isChecked() &&
                !cbNumeroPermiso.isChecked() && !cbMaterialesVereda.isChecked() &&
                !cbCercoObra.isChecked() && !cbPlanosAprobados.isChecked() &&
                !cbDirectorObra.isChecked() && !cbVarios.isChecked()) {
            showToast("Seleccione al menos una causa de infracción");
            return false;
        }
        //  if (!signatureView.hasSignature()) {
        //    showToast("Se requiere la firma del infractor");
        //   return false;
        //  }
        return true;
    }

    private void guardarYImprimir() {
        if (!validateForm()) return;

        // Verificar conectividad
        if (!isNetworkAvailable()) {
            showToast("No hay conexión a internet. No se puede guardar el acta.");
            return;
        }

        actaYaGuardada = false;
        final ActaInfraccionData acta = generateActaData();
        // Añadir firma e imágenes al objeto acta
        acta.setFirmaInfractor(signatureView.getBitmap());
        acta.setImagenesPrueba(imageList);

        ultimaActaImpresa = acta;

        // Obtener ID del inspector (o usar valor provisional)
        SharedPreferences preferences = getSharedPreferences("ActasInfraccionApp", MODE_PRIVATE);
        String inspectorId = preferences.getString("inspector_id", "1"); // Valor provisional: 1
        acta.setInspectorId(inspectorId);

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Guardando acta...");
        progressDialog.setCancelable(true); // Permitir cancelar si el usuario lo desea
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

        // Registramos el tiempo de inicio para diagnóstico
        final long startTime = System.currentTimeMillis();
        Log.d(TAG, "Iniciando guardar acta: " + startTime);

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
                            subirFirma(String.valueOf(actaId), acta.getFirmaInfractor(), progressDialog, true);
                        } else {
                            progressDialog.dismiss();
                            actaYaGuardada = true;
                            showToast("Acta guardada. Imprimiendo...");
                            runOnUiThread(() -> {
                                checkBluetoothPermission();
                            });
                        }
                    } else {
                        progressDialog.dismiss();
                        String error = response.optString("error", "Error desconocido");
                        showToast("Error al guardar: " + error);
                        Log.e(TAG, "Error al guardar acta: " + error);
                    }
                } catch (JSONException e) {
                    progressDialog.dismiss();
                    showToast("Error al procesar la respuesta del servidor");
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
        // Añadir firma e imágenes
        acta.setFirmaInfractor(signatureView.getBitmap());
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

    // Método subirFirma mejorado para evitar bloqueos
    private void subirFirma(String actaId, Bitmap firma, ProgressDialog progressDialog, boolean imprimirLuego) {
        // Verificamos si hay firma y es válida
        if (firma == null || firma.isRecycled() ||
                firma.getWidth() <= 0 || firma.getHeight() <= 0 ||
                (signatureView != null && !signatureView.hasSignature())) {

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

    private void printActa() {
        // Crear acta
        ActaInfraccionData acta = ultimaActaImpresa != null ? ultimaActaImpresa : generateActaData();

        new Thread(() -> {
            try {
                if (outputStream == null) {
                    runOnUiThread(() -> showToast("Sin conexión a la impresora"));
                    return;
                }

                int lineWidth = 32; // Caracteres por línea para impresoras de 57mm

                // Inicializar la impresora
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
                    Log.e(TAG, "Error al imprimir logo: " + e.getMessage());
                }

                // Contenido
                printLine("MUNICIPALIDAD DE POSADAS", "", lineWidth);
                printLine("Dirección de Obras Particulares", "", lineWidth);
                printLine("", "", lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                printLine("ACTA DE INFRACCIÓN Nº: ", acta.getNumeroActa(), lineWidth);
                printLine("Fecha: ", acta.getFecha(), lineWidth);
                printLine("Hora: ", acta.getHora(), lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                printLine("PROPIETARIO", "", lineWidth);
                printLine("Nombre y Apellido: ", acta.getPropietario(), lineWidth);
                printLine("Domicilio: ", acta.getDomicilio(), lineWidth);
                printLine("Ubicación: ", acta.getLugarInfraccion(), lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                printLine("DATOS DEL INMUEBLE", "", lineWidth);
                printLine("Sección: ", acta.getSeccion(), lineWidth);
                printLine("Chacra: ", acta.getChacra(), lineWidth);
                printLine("Manzana: ", acta.getManzana(), lineWidth);
                printLine("Parcela: ", acta.getParcela(), lineWidth);
                printLine("Lote: ", acta.getLote(), lineWidth);
                printLine("Partida: ", acta.getPartida(), lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                printLine("CAUSAS/FALTAS", "", lineWidth);
                if (acta.isCartelObra())
                    printLine("- ", "Cartel de Obra Reglamentario (punto 2.2.12 C.E.P.)", lineWidth);
                if (acta.isDispositivosSeguridad())
                    printLine("- ", "Dispositivos de Seguridad (punto 4.14 y anexos C.E.P.)", lineWidth);
                if (acta.isNumeroPermiso())
                    printLine("- ", "Nº Permiso (punto 2.2.6 y anexos C.E.P.)", lineWidth);
                if (acta.isMaterialesVereda())
                    printLine("- ", "Materiales en Vereda (punto 3.5.2.4 C.E.P.)", lineWidth);
                if (acta.isCercoObra())
                    printLine("- ", "Cerco de Obra (punto 4.1 y anexos C.E.P.)", lineWidth);
                if (acta.isPlanosAprobados())
                    printLine("- ", "Planos Aprobados (punto 2.1.8.7 C.E.P.)", lineWidth);
                if (acta.isDirectorObra())
                    printLine("- ", "Director de Obra (punto 2.4.1 C.E.P.)", lineWidth);
                if (acta.isVarios())
                    printLine("- ", "Varios", lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                if (!acta.getBoletaInspeccion().isEmpty()) {
                    printLine("Por incumplimiento de Boleta de Inspección Nº: ", acta.getBoletaInspeccion(), lineWidth);
                }

                if (!acta.getObservaciones().isEmpty()) {
                    printLine("Observaciones: ", acta.getObservaciones(), lineWidth);
                }
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                outputStream.write(PrinterHelper.Commands.ESC_ALIGN_LEFT);
                printLine("El infractor deberá comparecer ante el Tribunal Municipal de Faltas cuando sea citado al efecto (Art. 57 Inc. C -Ord. X-Nº7)", "", lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                outputStream.write(PrinterHelper.Commands.ESC_ALIGN_CENTER);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);

                // Imprime la firma del infractor si existe
                if (acta.getFirmaInfractor() != null) {
                    try {
                        byte[] firmaCommand = PrinterHelper.decodeBitmap(acta.getFirmaInfractor());
                        outputStream.write(firmaCommand);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al imprimir firma: " + e.getMessage());
                    }
                }

                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                printLine("_______________________", "", lineWidth);
                printLine("Firma del Infractor", "", lineWidth);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                outputStream.write(PrinterHelper.Commands.FEED_LINE);
                printLine("_______________________", "", lineWidth);
                printLine("Firma del Inspector", "", lineWidth);

                // Finalizar impresión con corte de papel
                outputStream.write(new byte[]{0x0A, 0x0A, 0x0A, 0x0A});
                outputStream.write(PrinterHelper.Commands.FEED_PAPER_AND_CUT);

                runOnUiThread(() -> showToast("Acta impresa correctamente"));

            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                Log.e(TAG, "Error al imprimir: " + errorMsg, e);
                runOnUiThread(() -> showAlert("Error de Impresión", errorMsg));
            } finally {
                printerConnector.close();
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
        acta.setNumeroActa(etBoletaInspeccion.getText().toString());
        acta.setPropietario(etPropietario.getText().toString());
        acta.setDomicilio(etDomicilio.getText().toString());
        acta.setLugarInfraccion(etLugarInfraccion.getText().toString());

        // Fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat stf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date now = new Date();
        acta.setFecha(sdf.format(now));
        acta.setHora(stf.format(now));

        acta.setSeccion(etSeccion.getText().toString());
        acta.setChacra(etChacra.getText().toString());
        acta.setManzana(etManzana.getText().toString());
        acta.setParcela(etParcela.getText().toString());
        acta.setLote(etLote.getText().toString());
        acta.setPartida(etPartida.getText().toString());

        // Por defecto, mismos datos que el propietario
        acta.setInfractorNombre(etPropietario.getText().toString());
        acta.setInfractorDomicilio(etDomicilio.getText().toString());

        // Causas de infracción
        acta.setCartelObra(cbCartelObra.isChecked());
        acta.setDispositivosSeguridad(cbDispositivosSeguridad.isChecked());
        acta.setNumeroPermiso(cbNumeroPermiso.isChecked());
        acta.setMaterialesVereda(cbMaterialesVereda.isChecked());
        acta.setCercoObra(cbCercoObra.isChecked());
        acta.setPlanosAprobados(cbPlanosAprobados.isChecked());
        acta.setDirectorObra(cbDirectorObra.isChecked());
        acta.setVarios(cbVarios.isChecked());

        acta.setObservaciones(etObservaciones.getText().toString());
        acta.setBoletaInspeccion(etBoletaInspeccion.getText().toString());
        acta.setLogoResourceId(LOGO_RESOURCE_ID);

        // Añadir firma si existe
        if (signatureView != null && signatureView.hasSignature()) {
            acta.setFirmaInfractor(signatureView.getBitmap());
        }

        // Añadir imágenes de prueba si existen
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