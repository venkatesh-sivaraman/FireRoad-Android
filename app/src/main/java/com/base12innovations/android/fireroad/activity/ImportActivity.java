package com.base12innovations.android.fireroad.activity;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.base12innovations.android.fireroad.MainActivity;
import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.doc.Document;
import com.base12innovations.android.fireroad.models.doc.DocumentManager;
import com.base12innovations.android.fireroad.models.doc.RoadDocument;
import com.base12innovations.android.fireroad.models.doc.User;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ImportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        final Uri data = getIntent().getData();
        if (data != null) {
            getIntent().setData(null);
            TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    try {
                        RoadDocument doc = importData(data);
                        if (doc != null) {
                            User.currentUser().initialize(ImportActivity.this);
                            User.currentUser().setCurrentDocument(doc);
                        } else {
                            showError();
                            return;
                        }

                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                // launch home Activity (with FLAG_ACTIVITY_CLEAR_TOP) here…
                                Intent i = new Intent(ImportActivity.this, MainActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError();
                    }
                }
            });
        }
    }

    private void showError() {
        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                AlertDialog.Builder b = new AlertDialog.Builder(ImportActivity.this);
                b.setTitle("Invalid File");
                b.setMessage("There was a problem importing the file.");
                b.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                });
                b.show();
            }
        });
    }

    private RoadDocument importData(Uri data) throws Exception {
        final String scheme = data.getScheme();

        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            ContentResolver cr = getContentResolver();
            InputStream is = cr.openInputStream(data);
            if (is == null) return null;

            StringBuffer buf = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str;
            while ((str = reader.readLine()) != null) {
                buf.append(str + "\n");
            }
            is.close();

            DocumentManager roadDocManager = new DocumentManager(Document.ROAD_DOCUMENT_TYPE, getFilesDir(), this);
            String baseName = data.getLastPathSegment();
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
            RoadDocument newDoc = (RoadDocument)roadDocManager.getNewDocument(roadDocManager.noConflictName(baseName));
            newDoc.parse(buf.toString());
            newDoc.save();

            return newDoc;
        }
        return null;
    }
}
