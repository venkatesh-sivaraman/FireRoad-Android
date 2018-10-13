package com.base12innovations.android.fireroad.models.doc;

import android.util.Log;

import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class Document {

    public static String ROAD_DOCUMENT_TYPE = "RoadDocument";
    public static String SCHEDULE_DOCUMENT_TYPE = "ScheduleDocument";
    public static String INITIAL_DOCUMENT_TITLE = "First Steps";

    public File file;

    private boolean readOnly;
    public boolean isReadOnly() { return readOnly; }

    public Document(File location) {
        this.file = location;
    }
    public Document(File location, boolean readOnly) {
        this.file = location;
        this.readOnly = readOnly;
    }

    public String getFileName() {
        return file.getName().substring(0, file.getName().lastIndexOf('.'));
    }

    public List<Course> getAllCourses() {
        return new ArrayList<>();
    }

    public void save() {
        if (isReadOnly() || file == null) return;
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

    public String plainTextRepresentation() { return ""; }

    public void parse(String contents) {

    }

    public void read() {
        String contents = readTextFile(this.file);
        parse(contents);
    }

    public void readInBackground(final TaskDispatcher.TaskNoReturn completion) {
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                read();
                if (completion != null)
                    TaskDispatcher.onMain(completion);
            }
        });
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
