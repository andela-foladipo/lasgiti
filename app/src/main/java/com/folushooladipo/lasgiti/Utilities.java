package com.folushooladipo.lasgiti;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class Utilities {
    static int GITHUB_SEARCH_RESULTS_LIMIT = 1000;

    static String downloadUrl(String myUrl) throws IOException {
        InputStream inStream = null;
        try {
            URL url = new URL(myUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            inStream = conn.getInputStream();

            return readIt(inStream);

        }
        catch (UnsupportedEncodingException e) {
            throw new IOException();
        }
        finally {
            if (inStream != null) {
                inStream.close();
            }
        }
    }

    private static String readIt(InputStream stream) throws IOException {
        String result = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            result += line;
        }
        reader.close();
        return result;
    }
}
