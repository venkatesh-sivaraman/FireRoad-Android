package com.base12innovations.android.fireroad.models;

import android.util.Log;

import com.base12innovations.android.fireroad.ScheduleFragment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DocumentManager {

    private String documentType;
    public String getDocumentType() {
        return documentType;
    }
    public String getDocumentTypeName(boolean capitalized) {
        String baseTitle;
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE)) {
            baseTitle = "road";
        } else {
            baseTitle = "schedule";
        }
        if (capitalized)
            return baseTitle.substring(0, 1).toUpperCase() + baseTitle.substring(1);
        return baseTitle;
    }
    public String getPluralDocumentTypeName(boolean capitalized) {
        String baseTitle;
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE)) {
            baseTitle = "roads";
        } else {
            baseTitle = "schedules";
        }
        if (capitalized)
            return baseTitle.substring(0, 1).toUpperCase() + baseTitle.substring(1);
        return baseTitle;
    }
    public String getPathExtension() {
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE)) {
            return ".road";
        } else {
            return ".sched";
        }
    }
    private Document initializeDocument(File loc, boolean temporary) {
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE)) {
            return new RoadDocument(loc, temporary);
        } else {
            return new ScheduleDocument(loc, temporary);
        }
    }
    public void setAsCurrent(Document doc) {
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE) && doc instanceof RoadDocument) {
            User.currentUser().setCurrentDocument((RoadDocument)doc);
        } else {
            User.currentUser().setCurrentSchedule((ScheduleDocument)doc);
        }
    }
    public Document getCurrent() {
        if (documentType.equals(Document.ROAD_DOCUMENT_TYPE)) {
            return User.currentUser().getCurrentDocument();
        } else {
            return User.currentUser().getCurrentSchedule();
        }
    }

    private File baseDir;

    public DocumentManager(String documentType, File baseDir) {
        this.documentType = documentType;
        this.baseDir = baseDir;
    }

    // Loading files

    private List<File> fileHandles;
    private void loadFileHandles() {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.toLowerCase().contains(getPathExtension());
            }
        };
        fileHandles = Arrays.asList(baseDir.listFiles(filter));
        fileHandles.sort(new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return -Long.compare(file.lastModified(), t1.lastModified());
            }
        });
    }

    private void loadFileHandlesIfNeeded() {
        if (fileHandles == null)
            loadFileHandles();
    }

    public int getItemCount() {
        loadFileHandlesIfNeeded();
        return fileHandles.size();
    }

    public File getFileHandle(int index) {
        loadFileHandlesIfNeeded();
        return fileHandles.get(index);
    }

    private Map<File, Document> loadedDocuments;

    public Document getTempDocument(int index) {
        File loc = fileHandles.get(index);
        if (loadedDocuments == null)
            loadedDocuments = new HashMap<>();
        if (loadedDocuments.containsKey(loc))
            return loadedDocuments.get(loc);
        Document newDoc = initializeDocument(loc, true);
        loadedDocuments.put(loc, newDoc);
        return newDoc;
    }

    public String getDocumentName(int index) {
        File loc = fileHandles.get(index);
        String base = loc.getName();
        return base.substring(0, base.lastIndexOf('.'));
    }

    public String getDocumentModifiedString(int index) {
        File loc = fileHandles.get(index);
        Date modDate = new Date(loc.lastModified());
        SimpleDateFormat sdf;
        if ((new Date()).getTime() - modDate.getTime() > 1000 * 60 * 60 * 24) {
            sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        } else {
            sdf = new SimpleDateFormat("h:mm a", Locale.US);
        }
        return sdf.format(modDate);
    }

    public Document getNonTemporaryDocument(int index) {
        File loc = fileHandles.get(index);
        Document doc = initializeDocument(loc, false);
        doc.read();
        return doc;
    }

    public Document getNewDocument(String title) throws FileAlreadyExistsException {
        File loc = new File(baseDir, title + getPathExtension());
        if (loc.exists()) {
            throw new FileAlreadyExistsException(loc.getName());
        }
        Document doc = initializeDocument(loc, false);
        doc.save();
        fileHandles = null;
        return doc;
    }

    public String noConflictName(String title) {
        File loc = new File(baseDir, title + getPathExtension());
        if (!loc.exists())
            return title;

        int counter = 1;
        while (loc.exists() && counter < 50) {
            counter += 1;
            loc = new File(baseDir, title + " " + Integer.toString(counter) + getPathExtension());
        }
        return title + " " + Integer.toString(counter);
    }

    public boolean deleteDocument(int index) {
        File loc = fileHandles.get(index);
        boolean result = loc.delete();
        if (result) {
            loadedDocuments.remove(loc);
            fileHandles = null;
        }
        return result;
    }

    public boolean renameDocument(int index, String newName) throws FileAlreadyExistsException {
        File loc = fileHandles.get(index);
        File newLoc = new File(baseDir, newName + getPathExtension());
        if (newLoc.exists()) {
            throw new FileAlreadyExistsException(newLoc.getName());
        }
        boolean result = loc.renameTo(newLoc);
        if (result) {
            loadedDocuments.remove(loc);
            fileHandles = null;
        }
        return result;
    }

    public boolean duplicateDocument(int index) {
        File loc = fileHandles.get(index);
        String fileName = getDocumentName(index);
        File newLoc = new File(baseDir, noConflictName(fileName) + getPathExtension());
        try {
            Files.copy(loc.toPath(), newLoc.toPath(), StandardCopyOption.REPLACE_EXISTING);
            fileHandles = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
