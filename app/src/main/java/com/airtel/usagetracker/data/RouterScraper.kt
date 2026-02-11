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
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RouterScraper(private val context: Context) {
    
    private val TAG = "RouterScraper"
    
    suspend fun scrapeRouterData(
        routerIp: String,
        username: String,
        password: String,
        onStatusUpdate: (com.airtel.usagetracker.data.models.ScrapingStatus) -> Unit = {}
    ): ScrapedData = withContext(Dispatchers.Main) {
        // Add timeout wrapper
        withTimeout(30000) { // 30 second timeout
            suspendCancellableCoroutine { continuation ->
                
                onStatusUpdate(com.airtel.usagetracker.data.models.ScrapingStatus.CONNECTING)
                
                // WebView MUST be created on the main thread
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                }
                
                var isResumed = false
                var loginAttempted = false
                var loginPageCount = 0
                
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        Log.d(TAG, "Page loaded: $url")
                        
                        when {
                            url?.contains("login", ignoreCase = true) == true || 
                            url == "http://$routerIp/" -> {
                                loginPageCount++
                                
                                // If we see login page twice, authentication failed
                                if (loginPageCount > 1) {
                                    Log.e(TAG, "Login failed - redirected back to login page")
                                    if (!isResumed) {
                                        isResumed = true
                                        webView.destroy()
                                        continuation.resumeWithException(
                                            Exception("Login failed - incorrect username or password")
                                        )
                                    }
                                    return
                                }
                                
                                if (!loginAttempted) {
                                    // Login page - inject credentials and navigate
                                    Log.d(TAG, "Login page loaded, injecting credentials")
                                    onStatusUpdate(com.airtel.usagetracker.data.models.ScrapingStatus.LOGGING_IN)
                                    loginAttempted = true
                                    
                                    // First, inspect the page to see what fields exist
                                    val inspectScript = """
                                        (function() {
                                            var inputs = document.querySelectorAll('input');
                                            var inputInfo = [];
                                            for (var i = 0; i < inputs.length; i++) {
                                                inputInfo.push({
                                                    name: inputs[i].name,
                                                    id: inputs[i].id,
                                                    type: inputs[i].type
                                                });
                                            }
                                            return JSON.stringify(inputInfo);
                                        })();
                                    """.trimIndent()
                                    
                                    view?.evaluateJavascript(inspectScript) { inputsJson ->
                                        Log.d(TAG, "Login page inputs: $inputsJson")
                                        
                                        // Now try to login with the actual field names
                                        val loginScript = """
                                            (function() {
                                                // Use the actual field IDs from inspection
                                                var userField = document.getElementById('Loginuser');
                                                var passField = document.getElementById('LoginPassword');
                                                var passHidden = document.getElementsByName('LoginPasswordValue')[0];
                                                
                                                if (userField && passField) {
                                                    // Fill in the visible fields
                                                    userField.value = '$username';
                                                    passField.value = '$password';
                                                    
                                                    // Also fill the hidden password field if it exists
                                                    if (passHidden) {
                                                        passHidden.value = '$password';
                                                    }
                                                    
                                                    // DO NOT fill fake fields - they're honeypots
                                                    
                                                    // Find and click the submit button
                                                    var submitBtn = document.getElementById('Login_ID')
                                                                 || document.getElementsByName('Prestige_Login')[0]
                                                                 || document.querySelector('input[type="submit"]');
                                                    
                                                    if (submitBtn) {
                                                        submitBtn.click();
                                                        return 'clicked_button';
                                                    } else {
                                                        var form = document.querySelector('form');
                                                        if (form) {
                                                            form.submit();
                                                            return 'submitted_form';
                                                        }
                                                    }
                                                    return 'no_submit_method';
                                                } else {
                                                    return 'fields_not_found: user=' + (userField ? 'found' : 'missing') + ', pass=' + (passField ? 'found' : 'missing');
                                                }
                                            })();
                                        """.trimIndent()
                                        
                                        view?.evaluateJavascript(loginScript) { loginResult ->
                                            Log.d(TAG, "Login attempt result: $loginResult")
                                            // After submitting login, directly navigate to data page
                                            Log.d(TAG, "Login submitted, navigating to data page")
                                            onStatusUpdate(com.airtel.usagetracker.data.models.ScrapingStatus.NAVIGATING)
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                view?.loadUrl("http://$routerIp/cgi-bin/traffic_wan_frame2.cgi")
                                            }, 1500) // Wait 1.5s for login to process
                                        }
                                    }
                                }
                            }
                            
                            url?.contains("traffic_wan_frame2.cgi") == true -> {
                                // Data page - scrape the table
                                Log.d(TAG, "Data page loaded, scraping table")
                                onStatusUpdate(com.airtel.usagetracker.data.models.ScrapingStatus.SCRAPING_DATA)
                                val scrapeScript = """
                                    (function() {
                                        var rows = document.querySelectorAll('tr');
                                        for (var i = 0; i < rows.length; i++) {
                                            var row = rows[i];
                                            var rowText = row.textContent;
                                            
                                            // Look for the PPPoE row
                                            if (rowText.indexOf('PPPoE') !== -1) {
                                                var cols = row.querySelectorAll('td');
                                                
                                                // Log all cell contents for debugging
                                                var cellTexts = [];
                                                for (var j = 0; j < cols.length; j++) {
                                                    cellTexts.push(cols[j].textContent.trim());
                                                }
                                                
                                                // The table structure appears to be:
                                                // [Interface] [Uptime] [TX Packets] [TX Error] [TX Bytes] [RX Packets] [RX Error] [RX Bytes]
                                                // We need to find cells with numeric values
                                                
                                                var uptime = '';
                                                var txBytes = '';
                                                var rxBytes = '';
                                                
                                                // Extract uptime (format: "0: 2:24:50" or "0:2:24:50")
                                                for (var j = 0; j < cellTexts.length; j++) {
                                                    var cleaned = cellTexts[j].trim();
                                                    // Match cells that are ONLY uptime (no other text)
                                                    if (cleaned.match(/^\d+:\s*\d+:\d+:\d+$/)) {
                                                        uptime = cleaned;
                                                        break;
                                                    }
                                                }
                                                
                                                // Extract byte values (large numbers)
                                                var byteValues = [];
                                                for (var j = 0; j < cellTexts.length; j++) {
                                                    // Look for cells with pure numbers (bytes)
                                                    var cleaned = cellTexts[j].trim();
                                                    var num = parseInt(cleaned);
                                                    if (!isNaN(num) && num > 10000 && cleaned.match(/^\d+$/)) {
                                                        byteValues.push({
                                                            value: cleaned,
                                                            num: num,
                                                            index: j
                                                        });
                                                    }
                                                }
                                                
                                                // Sort by numeric value descending to get the largest values (bytes, not packets)
                                                byteValues.sort(function(a, b) { return b.num - a.num; });
                                                
                                                // The two largest values are TX and RX bytes
                                                // TX comes before RX in the table (lower index)
                                                if (byteValues.length >= 2) {
                                                    if (byteValues[0].index < byteValues[1].index) {
                                                        txBytes = byteValues[0].value;
                                                        rxBytes = byteValues[1].value;
                                                    } else {
                                                        txBytes = byteValues[1].value;
                                                        rxBytes = byteValues[0].value;
                                                    }
                                                }
                                                
                                                return JSON.stringify({
                                                    uptime: uptime,
                                                    tx: txBytes,
                                                    rx: rxBytes,
                                                    debug: cellTexts
                                                });
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
                                                    webView.destroy()
                                                    continuation.resume(ScrapedData(tx, rx, uptimeSeconds))
                                                }
                                            } else {
                                                if (!isResumed) {
                                                    isResumed = true
                                                    webView.destroy()
                                                    continuation.resumeWithException(
                                                        Exception("Failed to parse scraped data")
                                                    )
                                                }
                                            }
                                        } else {
                                            if (!isResumed) {
                                                isResumed = true
                                                webView.destroy()
                                                continuation.resumeWithException(
                                                    Exception("No data found in table")
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing scraped data", e)
                                        if (!isResumed) {
                                            isResumed = true
                                            webView.destroy()
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
                    
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        Log.e(TAG, "WebView error: $description at $failingUrl")
                        if (!isResumed) {
                            isResumed = true
                            webView.destroy()
                            continuation.resumeWithException(
                                Exception("WebView error: $description")
                            )
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
