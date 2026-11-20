package com.scrappy.app

import android.content.Intent
import android.content.ActivityNotFoundException
import com.scrappy.app.data.Calculations
import com.scrappy.app.data.PriceQuote
import com.scrappy.app.data.PriceQuotes
import com.scrappy.app.data.DailyVerse
import com.scrappy.app.data.WeatherService
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.scrappy.app.data.Prefs
import com.scrappy.app.data.ScrappyRepository
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val worker=Executors.newSingleThreadExecutor()
    private val handler=Handler(Looper.getMainLooper())
    private var homeQuote: TextView?=null
    private var homeWeather: TextView?=null
    private var weatherInFlight: TextView?=null
    private val quoteTick = object : Runnable {
        override fun run() {
            refreshQuote()
            handler.postDelayed(this, 60_000)
        }
    }
    override fun onResume(){super.onResume();refreshQuote();handler.post(quoteTick)}
    override fun onPause(){handler.removeCallbacks(quoteTick);super.onPause()}
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy()}
    private fun refreshQuote(){val v=DailyVerse.forDate();homeQuote?.text="Daily encouragement\n\n“${v.words}”\n— ${v.reference} (KJV)"}
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); prefs=Prefs(this); if(intent.getBooleanExtra("open_identifier",false)) showIdentifierHelper() else showHome() }
    override fun setContentView(view: android.view.View?) { super.setContentView(view);view?.let { applyScreenInsets(it) } }
    private fun root(title:String)=ScrollView(this).also{sv-> homeQuote=null;homeWeather=null; val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,36,28,28)}; sv.addView(l); l.addView(TextView(this).apply{text=title;textSize=27f})}
    private fun body(sv:ScrollView)=sv.getChildAt(0) as LinearLayout
    private fun button(l:LinearLayout,text:String,action:()->Unit)=Button(this).apply{this.text=text;setOnClickListener{action()};l.addView(this,ViewGroup.LayoutParams(-1,-2))}
    private fun text(l:LinearLayout,s:String,size:Float=17f)=TextView(this).apply{text=s;textSize=size;setPadding(0,8,0,8);l.addView(this)}
    private fun back(l:LinearLayout)=button(l,"Back to home"){showHome()}
    private fun number(hint:String)=EditText(this).apply{this.hint=hint;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL}
    private fun valid(field: EditText, label: String, default: Double? = null, max: Double = 1_000_000.0): Double? {
        val raw = field.text.toString().trim()
        val value = if (raw.isEmpty()) default else raw.toDoubleOrNull()
        if (value == null || !value.isFinite() || value < 0.0 || value > max) {
            field.error = "$label must be between 0 and $max"; field.requestFocus(); return null
        }
        field.error = null; return value
    }
    private fun openExternal(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { info("No app is available to open this action.") }
    }
    private fun money(v:Double)=String.format(Locale.US,"$%.2f",v)

    private fun showHome(){ val sv=root("SCRAPPY • v${BuildConfig.VERSION_NAME}"); val l=body(sv)
        homeQuote=text(l,"",19f).apply{setBackgroundColor(0xFFF2E5D4.toInt());setTextColor(0xFF30251C.toInt());setPadding(24,24,24,24)};refreshQuote()
        homeWeather=text(l,"🌤 Weather — choose your city",18f)
        button(l,"Weather location"){showWeatherLocation()}
        button(l,"🌧 Open Weather Radar"){startActivity(Intent(this,RadarActivity::class.java))}
        button(l,"Refresh weather"){refreshWeather()};refreshWeather()
        text(l,"Identify metal. Estimate value. Compare yards. Keep your best scrap decisions organized.",18f)
        button(l,"📷 Camera Metal Identifier"){startActivity(Intent(this,CameraActivity::class.java))}
        button(l,"🧭 Identification Helper"){showIdentifierHelper()};button(l,"💰 Scrap Price Guide"){showPrices()};button(l,"📈 Price History"){showHistory()};button(l,"⚙ Motor Value Calculator"){showMotor()};button(l,"🧮 Load Value Calculator"){showLoadValue()};button(l,"🚚 Mixed Loads, Trips & Sales"){startActivity(Intent(this,LedgerActivity::class.java))};button(l,"📊 Earnings & Profit Tools"){startActivity(Intent(this,WorkbenchActivity::class.java))};button(l,"📞 My Scrapyard Contacts"){showContacts()};button(l,"📍 Demo Yard Payout Comparison"){showYards()};button(l,"⭐ Favorite Yards"){showFavorites()};button(l,"🧲 Magnet Test Guide"){showMagnetGuide()};button(l,"📚 Metal Reference Library"){showMetalLibrary()};button(l,"⚙ Settings"){showSettings()};button(l,"🔒 Privacy & Safety"){showPrivacy()}; setContentView(sv) }

    private fun refreshWeather(){
        val target=homeWeather ?: return
        if(weatherInFlight===target)return
        val place=prefs.weatherPlace() ?: run{target.text="🌤 Weather — choose your city";return}
        val cache=prefs.weatherCache()
        target.text=if(cache.isBlank()) "Loading weather for ${place.name}…" else "$cache\nSaved report — refreshing…"
        weatherInFlight=target
        worker.execute {
            val outcome=runCatching { WeatherService.current(place) }
            handler.post {
                if(weatherInFlight===target)weatherInFlight=null
                if(isDestroyed || isFinishing || homeWeather!==target || prefs.weatherPlace()!=place)return@post
                outcome.onSuccess{c->
                    val value="${WeatherService.label(c.code,c.isDay)} • ${String.format(Locale.US,"%.0f",c.temperature)}°F\n${place.name}\nModel time: ${c.time.replace('T',' ')} (location time)\nWeather by Open-Meteo"
                    prefs.saveWeatherCache(value);target.text=value
                }.onFailure{target.text=if(cache.isBlank()) "Weather unavailable for ${place.name}. Check your connection and tap Refresh weather." else "$cache\nOffline / refresh failed — saved report may be outdated."}
            }
        }
    }
    private fun showWeatherLocation(){
        val sv=root("Weather Location");val l=body(sv)
        text(l,"Enter a city or ZIP code, then select the correct location. Your search and selected city coordinates are sent to Open-Meteo for weather. GPS permission is not required.")
        val city=EditText(this).apply{hint="City, state or ZIP code"};l.addView(city)
        val results=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};l.addView(results)
        val search=button(l,"Search locations"){}
        search.setOnClickListener{
            val query=city.text.toString().trim()
            if(query.length<2){city.error="Enter a city or ZIP code";return@setOnClickListener}
            search.isEnabled=false;results.removeAllViews();text(results,"Searching…")
            worker.execute{
                val outcome=runCatching{WeatherService.search(query)}
                handler.post{
                    if(isDestroyed || isFinishing || !search.isAttachedToWindow)return@post
                    search.isEnabled=true;results.removeAllViews()
                    outcome.onSuccess{places->
                        if(places.isEmpty())text(results,"No matches. Try a nearby city or add the state.")
                        places.forEach{place->button(results,place.name){prefs.setWeatherPlace(place);showHome()}}
                    }.onFailure{text(results,"Location search unavailable. Check your connection and try again.")}
                }
            }
        }
        button(l,"Clear saved weather location"){prefs.clearWeatherPlace();showHome()}
        back(l);setContentView(sv)
    }
    private fun selectedPrice(metal:String)=PriceQuotes.estimatePriceFor(metal,prefs.priceQuotes())
    private fun priceSource(metal:String):String {
        val quote=PriceQuotes.latest(metal,prefs.priceQuotes()) ?: return "$metal: demo benchmark; confirm with a yard"
        val date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(quote.recordedAt))
        val warning=if(PriceQuotes.needsConfirmation(quote,System.currentTimeMillis())) " • Reconfirm this saved quote" else " • User-entered; call to confirm"
        return "$metal: ${money(quote.pricePerLb)}/lb • ${quote.yard} • $date$warning"
    }
    private fun showPrices(){
        val sv=root("Scrap Price Guide");val l=body(sv)
        text(l,"Your saved yard quotes are used by the load and motor calculators. Metals without a saved quote use demo benchmarks. Quotes are entered by you and are not a live market feed.")
        button(l,"Add or update a yard quote"){showPriceQuote()}
        ScrappyRepository.metals.forEach{m->
            text(l,priceSource(m.name),15f)
            button(l,"Save ${m.name} price snapshot — ${money(selectedPrice(m.name))}/lb"){
                prefs.addPriceSnapshot(m.name,selectedPrice(m.name),priceSource(m.name));info("Saved ${m.name} price snapshot.")
            }
        }
        back(l);setContentView(sv)
    }
    private fun showPriceQuote(){
        val sv=root("My Yard Quotes");val l=body(sv)
        text(l,"Enter the yard's quoted price in US dollars per pound for the exact metal grade. Saving updates only that grade at that yard; quotes from other yards stay saved. Up to 500 quotes can be stored. Quotes remain saved until you remove them; reports older than 24 hours are flagged for confirmation.")
        val metal=metalSpinner();l.addView(metal)
        val yard=EditText(this).apply{hint="Scrapyard name (required)";filters=arrayOf(android.text.InputFilter.LengthFilter(120))};l.addView(yard)
        val contacts=prefs.contacts()
        if(contacts.isNotEmpty()){
            val choose=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Choose a saved contact")+contacts.map{it.name})};l.addView(choose)
            button(l,"Use selected contact name"){val index=choose.selectedItemPosition-1;if(index>=0)yard.setText(contacts[index].name)}
        }
        val price=number("Quoted price $/lb (example 3.50)");l.addView(price)
        button(l,"Save yard quote"){
            val name=yard.text.toString().trim()
            if(name.isBlank()){yard.error="Enter the scrapyard name";return@button}
            val value=valid(price,"Price per pound",max=10_000.0) ?: return@button
            try { prefs.savePriceQuote(PriceQuote(metal.selectedItem.toString(),value,name,System.currentTimeMillis()));showPrices() }
            catch(e:IllegalArgumentException) { info(e.message ?: "Unable to save quote") }
        }
        prefs.priceQuotes().sortedBy{it.metal}.forEach{quote->
            text(l,"${quote.yard} • ${quote.metal}: ${money(quote.pricePerLb)}/lb\n${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(quote.recordedAt))} • ${if(PriceQuotes.needsConfirmation(quote,System.currentTimeMillis())) "Reconfirm date and price" else "User-entered; confirm before sale"}",15f)
            button(l,"Remove ${quote.yard} • ${quote.metal} quote"){
                androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Remove ${quote.yard}’s quote for ${quote.metal}? Other yards’ quotes and saved estimates stay unchanged.")
                    .setPositiveButton("Remove"){_,_->prefs.removePriceQuote(quote.metal,quote.yard);showPriceQuote()}.setNegativeButton("Cancel",null).show()
            }
        }
        back(l);setContentView(sv)
    }
    private fun showHistory(){val sv=root("Saved Price History");val l=body(sv);val h=prefs.history();if(h.isEmpty())text(l,"No saved price snapshots yet. Save one in Price Guide.") else h.forEach{x->text(l,"${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(x.recordedAt))} • ${x.metal} • ${money(x.price)}/lb\n${x.source}")};back(l);setContentView(sv)}
    private fun showIdentifierHelper(){val sv=root("Identification Helper");val l=body(sv);text(l,"Use several clues together—appearance, magnet response, weight/density, sparks (only where safe), markings and known item type. Camera AI should be treated as an estimate, not proof.");val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("Magnetic","Not magnetic","Not sure"));l.addView(sp);button(l,"Show likely groups"){val s=sp.selectedItem.toString();info(if(s=="Magnetic")"Start with steel, iron and some stainless grades." else if(s=="Not magnetic")"Consider copper, brass, aluminum, lead, zinc and many stainless grades." else "Check color, density, markings and a real magnet before deciding.")};back(l);setContentView(sv)}
    private fun showMotor(){val sv=root("Motor Value Calculator");val l=body(sv);val w=number("Motor weight (lb)");val pct=number("Estimated copper % (example 12)");val whole=number("Whole motor yard price per lb (default 0.28)");val minutes=number("Minutes needed to strip");listOf(w,pct,whole,minutes).forEach{l.addView(it)};val out=TextView(this).apply{textSize=18f};l.addView(out);button(l,"Compare whole vs strip"){val wt=valid(w,"Motor weight") ?: return@button
        val p=valid(pct,"Copper percentage",max=100.0) ?: return@button
        val wh=valid(whole,"Whole price",default=0.28) ?: return@button
        val min=valid(minutes,"Stripping time") ?: return@button
        val result=Calculations.motor(wt,p,wh,min,prefs.laborRate(),selectedPrice("Copper #2"),selectedPrice("Steel"))
        out.text="Sell whole: ${money(result.whole)}\nSeparated gross: ${money(result.gross)}\nEstimated labor cost: ${money(result.labor)}\nSeparated net: ${money(result.net)}\nDifference vs whole: ${money(result.net-result.whole)}\n\n${priceSource("Copper #2")}\n${priceSource("Steel")}\n\nAssumes all remaining weight is saleable steel. Confirm winding material, recovery and current yard prices before stripping."};back(l);setContentView(sv)}
    private fun showLoadValue(){val sv=root("Load Value Calculator");val l=body(sv);val sp=metalSpinner();l.addView(sp);val wt=number("Weight (lb)");l.addView(wt);val out=TextView(this).apply{textSize=19f};l.addView(out);button(l,"Estimate load value"){val m=sp.selectedItem.toString();val w=valid(wt,"Weight") ?: return@button;out.text="$m × ${String.format(Locale.US,"%.2f",w)} lb = ${money(w*selectedPrice(m))}\n${priceSource(m)}"};button(l,"Share estimate"){val s=out.text.toString();if(s.isNotBlank())openExternal(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"Scrappy estimate: $s")},"Share Scrappy estimate"))};back(l);setContentView(sv)}
    private fun metalSpinner()=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,ScrappyRepository.metals.map{it.name});val idx=ScrappyRepository.metals.indexOfFirst{it.name==prefs.defaultMetal()};if(idx>=0)setSelection(idx)}
    private fun showYards(){val sv=root("Demo Yard Payout Comparison");val l=body(sv);text(l,"Example yards, distances and prices. These are not local business listings. Add real businesses in My Scrapyard Contacts.");val sp=metalSpinner();l.addView(sp);val wt=number("Weight (lb)");l.addView(wt);val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};l.addView(box);button(l,"Compare payouts"){box.removeAllViews();val m=sp.selectedItem.toString();val w=valid(wt,"Weight") ?: return@button;ScrappyRepository.yards.sortedByDescending{(it.prices[m]?:0.0)*w}.forEachIndexed{i,y->text(box,"${i+1}. ${y.name} • ${y.miles} mi • ${money((y.prices[m]?:0.0)*w)} @ ${money(y.prices[m]?:0.0)}/lb");val row=LinearLayout(this);button(row,if(prefs.favoriteYards().contains(y.id))"★" else "☆"){prefs.toggleFavorite(y.id);showYards()};box.addView(row)}};back(l);setContentView(sv)}
    private fun showFavorites(){val sv=root("Favorite Yards");val l=body(sv);val fav=prefs.favoriteYards();val ys=ScrappyRepository.yards.filter{fav.contains(it.id)};if(ys.isEmpty())text(l,"No favorites yet. Add one from Yard Payout Comparison.") else ys.forEach{y->text(l,"★ ${y.name} • ${y.miles} mi • ${y.hours}");text(l,"Demo yard — no verified contact details.",14f)};back(l);setContentView(sv)}
    private fun showContacts(){
        val sv=root("My Scrapyard Contacts");val l=body(sv)
        text(l,"Save scrapyards you know. Tap Call to open your phone dialer. These contacts are stored on this device.")
        button(l,"Find scrapyards near me"){openExternal(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode("scrap metal recycling near me"))))}
        val name=EditText(this).apply{hint="Scrapyard name"};val phone=EditText(this).apply{hint="Phone number";inputType=InputType.TYPE_CLASS_PHONE};val address=EditText(this).apply{hint="Street address, city and state"}
        listOf(name,phone,address).forEach{l.addView(it)}
        button(l,"Save contact"){
            val n=name.text.toString().trim();val ph=phone.text.toString().trim();val a=address.text.toString().trim()
            if(n.isBlank()){name.error="Enter a name";return@button}
            if(!ph.matches(Regex("[+0-9() .-]{7,30}")) || ph.count{it.isDigit()} !in 7..15){phone.error="Enter a valid phone number";return@button}
            prefs.addContact(n,ph,a);showContacts()
        }
        prefs.contacts().forEach{contact->
            text(l,contact.name+"\n"+contact.phone+if(contact.address.isBlank()) "" else "\n"+contact.address)
            button(l,"Call ${contact.name}"){openExternal(Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",contact.phone.filter{it.isDigit() || it=='+'},null)))}
            if(contact.address.isNotBlank())button(l,"Directions"){openExternal(Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode(contact.address))))}
            button(l,"Remove contact"){androidx.appcompat.app.AlertDialog.Builder(this).setMessage("Remove ${contact.name}?").setPositiveButton("Remove"){_,_->prefs.removeContact(contact.id);showContacts()}.setNegativeButton("Cancel",null).show()}
        }
        back(l);setContentView(sv)
    }
    private fun showMagnetGuide(){val sv=root("Magnet Test Guide");val l=body(sv);text(l,"Strong attraction usually points toward iron or common steel. Weak/no attraction does not uniquely identify a metal: copper, brass, aluminum, lead, zinc and many stainless steels may all be non-magnetic. A phone's magnetometer detects magnetic fields; it does not turn the phone into a reliable scrap-metal identifier.");text(l,"Safety: keep strong magnets away from sensitive electronics, medical devices and pinch points.",15f);back(l);setContentView(sv)}
    private fun showMetalLibrary(){val sv=root("Metal Reference Library");val l=body(sv);ScrappyRepository.metals.forEach{m->button(l,m.name){showMetal(m.name)}};back(l);setContentView(sv)}
    private fun showMetal(name:String){val m=ScrappyRepository.metals.first{it.name==name};val sv=root(m.name);val l=body(sv);text(l,"Category: ${m.category}\nBenchmark: ${money(m.benchmarkPerLb)}/lb\nMagnetic clue: ${if(m.magnetic)"usually yes" else "usually no"}\nColor clue: ${m.colorHint}\n\n${m.notes}");back(l);setContentView(sv)}
    private fun showSettings(){val sv=root("Settings");val l=body(sv);text(l,"Default metal");val sp=metalSpinner();l.addView(sp);val rate=number("Labor rate $/hour").apply{setText(String.format(Locale.US,"%.2f",prefs.laborRate()))};l.addView(rate);button(l,"Save settings"){prefs.setDefaultMetal(sp.selectedItem.toString());val value=valid(rate,"Labor rate",max=10_000.0) ?: return@button;prefs.setLaborRate(value);info("Settings saved.")};back(l);setContentView(sv)}
    private fun showPrivacy(){val sv=root("Privacy & Safety");val l=body(sv);text(l,"Scrappy v${BuildConfig.VERSION_NAME} stores contacts, your yard quotes, your chosen weather city, a saved weather report, favorites, settings and price snapshots locally using Android SharedPreferences. Mixed loads, trip costs, saved estimates and actual sale records are also stored on this device. A local pre-import checkpoint and a temporary deletion undo record are kept with the ledger. CSV export writes sale records to the file you choose; sharing sends the selected estimate to the app you choose. Camera photos are written to app cache for the capture workflow. This build contains no Gemini API key. Camera photos stay local unless you explicitly choose Send and analyze in a build configured with an AI service and a public privacy policy. The sent copy is resized and re-encoded without original photo metadata. Scrappy’s supplied server processes images in memory; Google handles them under the operator’s service terms.");text(l,"Weather searches and city coordinates are sent to Open-Meteo when you choose a city. Opening radar loads NOAA/NWS inside the app and sends the selected map location to that service. Both require internet. Daily Bible verses work offline. Every AI upload requires a separate consent prompt. AI results are temporary visual estimates, not verified alloys or yard grades. Request limits and network errors fall back to the manual helper. Scrap values and identification results are estimates; confirm grade and price with the receiving yard.",15f);back(l);setContentView(sv)}
    private fun info(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
}
