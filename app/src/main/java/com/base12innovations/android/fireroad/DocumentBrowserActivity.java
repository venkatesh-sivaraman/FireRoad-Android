package com.base12innovations.android.fireroad;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.base12innovations.android.fireroad.adapter.DocumentBrowserAdapter;
import com.base12innovations.android.fireroad.models.Document;
import com.base12innovations.android.fireroad.models.DocumentManager;
import com.base12innovations.android.fireroad.models.NetworkManager;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.FileAlreadyExistsException;

public class DocumentBrowserActivity extends AppCompatActivity implements DocumentBrowserAdapter.Delegate {

    public static String DOCUMENT_TYPE_EXTRA = "com.base12innovations.android.fireroad.DocumentBrowserActivity.documentType";

    private String documentType;
    private DocumentManager documentManager;
    private DocumentBrowserAdapter listAdapter;
    private RecyclerView listView;

    private boolean hasChangedDocument = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        Intent intent = getIntent();
        if (intent != null) {
            documentType = intent.getStringExtra(DOCUMENT_TYPE_EXTRA);
        } else {
            documentType = Document.ROAD_DOCUMENT_TYPE;
        }
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE))
            documentManager = NetworkManager.sharedInstance().getRoadManager();
        else
            documentManager = NetworkManager.sharedInstance().getScheduleManager();
        toolbar.setTitle(documentManager.getPluralDocumentTypeName(true));

        setupListView();

        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.newDocButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewDocument();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        showHelpText();
    }

    private void setupListView() {
        // Set up recycler view
        listAdapter = new DocumentBrowserAdapter(this, documentManager);
        listAdapter.delegate = new WeakReference<DocumentBrowserAdapter.Delegate>(this);
        listView = findViewById(R.id.listView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listAdapter);

        Log.d("DocumentBrowser", "Current document at launch: " + documentManager.getCurrent().file.getName());
        /*// Deletions
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped file and notify the RecyclerView
                int pos = viewHolder.getAdapterPosition();
                deleteDocument(pos);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listView);*/
    }

    private void exitActivity() {
        if (hasChangedDocument) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public void documentBrowserSelectedDocument(Document doc) {
        documentManager.setAsCurrent(doc);
        hasChangedDocument = true;
        exitActivity();
    }

    @Override
    public void documentBrowserLongPressedRow(final int index, View sourceView) {
        // Show context menu
        final PopupMenu menu = new PopupMenu(this, sourceView);
        MenuInflater mInflater = menu.getMenuInflater();
        mInflater.inflate(R.menu.menu_document_browser_cell, menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.deleteDocument) {
                    deleteDocument(index);
                    return true;
                } else if (menuItem.getItemId() == R.id.renameDocument) {
                    renameDocument(index);
                    return true;
                } else if (menuItem.getItemId() == R.id.duplicateDocument) {
                    boolean success = false;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        success = documentManager.duplicateDocument(index);
                        if (success)
                            listAdapter.notifyDataSetChanged();
                    }
                    else
                        Toast.makeText(DocumentBrowserActivity.this, "Failed to duplicate file - try again.", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
        menu.show();
    }

    private void addNewDocument() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create new " + documentManager.getDocumentTypeName(false));

        final EditText textBox = new EditText(this);
        textBox.setInputType(InputType.TYPE_CLASS_TEXT);
        textBox.setHint("Choose a title...");
        textBox.setSingleLine();
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_text_box_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_text_box_margin);
        textBox.setLayoutParams(params);
        container.addView(textBox);
        builder.setView(container);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String text = textBox.getText().toString();
                dialogInterface.dismiss();

                if (text.length() == 0) {
                    addNewDocument();
                    return;
                }
                try {
                    Document doc = documentManager.getNewDocument(text);
                    documentBrowserSelectedDocument(doc);
                } catch (IOException e) {
                    Toast.makeText(DocumentBrowserActivity.this, "File exists - try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
        textBox.requestFocus();
    }

    private void deleteDocument(int pos) {
        String fileName = documentManager.getDocumentName(pos);
        Log.d("DocumentBrowser", "Deleting " + documentManager.getFileHandle(pos).getName());
        final File currentFile = documentManager.getCurrent().file;
        final File deletingFile = documentManager.getFileHandle(pos);
        boolean success = documentManager.deleteDocument(pos, false);
        if (success) {
            listAdapter.notifyItemRemoved(pos);
            // Set an appropriate current doc
            if (currentFile.equals(deletingFile)) {
                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        boolean justChanged = false;
                        if (documentManager.getItemCount() > 0) {
                            for (int i = 0; i < documentManager.getItemCount(); i++) {
                                File f = documentManager.getFileHandle(i);
                                if (f.equals(currentFile) || f.equals(deletingFile)) continue;
                                final Document doc = documentManager.getNonTemporaryDocument(i);
                                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                    @Override
                                    public void perform() {
                                        documentManager.setAsCurrent(doc);
                                    }
                                });
                                hasChangedDocument = true;
                                justChanged = true;
                                break;
                            }
                        }

                        if (!justChanged) {
                            // Create new document
                            try {
                                final Document doc = documentManager.getNewDocument(documentManager.noConflictName(Document.INITIAL_DOCUMENT_TITLE));
                                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                    @Override
                                    public void perform() {
                                        documentBrowserSelectedDocument(doc);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } else {
            Snackbar.make(listView, "Failed to delete \"" + fileName + "\"", Snackbar.LENGTH_LONG).show();
        }
    }

    private void renameDocument(final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String fileName = documentManager.getDocumentName(pos);
        builder.setTitle("Rename \"" + fileName + "\"");

        final EditText textBox = new EditText(this);
        textBox.setInputType(InputType.TYPE_CLASS_TEXT);
        textBox.setHint("Choose a new title...");
        textBox.setSingleLine();
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_text_box_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_text_box_margin);
        textBox.setLayoutParams(params);
        container.addView(textBox);
        builder.setView(container);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String text = textBox.getText().toString();
                dialogInterface.dismiss();

                if (text.length() == 0) {
                    return;
                }
                try {
                    boolean success = documentManager.renameDocument(pos, text, false);
                    if (success) {
                        Snackbar.make(listView, "Successfully renamed file", Snackbar.LENGTH_LONG).show();
                        listAdapter.notifyDataSetChanged();
                    }
                } catch (IOException e) {
                    Toast.makeText(DocumentBrowserActivity.this, "File exists - try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
        textBox.requestFocus();
    }

    // Help text

    private static String PREFERENCES = "com.base12innovations.android.fireroad.DocumentBrowserActivity.Preferences";
    private static String HAS_SHOWN_HELP = "hasShownHelp";

    private boolean hasShownHelpText() {
        SharedPreferences prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getBoolean(HAS_SHOWN_HELP, false);
    }

    void showHelpText() {
        if (hasShownHelpText()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Document Browser");
        builder.setMessage("* Tap the pencil icon to create a new " + documentManager.getDocumentTypeName(false) + "\n* Tap and hold on documents to rename or delete");

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                SharedPreferences prefs = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(HAS_SHOWN_HELP, true);
                editor.apply();
            }
        });
        builder.show();
    }
}
