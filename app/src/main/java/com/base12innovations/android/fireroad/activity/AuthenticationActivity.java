package com.base12innovations.android.fireroad.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.AppSettings;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthenticationActivity extends AppCompatActivity {

    public static String AUTH_URL_EXTRA = "com.base12innovations.android.fireroad.authURL";
    public static String AUTH_RESULT_EXTRA ="com.base12innovations.android.fireroad.authResult";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent i = getIntent();
        if (i != null) {
            String url = i.getStringExtra(AUTH_URL_EXTRA);
            WebView wv = findViewById(R.id.webView);
            wv.loadUrl(url);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    if (url.contains("decline")) {
                        setResult(RESULT_CANCELED);
                        finish();
                        return;
                    }

                    view.evaluateJavascript("(document.getElementById('access_info') != null) ? " +
                                    "document.getElementById('access_info').innerHTML : false",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {
                                    if (s != null && !s.equals("false")) {
                                        Intent i = new Intent();
                                        i.putExtra(AUTH_RESULT_EXTRA, s);
                                        setResult(RESULT_OK, i);
                                        finish();
                                    }
                                }
                            });
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            AppSettings.setAllowsRecommendations(AppSettings.RECOMMENDATIONS_DISALLOWED);
        }
        return super.onOptionsItemSelected(item);
    }
}
