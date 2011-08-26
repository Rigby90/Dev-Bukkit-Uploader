package com.onarandombox.dev;

import joptsimple.OptionSet;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class DevBukkitUploader {

    public static void main(OptionSet options) {
        new DevBukkitUploader(options);
    }

    private OptionSet options;

    private DevBukkitUploader(OptionSet options) {
        this.options = options;

        try {
            if (!this.checkOptions()) { // TODO: Sort out all the option checks.
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {
            this.upload();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private boolean checkOptions() throws IOException {
        boolean result = true;
        // Grab the API Key directly from the command line.
        if (options.hasArgument("apikey")) {
            API_KEY = (String) options.valueOf("apikey");
            // Grab the API Key from a file passed through the command line.
        } else if (options.hasArgument("apikeyfile")) {
            BufferedReader input = new BufferedReader(new FileReader((File) options.valueOf("apikeyfile")));
            String line;
            if ((line = input.readLine()) != null) {
                API_KEY = line;
            }
            input.close();
            //
        } else {
            File fallback = new File("api.txt");
            if (fallback.exists()) {
                BufferedReader input = new BufferedReader(new FileReader(fallback));
                String line;
                if ((line = input.readLine()) != null) {
                    API_KEY = line;
                }
                input.close();
            } else {
                System.out.println("Missing API Key!");
                result = false;
            }
        }

        System.out.println("---------------------------");
        System.out.println("API Key - " + API_KEY); // TODO: Remove
        System.out.println("---------------------------");

        // Start of Parameters
        parameters.put("name", (String) options.valueOf("version"));
        parameters.put("file_type", (String) options.valueOf("type"));
        parameters.put("change_log", (String) options.valueOf("changelog"));
        parameters.put("change_markup_type", (String) options.valueOf("markup"));
        parameters.put("known_caveats", (String) options.valueOf("caveats"));
        parameters.put("caveats_markup_type", (String) options.valueOf("caveatsmarkup"));
        parameters.put("game_version", "1"); // TODO: This needs sorting once the Versions are out.
        // End of Parameters

        System.out.println("---------------------------");
        for (Entry<String, String> entry : parameters.entrySet()) {
            System.out.println(entry.getKey() + " - '" + entry.getValue() + "'");
        }
        System.out.println("---------------------------");

        UPLOAD_URL = new URL("http://dev.bukkit.org/server-mods/" + options.valueOf("project") + "/upload-file.json"); // TODO: Keep an eye on incase it changes.
        FILE_UPLOAD = (File) options.valueOf("file");
        // Return the result of all the checks.
        return result;
    }

    private static String CRLF = "\r\n";
    private static String MIME_BOUNDARY = Long.toHexString(System.currentTimeMillis());
    private static String CHARSET = "UTF-8";

    private static String API_KEY = "";
    private static Map<String, String> parameters = new HashMap<String, String>();
    private static URL UPLOAD_URL = null;
    private static File FILE_UPLOAD = null;

    private void upload() throws IOException, JSONException {
        // Grab the Upload URL.
        System.out.println("URL: " + UPLOAD_URL.toString());
        // Grab the Local File to Upload.
        File fileToUpload = (File) options.valueOf("file");

        // Create a Connection to the URL.
        URLConnection connection = UPLOAD_URL.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);

        connection.setRequestProperty("User-Agent", "Dev Bukkit Uploader");
        // Pass along the Users API-Key, without this they can't upload.
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.setRequestProperty("Content-Type", "multipart/form-data; MIME_BOUNDARY=" + MIME_BOUNDARY);

        PrintWriter writer = null;
        OutputStream output = null;
        InputStream input = null;
        try {
            // Get the OutputStream
            output = connection.getOutputStream();
            // Create the Writer.
            writer = new PrintWriter(new OutputStreamWriter(output, CHARSET), true); // true = autoFlush, important!

            // Send the Parameters to the Stream.
            this.sendParameters(writer, parameters);

            // Send the File data to the Stream.
            this.sendFileData(writer, output, fileToUpload);

            // End of multipart/form-data.
            writer.append("--" + MIME_BOUNDARY + "--").append(CRLF);

            output.flush();
            output.close();

            // Grab the URL Connection so we can get Response Codes and Messages.
            HttpURLConnection httpURL = (HttpURLConnection) connection;

            // Grab the Response Code.
            int code = httpURL.getResponseCode();
            // Check the Response Code for the appropriate response message.
            if (code == 201) {
                System.out.println("File uploaded SUCCESSFULLY!");
            } else if (code == 403) {
                System.out.println("You didn't specify your API Key correctly or you do not have permission to upload a file to that project.");
            } else if (code == 404) {
                System.out.println("Project couldn't be found. You either specified it wrong or it was renamed.");
            } else if (code == 422) {
                System.out.println("You have an error in your form. This is a JSON response that will tell you which fields had an issue.");
                System.out.println("Status - " + code + " - " + httpURL.getResponseMessage());

                input = httpURL.getErrorStream();
                System.out.println();
                int str;
                StringBuilder jsonResponse = new StringBuilder();
                while (-1 != ((str = input.read()))) {
                    //System.out.print((char) str);
                    jsonResponse.append((char) str);
                }

                if (jsonResponse.toString().length() > 0) {
                    JSONObject json = new JSONObject(jsonResponse.toString());

                    Iterator iterator = json.keys();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        System.out.println(key + " - " + json.getJSONArray(key).get(0));
                    }

                    System.out.print(json.toString());
                }
                // Need to sort out JSON reading to alert of what is wrong.
            } else {
                System.out.println("Status - " + code + " - " + httpURL.getResponseMessage());
            }
        } finally {
            if (writer != null) writer.close();
            if (output != null) output.close();
            if (input != null) input.close();
        }
    }

    private void sendParameters(PrintWriter writer, Map<String, String> parameters) {
        // Send the standard Parameters.
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            writer.append("--" + MIME_BOUNDARY).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"").append(CRLF);
            writer.append("Content-Type: text/plain; CHARSET=" + CHARSET).append(CRLF);
            writer.append(CRLF);
            writer.append(entry.getValue()).append(CRLF).flush();
        }
    }

    private void sendFileData(PrintWriter writer, OutputStream output, File fileToUpload) throws IOException {
        writer.append("--" + MIME_BOUNDARY).append(CRLF);
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileToUpload.getName() + "\"").append(CRLF);
        writer.append("Content-Type: application/zip" + CRLF);
        writer.append(CRLF).flush();

        InputStream input = null;
        try {
            // File Size.
            long size = fileToUpload.length();
            // Amount of Bytes processed.
            long current = 0L;

            // File Input Stream.
            input = new FileInputStream(fileToUpload);
            byte[] buffer = new byte[1024];
            for (int length = 0; (length = input.read(buffer)) > 0; ) {
                output.write(buffer, 0, length);
                // Overly complicated Progress Bar which probs could be done alot easier.
                current += length;
                String progress = "";
                double percent = (current / size * 100);
                int barCount = 50;
                for (int i = 0; i < barCount; i++) {
                    if ((i / barCount * 100) < percent) {
                        progress += "|";
                    } else {
                        progress += " ";
                    }
                }
                if (percent == 100) {
                    System.out.println("Upload Progress - [" + progress + "] " + percent + "%");
                } else {
                    System.out.print("Upload Progress - [" + progress + "] " + percent + "%\r");
                }
                // Progress Bar Related
            }
            output.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
        writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary MIME_BOUNDARY.
    }
}
