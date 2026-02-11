package com.airtel.usagetracker.data

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.airtel.usagetracker.data.models.ScrapedData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RouterScraper(private val context: Context) {
    
    private val TAG = "RouterScraper"
    
    suspend fun scrapeRouterData(
        routerIp: String,
        username: String,
        password: String
    ): ScrapedData = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            
            // WebView MUST be created on the main thread
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
            
            var isResumed = false
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    
                    when {
                        url?.contains("login", ignoreCase = true) == true || 
                        url == "http://$routerIp/" -> {
                            // Login page - inject credentials
                            Log.d(TAG, "Login page loaded, injecting credentials")
                            val loginScript = """
                                (function() {
                                    var userField = document.getElementsByName('Loginuser')[0];
                                    var passField = document.getElementsByName('Loginpwd')[0];
                                    if (userField && passField) {
                                        userField.value = '$username';
                                        passField.value = '$password';
                                        
                                        // Simulate Tab and Enter
                                        userField.focus();
                                        var form = userField.form;
                                        if (form) {
                                            form.submit();
                                        }
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(loginScript, null)
                        }
                        
                        url?.contains("traffic_wan_frame2.cgi") == true -> {
                            // Data page - scrape the table
                            Log.d(TAG, "Data page loaded, scraping table")
                            val scrapeScript = """
                                (function() {
                                    var rows = document.querySelectorAll('tr');
                                    for (var i = 0; i < rows.length; i++) {
                                        var row = rows[i];
                                        if (row.textContent.indexOf('PPPoE') !== -1) {
                                            var cols = row.querySelectorAll('td');
                                            if (cols.length >= 9) {
                                                var uptime = cols[2].textContent.trim();
                                                var tx = cols[5].textContent.trim();
                                                var rx = cols[8].textContent.trim();
                                                return JSON.stringify({
                                                    uptime: uptime,
                                                    tx: tx,
                                                    rx: rx
                                                });
                                            }
                                        }
                                    }
                                    return null;
                                })();
                            """.trimIndent()
                            
                            view?.evaluateJavascript(scrapeScript) { result ->
                                try {
                                    if (result != null && result != "null") {
                                        val cleanResult = result.trim('"').replace("\\\"", "\"")
                                        Log.d(TAG, "Scraped data: $cleanResult")
                                        
                                        // Parse JSON manually
                                        val uptimeMatch = Regex(""""uptime":"([^"]+)"""").find(cleanResult)
                                        val txMatch = Regex(""""tx":"([^"]+)"""").find(cleanResult)
                                        val rxMatch = Regex(""""rx":"([^"]+)"""").find(cleanResult)
                                        
                                        if (uptimeMatch != null && txMatch != null && rxMatch != null) {
                                            val uptimeStr = uptimeMatch.groupValues[1]
                                            val txStr = txMatch.groupValues[1]
                                            val rxStr = rxMatch.groupValues[1]
                                            
                                            val uptimeSeconds = parseUptime(uptimeStr)
                                            val tx = txStr.toLongOrNull() ?: 0L
                                            val rx = rxStr.toLongOrNull() ?: 0L
                                            
                                            if (!isResumed) {
                                                isResumed = true
                                                continuation.resume(ScrapedData(tx, rx, uptimeSeconds))
                                            }
                                        } else {
                                            if (!isResumed) {
                                                isResumed = true
                                                continuation.resumeWithException(
                                                    Exception("Failed to parse scraped data")
                                                )
                                            }
                                        }
                                    } else {
                                        if (!isResumed) {
                                            isResumed = true
                                            continuation.resumeWithException(
                                                Exception("No data found in table")
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing scraped data", e)
                                    if (!isResumed) {
                                        isResumed = true
                                        continuation.resumeWithException(e)
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            // Unknown page, try navigating to data page
                            Log.d(TAG, "Unknown page: $url, navigating to data page")
                            view?.loadUrl("http://$routerIp/cgi-bin/traffic_wan_frame2.cgi")
                        }
                    }
                }
            }
            
            continuation.invokeOnCancellation {
                webView.destroy()
            }
            
            // Start by loading login page
            webView.loadUrl("http://$routerIp/")
        }
    }
    
    private fun parseUptime(uptimeStr: String): Long {
        // Format: "D:HH:MM:SS" or "D:HH: M:SS" (with spaces)
        val parts = uptimeStr.replace(" ", "").split(":")
        
        return when (parts.size) {
            4 -> {
                val days = parts[0].toLongOrNull() ?: 0
                val hours = parts[1].toLongOrNull() ?: 0
                val minutes = parts[2].toLongOrNull() ?: 0
                val seconds = parts[3].toLongOrNull() ?: 0
                days * 86400 + hours * 3600 + minutes * 60 + seconds
            }
            3 -> {
                // Format: HH:MM:SS (no days)
                val hours = parts[0].toLongOrNull() ?: 0
                val minutes = parts[1].toLongOrNull() ?: 0
                val seconds = parts[2].toLongOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            }
            else -> 0L
        }
    }
}
