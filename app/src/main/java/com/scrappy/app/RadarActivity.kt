package com.scrappy.app

import android.os.Bundle
import android.net.Uri
import android.util.Base64
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.scrappy.app.data.Prefs
import org.json.JSONArray
import org.json.JSONObject

class RadarActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var pageFailed=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,24,16,16)}
        root.addView(TextView(this).apply{text="Weather Radar • NOAA/NWS";textSize=23f})
        root.addView(TextView(this).apply{text="U.S. radar coverage. Use the map’s play control to animate precipitation. Internet required."})
        val status=TextView(this).apply{text="Loading radar…"};root.addView(status)
        web=WebView(this)
        web.settings.apply {
            javaScriptEnabled=true;domStorageEnabled=true
            allowFileAccess=false;allowContentAccess=false
            mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
        }
        web.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(view:WebView, request:WebResourceRequest):Boolean {
                val uri=request.url
                return uri.scheme!="https" || uri.host!="radar.weather.gov"
            }
            override fun onPageFinished(view:WebView,url:String){if(!pageFailed)status.text="NOAA/NWS • Check the radar timestamp for freshness."}
            override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){
                if(request.isForMainFrame){pageFailed=true;status.text="Radar unavailable. Check your internet connection, then tap Retry."}
            }
            override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,errorResponse:WebResourceResponse){
                if(request.isForMainFrame){pageFailed=true;status.text="Radar service unavailable. Tap Retry to try again."}
            }
        }
        root.addView(web,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(Button(this).apply{text="Retry / Refresh";setOnClickListener{pageFailed=false;status.text="Loading radar…";web.reload()}})
        root.addView(Button(this).apply{text="Back to Scrappy";setOnClickListener{finish()}})
        setContentView(root)
        applyScreenInsets(root)
        val place=Prefs(this).weatherPlace()
        val url=if(place==null) "https://radar.weather.gov/" else {
            val center=JSONArray().put(place.longitude).put(place.latitude)
            val settings=JSONObject().put("agenda",JSONObject().put("id","weather").put("center",center).put("location",center).put("zoom",7)).put("animating",true).put("base","standard")
            "https://radar.weather.gov/?settings="+Uri.encode("v1_"+Base64.encodeToString(settings.toString().toByteArray(Charsets.UTF_8),Base64.NO_WRAP))
        }
        web.loadUrl(url)
    }
    override fun onPause(){web.onPause();super.onPause()}
    override fun onResume(){super.onResume();if(::web.isInitialized)web.onResume()}
    override fun onDestroy(){web.stopLoading();(web.parent as? android.view.ViewGroup)?.removeView(web);web.destroy();super.onDestroy()}
}
