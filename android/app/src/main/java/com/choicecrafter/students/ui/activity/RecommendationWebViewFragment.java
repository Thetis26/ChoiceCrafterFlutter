package com.choicecrafter.studentapp.ui.activity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import com.choicecrafter.studentapp.R;

public class RecommendationWebViewFragment extends Fragment {

    private WebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendation_web_view, container, false);
        webView = view.findViewById(R.id.recommendations_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.getSettings().setSafeBrowsingEnabled(true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("WebViewFragment", "Finished loading URL: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request != null ? request.getUrl().toString() : null;
                if (!isUrlAllowed(url)) {
                    Log.w("WebViewFragment", "Blocked navigation to unsafe URL: " + url);
                    Toast.makeText(requireContext(), R.string.error_invalid_recommendation_url, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!isUrlAllowed(url)) {
                    Log.w("WebViewFragment", "Blocked navigation to unsafe URL: " + url);
                    Toast.makeText(requireContext(), R.string.error_invalid_recommendation_url, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        if (getArguments() != null) {
            String url = getArguments().getString("url");
            if (!loadUrlIfAllowed(webView, url)) {
                Toast.makeText(requireContext(), R.string.error_invalid_recommendation_url, Toast.LENGTH_SHORT).show();
            }
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView = null;
    }

    private boolean isUrlAllowed(String url) {
        if (TextUtils.isEmpty(url) || !URLUtil.isHttpsUrl(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        return uri.getHost() != null;
    }

    private boolean loadUrlIfAllowed(WebView targetWebView, String url) {
        if (targetWebView == null || !isUrlAllowed(url)) {
            Log.w("WebViewFragment", "Blocked navigation to unsafe URL: " + url);
            return false;
        }
        targetWebView.loadUrl(url);
        return true;
    }
}
