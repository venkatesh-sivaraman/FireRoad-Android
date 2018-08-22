package com.base12innovations.android.fireroad.models;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Represents both the input and the output to the FireRoad cloud sync APIs.
 */
public class CloudSyncState {
    public CloudSyncState() { }

    public Boolean success;

    @SerializedName("error")
    public String logError;

    @SerializedName("error_msg")
    public String userError;

    @SerializedName("changed")
    public String changeDate;

    @SerializedName("downloaded")
    public String downloadDate;

    public Integer id;
    public String name;
    public String result;
    public Map<String, Object> contents;
    public String agent;
    public Boolean override;

    public Map<String, CloudSyncState> files;
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
