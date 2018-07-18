package com.base12innovations.android.fireroad;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Document {

    public File file;

    public Document(File location) {
        this.file = location;
    }

    public void save() {
        try {
            this.file.createNewFile();
            String contents = contentsString();
            writeToFile(contents, this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String contentsString() {
        return "";
    }

    public void parse(String contents) {

    }

    public void read() {
        String contents = readTextFile(this.file);
        parse(contents);
    }

    private String readTextFile(File mFile){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }

    private void writeToFile(String data, File mFile) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(mFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
