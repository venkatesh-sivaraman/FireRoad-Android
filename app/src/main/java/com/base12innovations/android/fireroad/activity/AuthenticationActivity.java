package com.base12innovations.android.fireroad.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

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
        final ProgressBar progressBar = findViewById(R.id.toolbar_progress_bar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent i = getIntent();
        if (i != null) {
            String url = i.getStringExtra(AUTH_URL_EXTRA);
            WebView wv = findViewById(R.id.webView);
            wv.loadUrl(url);
            WebSettings mWebSettings = wv.getSettings();
            mWebSettings.setJavaScriptEnabled(true);
            mWebSettings.setDomStorageEnabled(true);
            mWebSettings.setSupportZoom(false);
            mWebSettings.setAllowFileAccess(true);
            mWebSettings.setAllowContentAccess(true);
            wv.setWebViewClient(new WebViewClient() {

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);

                    progressBar.setVisibility(View.GONE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        new AlertDialog.Builder(AuthenticationActivity.this).setTitle(error.getDescription()).setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    progressBar.setVisibility(View.GONE);
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
                                    if (s != null && !s.equals("false") && s.contains("{") && s.contains("}")) {
                                        Intent i = new Intent();
                                        i.putExtra(AUTH_RESULT_EXTRA, s);
                                        setResult(RESULT_OK, i);
                                        finish();
                                    }
                                }
                            });
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    progressBar.setVisibility(View.VISIBLE);
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
