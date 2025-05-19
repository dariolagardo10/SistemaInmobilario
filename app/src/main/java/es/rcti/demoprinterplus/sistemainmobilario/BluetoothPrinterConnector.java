package es.rcti.demoprinterplus.sistemainmobilario.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothPrinterConnector {
    private static final String TAG = "BTPrinterConnector";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private OnPrinterConnectionListener listener;

    public interface OnPrinterConnectionListener {
        void onConnected(OutputStream outputStream);
        void onConnectionFailed(String errorMessage);
        void onDisconnected();
    }

    public BluetoothPrinterConnector(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setOnPrinterConnectionListener(OnPrinterConnectionListener listener) {
        this.listener = listener;
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void connect() {
        if (!isBluetoothAvailable()) {
            notifyConnectionFailed("Bluetooth no disponible en este dispositivo");
            return;
        }

        if (!isBluetoothEnabled()) {
            notifyConnectionFailed("Bluetooth no está activado");
            return;
        }

        new Thread(() -> {
            try {
                // Cerrar recursos previos si existen
                close();

                // Obtener dispositivos emparejados
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() == 0) {
                    notifyConnectionFailed("No hay dispositivos Bluetooth emparejados");
                    return;
                }

                // Intentar conectar con cada dispositivo
                boolean connected = false;
                for (BluetoothDevice device : pairedDevices) {
                    try {
                        Log.d(TAG, "Intentando conectar con: " + device.getName());
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();

                        // Conectado exitosamente
                        Log.d(TAG, "Conexión establecida con: " + device.getName());
                        notifyConnected(outputStream);
                        connected = true;
                        break;
                    } catch (IOException e) {
                        Log.e(TAG, "Error al conectar con " + device.getName() + ": " + e.getMessage());
                        try {
                            if (bluetoothSocket != null) {
                                bluetoothSocket.close();
                            }
                        } catch (IOException closeEx) {
                            Log.e(TAG, "Error al cerrar socket: " + closeEx.getMessage());
                        }
                    }
                }

                if (!connected) {
                    notifyConnectionFailed("No se pudo conectar a ninguna impresora");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error general: " + e.getMessage(), e);
                notifyConnectionFailed("Error: " + e.getMessage());
            }
        }).start();
    }

    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                notifyDisconnected();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar recursos: " + e.getMessage(), e);
        }
    }

    // Método para escribir datos directamente
    public void write(byte[] data) {
        if (outputStream != null) {
            try {
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error al escribir datos: " + e.getMessage(), e);
                notifyConnectionFailed("Error al imprimir: " + e.getMessage());
            }
        } else {
            notifyConnectionFailed("No hay conexión a la impresora");
        }
    }

    private void notifyConnected(OutputStream outputStream) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) {
                listener.onConnected(outputStream);
            }
        });
    }

    private void notifyConnectionFailed(String errorMessage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) {
                listener.onConnectionFailed(errorMessage);
            }

            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void notifyDisconnected() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) {
                listener.onDisconnected();
            }
        });
    }
}