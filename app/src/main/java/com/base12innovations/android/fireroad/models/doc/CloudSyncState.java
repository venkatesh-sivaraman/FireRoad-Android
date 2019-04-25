package com.base12innovations.android.fireroad.models.doc;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Represents both the input and the output to the FireRoad cloud sync APIs.
 */
public class CloudSyncState {
    public CloudSyncState() { }

    @SerializedName("success")
    public Boolean success;

    @SerializedName("error")
    public String logError;

    @SerializedName("error_msg")
    public String userError;

    @SerializedName("changed")
    public String changeDate;

    @SerializedName("downloaded")
    public String downloadDate;

    @SerializedName("id")
    public Integer id;

    @SerializedName("name")
    public String name;

    @SerializedName("result")
    public String result;

    @SerializedName("contents")
    public Map<String, Object> contents;

    @SerializedName("agent")
    public String agent;

    @SerializedName("override")
    public Boolean override;

    @SerializedName("files")
    public Map<String, CloudSyncState> files;

    @SerializedName("file")
    public CloudSyncState file;

    // Conflicts
    @SerializedName("other_agent")
    public String otherAgent;

    @SerializedName("other_date")
    public String otherDate;

    @SerializedName("other_contents")
    public Map<String, Object> otherContents;

    @SerializedName("other_name")
    public String otherName;

}
