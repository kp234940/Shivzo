package com.shivzo.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.shivzo.app.repository.NotificationRepository
import com.shivzo.app.ui.theme.ShivzoOrangeBrand

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFcmScreen(
    onRefreshFcmToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ShivzoWebContainer()
        }
    }
}

class ShivzoJsInterface(private val context: Context) {
    @JavascriptInterface
    fun getFcmToken(): String {
        return NotificationRepository.fcmToken.value ?: ""
    }

    @JavascriptInterface
    fun getDeviceToken(): String {
        return NotificationRepository.fcmToken.value ?: ""
    }

    @JavascriptInterface
    fun getAndroidId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    @JavascriptInterface
    fun getDeviceId(): String {
        return getAndroidId()
    }

    @JavascriptInterface
    fun getRingtoneMode(): String {
        return NotificationRepository.ringtoneMode.value
    }

    @JavascriptInterface
    fun hasLocationPermission(): Boolean {
        val fine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @JavascriptInterface
    fun requestLocationPermission() {
        (context as? Activity)?.runOnUiThread {
            if (!hasLocationPermission()) {
                (context as? Activity)?.requestPermissions(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1001
                )
            }
        }
    }

    @JavascriptInterface
    fun getLocation(): String {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return "{\"status\":\"error\", \"message\":\"Location service unavailable\"}"

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                return "{\"status\":\"disabled\", \"message\":\"GPS disabled\"}"
            }

            if (!hasLocationPermission()) {
                requestLocationPermission()
                return "{\"status\":\"permission_denied\", \"message\":\"Location permission requested\"}"
            }

            var loc: Location? = null
            if (isGpsEnabled) {
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (loc == null && isNetworkEnabled) {
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (loc != null) {
                val json = org.json.JSONObject()
                json.put("status", "success")
                json.put("latitude", loc.latitude)
                json.put("longitude", loc.longitude)
                json.put("accuracy", loc.accuracy.toDouble())

                try {
                    val geocoder = Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                        val country = address.countryName ?: ""
                        json.put("city", city)
                        json.put("country", country)
                        json.put("address", address.getAddressLine(0) ?: "")
                    }
                } catch (e: Exception) {
                    // Ignore geocoder exceptions
                }
                json.toString()
            } else {
                "{\"status\":\"error\", \"message\":\"Location not found yet\"}"
            }
        } catch (e: Exception) {
            "{\"status\":\"error\", \"message\":\"${e.message}\"}"
        }
    }
}

@Composable
private fun ShivzoWebContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webUrl = "https://shivzo.in/"

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(!isNetworkAvailable(context)) }
    var backPressedTime by remember { mutableLongStateOf(0L) }

    // Lifecycle Observer to Pause/Resume and Destroy WebView safely (Prevents Memory Leaks & Background RAM Usage)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.let { wv ->
                        try {
                            wv.stopLoading()
                            wv.loadUrl("about:blank")
                            wv.clearHistory()
                            wv.removeAllViews()
                            wv.destroy()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    webViewRef = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    // ignore
                }
            }
            webViewRef = null
        }
    }

    // Dynamic Network State Listener
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                (context as? Activity)?.runOnUiThread {
                    if (isOffline) {
                        isOffline = false
                        isLoading = true
                        webViewRef?.reload()
                    }
                }
            }

            override fun onLost(network: Network) {
                (context as? Activity)?.runOnUiThread {
                    isOffline = true
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            // ignore
        }

        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Android Back Button Navigation
    BackHandler(enabled = true) {
        val wv = webViewRef
        if (isOffline) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                backPressedTime = currentTime
                Toast.makeText(context, "Exit karne ke liye dobara back dabayein", Toast.LENGTH_SHORT).show()
            }
        } else if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                backPressedTime = currentTime
                Toast.makeText(context, "Exit karne ke liye dobara back dabayein", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        // Geolocation & Location Bridge Support
                        setGeolocationEnabled(true)
                        setGeolocationDatabasePath(ctx.filesDir.path)

                        // Fast Loading & Ultra High Performance Optimizations
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setRenderPriority(WebSettings.RenderPriority.HIGH)
                        offscreenPreRaster = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        mediaPlaybackRequiresUserGesture = false
                    }

                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(ShivzoJsInterface(ctx), "ShivzoAndroid")

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 30) {
                                isLoading = false
                            }
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            callback?.invoke(origin, true, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isOffline = true
                                isLoading = false
                            }
                        }

                        @Suppress("DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            isOffline = true
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            if (url != null && (url.contains("shivzo.in") || url.contains("shivzo"))) {
                                return false
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                            return true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            val token = NotificationRepository.fcmToken.value ?: ""
                            val androidId = try {
                                android.provider.Settings.Secure.getString(
                                    ctx.contentResolver,
                                    android.provider.Settings.Secure.ANDROID_ID
                                ) ?: ""
                            } catch (e: Exception) {
                                ""
                            }

                            val jsCode = """
                                window.FCM_TOKEN = '$token';
                                window.ANDROID_ID = '$androidId';
                                window.DEVICE_ID = '$androidId';
                                if (typeof window.ShivzoAndroid === 'undefined') {
                                    window.ShivzoAndroid = {
                                        getFcmToken: function() { return '$token'; },
                                        getDeviceToken: function() { return '$token'; },
                                        getAndroidId: function() { return '$androidId'; },
                                        getDeviceId: function() { return '$androidId'; }
                                    };
                                }
                                try {
                                    var aidInputs = document.querySelectorAll("input[name='android_id'], input[id='android_id'], input[name='device_id'], input[id='device_id']");
                                    aidInputs.forEach(function(inp) { if (inp) inp.value = '$androidId'; });
                                    var fcmInputs = document.querySelectorAll("input[name='fcm_token'], input[id='fcm_token'], input[name='token'], input[id='token']");
                                    fcmInputs.forEach(function(inp) { if (inp) inp.value = '$token'; });
                                } catch(e) {}
                                window.dispatchEvent(new CustomEvent('fcm_token_ready', { 
                                    detail: { token: '$token', android_id: '$androidId', device_id: '$androidId' } 
                                }));
                                window.dispatchEvent(new CustomEvent('android_id_ready', { detail: '$androidId' }));
                            """.trimIndent()

                            view?.evaluateJavascript(jsCode, null)
                        }
                    }
                    if (isNetworkAvailable(ctx)) {
                        loadUrl(webUrl)
                    } else {
                        isOffline = true
                    }
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            onRelease = { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (e: Exception) {
                    // ignore
                }
                webViewRef = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading && !isOffline) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = ShivzoOrangeBrand
            )
        }

        if (isOffline) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "No Internet Connection",
                        modifier = Modifier.size(80.dp),
                        tint = ShivzoOrangeBrand
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No Internet Connection",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kripya apna Mobile Data ya Wi-Fi chalu karein aur dobara koshish karein.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            if (isNetworkAvailable(context)) {
                                isOffline = false
                                isLoading = true
                                if (webViewRef?.url.isNullOrEmpty()) {
                                    webViewRef?.loadUrl(webUrl)
                                } else {
                                    webViewRef?.reload()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Internet connection abhi bhi chalu nahi hai",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShivzoOrangeBrand,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Dobara Try Karein",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
