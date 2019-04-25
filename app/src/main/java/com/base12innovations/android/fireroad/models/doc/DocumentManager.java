package com.base12innovations.android.fireroad.models.doc;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.base12innovations.android.fireroad.models.AppSettings;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private SharedPreferences prefs;
    private static final String PREFS_PREFIX = "com.base12innovations.android.fireroad.DocumentManager.";

    public DocumentManager(String documentType, File baseDir, Context context) {
        this.documentType = documentType;
        this.baseDir = baseDir;
        this.prefs = context.getSharedPreferences(PREFS_PREFIX + documentType, Context.MODE_PRIVATE);
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
        if (loadedDocuments == null)
            loadedDocuments = new HashMap<>();
        fileHandles = Arrays.asList(baseDir.listFiles(filter));
        Collections.sort(fileHandles, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                Date file1Date = getCloudModifiedDate(file.getName().substring(0, file.getName().lastIndexOf('.')));
                Date file2Date = getCloudModifiedDate(t1.getName().substring(0, t1.getName().lastIndexOf('.')));
                long file1Val = file1Date != null ? file1Date.getTime() : file.lastModified();
                long file2Val = file2Date != null ? file2Date.getTime() : t1.lastModified();
                return -Long.compare(file1Val, file2Val);
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

    public File getFileHandle(String name) {
        return new File(baseDir, name + getPathExtension());
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
        Date modDate = getCloudModifiedDate(loc.getName().substring(0, loc.getName().lastIndexOf('.')));
        if (modDate == null)
            modDate = new Date(loc.lastModified());
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

    public Document getNewDocument(String title) throws IOException {
        File loc = new File(baseDir, title + getPathExtension());
        if (loc.exists()) {
            throw new IOException(loc.getName());
        }
        fileHandles = null;
        return createDocumentFromCloud(null, title, null, null);
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

    public boolean deleteDocument(int index, boolean localOnly) {
        String name = getDocumentName(index);
        File loc = fileHandles.get(index);
        boolean result = loc.delete();
        if (result) {
            loadedDocuments.remove(loc);
            fileHandles = null;
        }
        if (!localOnly) {
            deleteFileFromCloud(name, null);
        }
        return result;
    }

    public boolean deleteDocument(File loc) {
        boolean result = loc.delete();
        if (result) {
            loadedDocuments.remove(loc);
            fileHandles = null;
        }
        return result;
    }

    public boolean renameDocument(int index, final String newName, boolean localOnly) throws IOException {
        final String oldName = getDocumentName(index);
        File loc = fileHandles.get(index);
        File newLoc = new File(baseDir, newName + getPathExtension());
        if (newLoc.exists()) {
            throw new IOException(newLoc.getName());
        }
        boolean result = loc.renameTo(newLoc);
        if (result) {
            loadedDocuments.remove(loc);
            fileHandles = null;
        }
        if (result && !localOnly) {
            TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() {
                    renameDocumentInCloud(oldName, newName);
                }
            });
        }
        return result;
    }

    public boolean renameDocument(String oldName, String newName, boolean localOnly) throws IOException {
        return renameDocument(fileHandles.indexOf(getFileHandle(oldName)), newName, localOnly);
    }

    @TargetApi(Build.VERSION_CODES.O)
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

    private Document createDocumentFromCloud(Integer id, String name, Map<String, Object> contents, SyncResponseHandler listener) {
        String newName = noConflictName(name);
        Document doc = initializeDocument(getFileHandle(newName), false);
        if (contents != null) {
            String contentsString = new Gson().toJson(contents);
            doc.parse(contentsString);
        } else {
            if (getDocumentType().equals(Document.ROAD_DOCUMENT_TYPE))
                ((RoadDocument)doc).addCourseOfStudy("girs");
        }
        doc.save();

        if (id != null) {
            setDocumentID(newName, id);
            String userID = AppSettings.shared().getString(AppSettings.RECOMMENDER_USER_ID, null);
            if (userID != null)
                setUserID(newName, userID);
        }
        if (id == null || newName.equals(name)) {
            setDownloadDate(newName, new Date());
            syncDocument(doc, true, false, false, listener);
        }
        return doc;
    }

    // Cloud sync attributes

    private static final String DOCUMENT_IDS_KEY = "documentIDs";
    private static final String USER_IDS_KEY = "userIDs";
    private static final String CLOUD_MODIFIED_DATES_KEY = "cloudModifiedDates";
    private static final String DOWNLOAD_DATES_KEY = "downloadedDates";

    private Map<String, Integer> documentIDs;
    private Map<String, String> userIDs;
    private Map<String, String> cloudModifiedDates;
    private Map<String, String> downloadDates;
    private Set<String> justModifiedFiles;

    private void loadCloudSyncAttributes() {
        if (documentIDs == null) {
            String raw = prefs.getString(DOCUMENT_IDS_KEY, "");
            if (raw.length() == 0) {
                documentIDs = new HashMap<>();
            } else {
                documentIDs = new HashMap<>();
                for (String comp : raw.split(";")) {
                    String[] subcomps = comp.split(",");
                    // Take the LAST comma and use its split point
                    String idNum = subcomps[subcomps.length - 1];
                    documentIDs.put(comp.substring(0, comp.length() - idNum.length() - 1), Integer.parseInt(idNum));
                }
            }
        }
        if (userIDs == null)
            userIDs = loadAttributeMap(USER_IDS_KEY);
        if (cloudModifiedDates == null)
            cloudModifiedDates = loadAttributeMap(CLOUD_MODIFIED_DATES_KEY);
        if (downloadDates == null)
            downloadDates = loadAttributeMap(DOWNLOAD_DATES_KEY);
    }

    private void saveDocumentIDs() {
        List<String> comps = new ArrayList<>();
        for (String key : documentIDs.keySet()) {
            comps.add(key + "," + Integer.toString(documentIDs.get(key)));
        }
        prefs.edit().putString(DOCUMENT_IDS_KEY, TextUtils.join(";", comps)).apply();
    }

    private Map<String, String> loadAttributeMap(String prefsKey) {
        String raw = prefs.getString(prefsKey, "");
        if (raw.length() == 0) {
            return new HashMap<>();
        } else {
            Map<String, String> result = new HashMap<>();
            for (String comp : raw.split(";")) {
                String[] subcomps = comp.split(",");
                // Take the LAST comma and use its split point
                String dateString = subcomps[subcomps.length - 1];
                result.put(comp.substring(0, comp.length() - dateString.length() - 1), dateString);
            }
            return result;
        }
    }

    private void saveAttributeMap(Map<String, String> map, String prefsKey) {
        List<String> comps = new ArrayList<>();
        for (String key : map.keySet()) {
            comps.add(key + "," + map.get(key));
        }
        prefs.edit().putString(prefsKey, TextUtils.join(";", comps)).apply();
    }

    private Integer getDocumentID(String name) {
        loadCloudSyncAttributes();
        if (!documentIDs.containsKey(name))
            return null;
        return documentIDs.get(name);
    }

    private void setDocumentID(String name, Integer value) {
        loadCloudSyncAttributes();
        if (value == null)
            documentIDs.remove(name);
        else
            documentIDs.put(name, value);
        saveDocumentIDs();
    }

    private String getDocumentNameByID(Integer id) {
        loadCloudSyncAttributes();
        for (String key: documentIDs.keySet()) {
            if (documentIDs.get(key).equals(id)) {
                return key;
            }
        }
        return null;
    }

    private String getUserID(String name) {
        loadCloudSyncAttributes();
        if (!userIDs.containsKey(name))
            return null;
        return userIDs.get(name);
    }

    private void setUserID(String name, String id) {
        loadCloudSyncAttributes();
        if (id == null)
            userIDs.remove(name);
        else
            userIDs.put(name, id);
        saveAttributeMap(userIDs, USER_IDS_KEY);
    }

    private Date dateFromString(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ", Locale.US);
        try {
            Date result = sdf.parse(dateStr);
            if (result != null)
                return result;
        } catch (ParseException e) { }
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US);
        try {
            Date result = sdf.parse(dateStr);
            if (result != null)
                return result;
        } catch (ParseException e) { }
        return null;
    }

    private String stringFromDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ", Locale.US);
        return sdf.format(date);
    }

    public Date getCloudModifiedDate(String name) {
        loadCloudSyncAttributes();
        if (!cloudModifiedDates.containsKey(name))
            return null;
        return dateFromString(cloudModifiedDates.get(name));
    }

    private void setCloudModifiedDate(String name, Date date) {
        loadCloudSyncAttributes();
        if (date == null)
            cloudModifiedDates.remove(name);
        else
            cloudModifiedDates.put(name, stringFromDate(date));
        saveAttributeMap(cloudModifiedDates, CLOUD_MODIFIED_DATES_KEY);
    }

    public Date getDownloadDate(String name) {
        loadCloudSyncAttributes();
        if (!downloadDates.containsKey(name))
            return null;
        return dateFromString(downloadDates.get(name));
    }

    private void setDownloadDate(String name, Date date) {
        loadCloudSyncAttributes();
        if (date == null) {
            downloadDates.remove(name);
        }
        else {
            downloadDates.put(name, stringFromDate(date));
        }
        saveAttributeMap(downloadDates, DOWNLOAD_DATES_KEY);
    }

    public void setJustModifiedFile(String name) {
        if (justModifiedFiles == null)
            justModifiedFiles = new HashSet<>();
        justModifiedFiles.add(name);
    }

    public boolean justModifiedFile(String name) {
        return justModifiedFiles != null && justModifiedFiles.contains(name);
    }

    // Cloud sync

    public interface SyncNetworkHandler {
        CloudSyncState cloudGetFiles(DocumentManager docManager);
        CloudSyncState cloudSyncFile(DocumentManager docManager, CloudSyncState input);
        CloudSyncState cloudDeleteFile(DocumentManager docManager, CloudSyncState input);
        CloudSyncState cloudDownloadFile(DocumentManager docManager, CloudSyncState input);
    }

    public WeakReference<SyncNetworkHandler> networkHelper = new WeakReference<>(null);

    public interface SyncFileListener {
        void documentManagerRenamedFile(DocumentManager manager, String oldName, String newName);
        void documentManagerModifiedFile(DocumentManager manager, String name);
        void documentManagerDeletedFile(DocumentManager manager, String name);
        void documentManagerSyncConflict(DocumentManager manager, SyncConflict conflict, SyncConflictResponse response);
    }

    public WeakReference<SyncFileListener> fileListener = new WeakReference<>(null);

    private boolean syncInProgress = false;

    public boolean isSyncInProgress() {
        return syncInProgress;
    }

    public class SyncConflict {
        public String title;
        public String message;
        public String positiveButton;
        public String neutralButton;
        public String negativeButton;
    }

    public interface SyncConflictResponse {
        void response(String action);
    }

    public interface SyncResponseHandler {
        void documentManagerSyncedSuccessfully(DocumentManager manager);
        void documentManagerSyncError(DocumentManager manager, String message);
    }

    // Sync results
    static final String UPDATE_REMOTE = "update_remote";
    static final String UPDATE_LOCAL = "update_local";
    static final String CONFLICT = "conflict";
    static final String ERROR = "error";
    static final String NO_CHANGE = "no_change";

    // Conflict responses
    static final String KEEP_LOCAL = "Keep Local";
    static final String KEEP_REMOTE = "Keep Remote";
    static final String KEEP_BOTH = "Keep Both";
    static final String DELETE = "Delete";

    public void syncAllFiles(final SyncResponseHandler listener) {
        if (AppSettings.shared().getInt(AppSettings.ALLOWS_RECOMMENDATIONS, AppSettings.RECOMMENDATIONS_NO_VALUE) != AppSettings.RECOMMENDATIONS_ALLOWED ||
                networkHelper.get() == null || isSyncInProgress())
            return;

        syncInProgress = true;
        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                CloudSyncState state = networkHelper.get().cloudGetFiles(DocumentManager.this);
                if (state == null || state.files == null) {
                    handleSyncError(state, listener);
                    return;
                }

                String deletedInitial = null;

                // Upload existing files that don't have IDs
                fileHandles = null;
                loadFileHandlesIfNeeded();
                for (File file: fileHandles) {
                    String myFileName = file.getName().substring(0, file.getName().lastIndexOf('.'));

                    if (getDocumentID(myFileName) == null) {
                        // Upload the file
                        //Log.d("DocumentManager", "Uploading my file " + myFileName);
                        Document doc = initializeDocument(getFileHandle(myFileName), false);
                        doc.read();

                        if (myFileName.equals(Document.INITIAL_DOCUMENT_TITLE) && doc.getAllCourses().size() == 0) {
                            // Empty initial document - delete it
                            if (deleteDocument(doc.file))
                                deletedInitial = myFileName;
                        }

                        boolean worked = syncDocument(doc, true, false, false, null);
                        //Log.d("DocumentManager", "Uploaded " + myFileName + " successfully: " + Boolean.toString(worked));

                    } else if (state.files.containsKey(getDocumentID(myFileName))) {
                        // Sync the file
                        //Log.d("DocumentManager", "Syncing my file " + myFileName);
                        Document doc = initializeDocument(getFileHandle(myFileName), false);
                        doc.read();
                        syncDocument(doc, justModifiedFile(myFileName), false, false, listener);
                    }
                }

                for (String fileID : state.files.keySet()) {
                    int id = Integer.parseInt(fileID);
                    if (getDocumentNameByID(id) != null &&
                            getFileHandle(getDocumentNameByID(id)).exists()) {
                        // Sync the file
                        String name = getDocumentNameByID(id);
                        //Log.d("DocumentManager", "Syncing " + name + " part 2");
                        Document doc = initializeDocument(getFileHandle(name), false);
                        doc.read();
                        syncDocument(doc, justModifiedFile(name), false, false, listener);
                    } else {
                        // Download the new file
                        //Log.d("DocumentManager", "Downloading new file " + fileID);
                        CloudSyncState downloadInput = new CloudSyncState();
                        downloadInput.id = id;
                        CloudSyncState downloadResult = networkHelper.get().cloudDownloadFile(DocumentManager.this, downloadInput);
                        if (downloadResult == null || downloadResult.success == null ||
                                !downloadResult.success || downloadResult.file == null ||
                                downloadResult.file.name == null || downloadResult.file.contents == null) {
                            handleSyncError(downloadResult, listener);
                            return;
                        }

                        downloadResult = downloadResult.file;
                        Document newDoc = createDocumentFromCloud(id, downloadResult.name, downloadResult.contents, listener);
                        String newName = newDoc.getFileName();
                        if (newName != null) {
                            if (downloadResult.changeDate != null) {
                                Date changeDate = dateFromString(downloadResult.changeDate);
                                if (changeDate != null)
                                    setCloudModifiedDate(newName, changeDate);
                            }
                            if (downloadResult.downloadDate != null) {
                                Date downloadDate = dateFromString(downloadResult.downloadDate);
                                setDownloadDate(newName, downloadDate != null ? downloadDate : new Date());
                            }
                        }
                    }
                }

                if (deletedInitial != null && fileListener.get() != null) {
                    //Log.d("DocumentManager", "Notifying deleted file " + deletedInitial);
                    final String deletedName = deletedInitial;
                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            fileListener.get().documentManagerDeletedFile(DocumentManager.this, deletedName);
                        }
                    });
                }

                justModifiedFiles = null;
                syncInProgress = false;
                TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() { listener.documentManagerSyncedSuccessfully(DocumentManager.this); }
                });
            }
        });
    }

    public boolean syncDocument(final Document doc, boolean justModified, boolean override, boolean pause, final SyncResponseHandler listener) {
        if (networkHelper.get() == null || (pause && isSyncInProgress()))
            return false;

        syncInProgress = true;
        CloudSyncState input = new CloudSyncState();
        final String name = doc.file.getName().substring(0, doc.file.getName().lastIndexOf('.'));
        input.name = name;
        input.contents = new Gson().fromJson(doc.contentsString(), new TypeToken<HashMap<String, Object>>() {}.getType());

        input.changeDate = (justModified || getDownloadDate(name) == null) ? stringFromDate(new Date()) : stringFromDate(getDownloadDate(name));
        Integer id = getDocumentID(name);
        String currentUser = AppSettings.shared().getString(AppSettings.RECOMMENDER_USER_ID, null);
        if (id != null) {
            String userID = getUserID(name);
            if (userID != null && currentUser != null && !userID.equals(currentUser)) {
                // Pretend it's a new file
            } else {
                input.id = id;
            }
        }

        Date downloadDate = getDownloadDate(name);
        if (downloadDate != null)
            input.downloadDate = stringFromDate(downloadDate);
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        if (myDevice != null)
            input.agent = myDevice.getName();
        else
            input.agent = "FireRoad on " + Build.MODEL;
        if (override)
            input.override = true;

        final CloudSyncState result = networkHelper.get().cloudSyncFile(this, input);
        if (result == null || result.success == null || !result.success || result.result == null) {
            Log.e("DocumentManager", "Handling sync error with " + (input.downloadDate != null ? input.downloadDate : "null"));
            handleSyncError(result, listener);
            return false;
        }

        switch (result.result) {
            case UPDATE_REMOTE:
                setDownloadDate(name, new Date());
                setCloudModifiedDate(name, new Date());
                if (result.id != null)
                    setDocumentID(name, result.id);
                if (currentUser != null)
                    setUserID(name, currentUser);
                finishCloudSync(doc, name, result, listener);
                return true;
            case UPDATE_LOCAL:
                setDownloadDate(name, new Date());
                if (result.changeDate != null) {
                    Date changeDate = dateFromString(result.changeDate);
                    if (changeDate != null)
                        setCloudModifiedDate(name, changeDate);
                }
                if (result.id != null)
                    setDocumentID(name, result.id);
                if (currentUser != null)
                    setUserID(name, currentUser);
                finishCloudSync(doc, name, result, listener);
                return true;
            case CONFLICT:
                if (result.otherDate != null && result.otherAgent != null) {
                    Date modifiedDate = null;
                    if (result.otherDate.length() > 0)
                        modifiedDate = dateFromString(result.otherDate);

                    if (modifiedDate != null) {
                        syncInProgress = false;
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                SyncConflict conflict = new SyncConflict();
                                conflict.title = "Sync Conflict";
                                conflict.message = getDocumentTypeName(true) + " \"" + name + "\" was modified by "
                                        + result.otherAgent + ".";
                                conflict.positiveButton = KEEP_LOCAL;
                                conflict.negativeButton = KEEP_REMOTE;
                                conflict.neutralButton = KEEP_BOTH;
                                if (fileListener.get() != null)
                                    fileListener.get().documentManagerSyncConflict(DocumentManager.this, conflict, new SyncConflictResponse() {
                                        @Override
                                        public void response(final String action) {
                                            TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                                                @Override
                                                public void perform() {
                                                    handleSyncConflict(doc, name, result, action, listener);
                                                }
                                            });
                                        }
                                    });
                            }
                        });
                    } else {
                        // File was deleted on server - delete it locally
                        //Log.d("DocumentManager", "Deleting local version of " + name);
                        setDownloadDate(name, null);
                        setCloudModifiedDate(name, null);
                        finishCloudSync(doc, name, result, listener);
                    }
                }
                return true;
            case NO_CHANGE:
                if (result.changeDate != null) {
                    Date change = dateFromString(result.changeDate);
                    if (change != null)
                        setCloudModifiedDate(name, change);
                }
                finishCloudSync(doc, name, result, listener);
                return true;
            default:
                Log.d("DocumentManager", "Unknown sync result " + result.result);
                finishCloudSync(doc, name, result, listener);
                return false;
        }
    }

    public void deleteFileFromCloud(final String name, final SyncResponseHandler listener) {
        setCloudModifiedDate(name, null);
        setDownloadDate(name, null);
        if (getDocumentID(name) == null || networkHelper.get() == null) {
            if (listener != null)
                listener.documentManagerSyncError(this, null);
            //Log.d("DocumentManager", this.toString() + "No document ID or network helper");
            return;
        }
        final int id = getDocumentID(name);
        setDocumentID(name, null);
        setUserID(name, null);

        TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
            @Override
            public void perform() {
                CloudSyncState input = new CloudSyncState();
                input.id = id;
                //Log.d("DocumentManager", "Deleting " + Integer.toString(id) + " from cloud");
                CloudSyncState result = networkHelper.get().cloudDeleteFile(DocumentManager.this, input);
                if (result == null || result.success == null || !result.success) {
                    handleSyncError(result, listener);
                } else {
                    //Log.d("DocumentManager", "Successfully deleted " + name + " from the cloud");
                    if (listener != null)
                        listener.documentManagerSyncedSuccessfully(DocumentManager.this);
                }
            }
        });
    }

    private void handleSyncConflict(Document doc, String name, CloudSyncState state, String action, SyncResponseHandler listener) {
        switch (action) {
            case KEEP_LOCAL:
                boolean success = syncDocument(doc, true, true, false, null);
                if (success) {
                    setDownloadDate(name, new Date());
                    setCloudModifiedDate(name, new Date());
                }
                break;
            case KEEP_REMOTE:
                setDownloadDate(name, new Date());
                if (state.otherDate != null) {
                    Date otherDate = dateFromString(state.otherDate);
                    if (otherDate != null)
                        setCloudModifiedDate(name, otherDate);
                }
                state.name = state.otherName;
                state.contents = state.otherContents;
                state.agent = state.otherAgent;
                state.result = UPDATE_LOCAL;
                finishCloudSync(doc, name, state, listener);
                break;
            case KEEP_BOTH:
                // Keep the remote copy in a duplicate file
                success = syncDocument(doc, true, true, false, null);
                if (success) {
                    setDownloadDate(name, new Date());
                    setCloudModifiedDate(name, new Date());
                }

                if (state.otherContents != null)
                    createDocumentFromCloud(null, state.otherName != null ? state.otherName : noConflictName(name), state.otherContents, null);
                break;
            case DELETE:
                setDownloadDate(name, null);
                setCloudModifiedDate(name, null);
                deleteDocument(getFileHandle(name));
                deleteFileFromCloud(name, null);
                finishCloudSync(doc, name, state, listener);
                break;
            default:
                Log.d("DocumentManager","Unknown action " + action);
                break;
        }
    }


    private void finishCloudSync(Document doc, final String name, final CloudSyncState state, final SyncResponseHandler listener) {
        if (state == null || state.result == null) {
            if (state == null)
                Log.d("DocumentManager", "Sync error because finishing cloud sync state is null");
            else if (state.result == null)
                Log.d("DocumentManager", "Sync error because finishing cloud sync state result is null");
            handleSyncError(state, listener);
            return;
        }

        syncInProgress = false;
        if (state.result.equals(UPDATE_LOCAL) && state.contents != null) {
            //Log.d("DocumentManager", "Updating contents");
            String contentsString = new Gson().toJson(state.contents);
            doc.parse(contentsString);
            if (state.name != null && !state.name.equals(name)) {
                try {
                    boolean worked = renameDocument(name, state.name, true);
                    if (worked) {
                        doc.file = getFileHandle(state.name);
                        doc.save();
                        if (fileListener.get() != null) {
                            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                                @Override
                                public void perform() {
                                    fileListener.get().documentManagerRenamedFile(DocumentManager.this, name, state.name);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    return;
                }
            } else {
                doc.save();
                if (fileListener.get() != null) {
                    TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                        @Override
                        public void perform() {
                            fileListener.get().documentManagerModifiedFile(DocumentManager.this, name);
                        }
                    });
                }
            }
        }
    }

    public void renameDocumentInCloud(String oldName, String newName) {

        if (getDocumentID(oldName) != null) {
            Integer id = getDocumentID(oldName);
            setDocumentID(oldName, null);
            setDocumentID(newName, id);
        }
        if (getUserID(oldName) != null) {
            String id = getUserID(oldName);
            setUserID(oldName, null);
            setUserID(newName, id);
        }
        if (getCloudModifiedDate(oldName) != null) {
            Date date = getCloudModifiedDate(oldName);
            setCloudModifiedDate(oldName, null);
            setCloudModifiedDate(newName, date);
        }
        if (getDownloadDate(oldName) != null) {
            Date date = getDownloadDate(oldName);
            setDownloadDate(oldName, null);
            setDownloadDate(newName, date);
        }

        Document doc = initializeDocument(getFileHandle(newName), false);
        doc.read();
        //Log.d("DocumentManager","Syncing renamed document with name " + doc.getFileName() + doc.getAllCourses().toString());
        boolean worked = syncDocument(doc, true, false, false, null);
        //Log.d("DocumentManager", "Upload worked: " + Boolean.toString(worked));
    }

    private void handleSyncError(CloudSyncState state, final SyncResponseHandler listener) {
        final String error = state != null ? state.userError : null;
        if (state != null && state.logError != null)
            Log.d("DocumentManager", "Sync error: " + state.logError);
        if (listener != null)
            TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                @Override
                public void perform() { listener.documentManagerSyncError(DocumentManager.this, error); }
            });
        syncInProgress = false;
    }
}
