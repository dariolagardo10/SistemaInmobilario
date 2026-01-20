package es.rcti.demoprinterplus.sistemainmobilario;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class HttpForm {

    public static String post(String urlStr, Map<String, String> params) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String body = buildQuery(params);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(body);
            writer.flush();
            writer.close();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            return readAll(is);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Para mandar campos repetidos: imagenes[]=... imagenes[]=...
    public static String postWithRepeated(String urlStr,
                                          Map<String, String> baseParams,
                                          String repeatedKey,
                                          List<String> repeatedValues) throws Exception {

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String body = buildQuery(baseParams) + buildRepeated(repeatedKey, repeatedValues);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(body);
            writer.flush();
            writer.close();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            return readAll(is);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String buildQuery(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!first) sb.append("&");
                first = false;

                sb.append(URLEncoder.encode(e.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
            }
        }

        return sb.toString();
    }

    private static String buildRepeated(String key, List<String> values) throws Exception {
        if (values == null || values.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            sb.append("&");
            sb.append(URLEncoder.encode(key, "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(v == null ? "" : v, "UTF-8"));
        }
        return sb.toString();
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            out.append(line);
        }
        br.close();
        return out.toString();
    }
}
