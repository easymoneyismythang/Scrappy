package com.scrappy.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.scrappy.app.data.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.text.DateFormat
import java.util.Date

/** Tools operate on captured prices; scenario previews never write to the ledger. */
class WorkbenchActivity : AppCompatActivity() {
    private lateinit var store: LedgerStore
    private var pendingCsv: String? = null
    private var filter = SaleFilter()
    private var currentPage = "home"
    private val comparisonTrips = mutableListOf<YardTrip>()
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val token = pendingCsv; pendingCsv = null
        if (token != null) guarded { try { if (uri != null) {
            val csv=ExportSnapshot(this).read(token)
            val stream = contentResolver.openOutputStream(uri, "wt") ?: error("Could not open destination")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            Toast.makeText(this, "Filtered sales exported", Toast.LENGTH_LONG).show()
        } } finally { ExportSnapshot(this).remove(token) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); store = LedgerStore(this)
        pendingCsv = savedInstanceState?.getString("csv")
        savedInstanceState?.getParcelableArrayList<Bundle>("comparisonTrips")?.forEach { b ->
            comparisonTrips.add(YardTrip(b.getString("yard") ?: "", TripCosts(
                b.getDouble("miles"), b.getDouble("mpg"), b.getDouble("fuel"),
                b.getDouble("minutes"), b.getDouble("labor"), b.getDouble("fees"))))
        }
        savedInstanceState?.let { b ->
            filter = SaleFilter(b.getString("start")?.let { LocalDate.parse(it) }, b.getString("end")?.let { LocalDate.parse(it) },
                b.getString("yard"), b.getString("query") ?: "", SaleSort.valueOf(b.getString("sort") ?: "NEWEST"))
        }
        guarded { when (savedInstanceState?.getString("page")) { "reports" -> reports(); "load" -> loadTools(); "compare" -> compareYards(); "planner" -> planner(); else -> home() } }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("csv", pendingCsv); outState.putString("page",currentPage)
        outState.putString("start",filter.start?.toString()); outState.putString("end",filter.end?.toString())
        outState.putString("yard",filter.yard); outState.putString("query",filter.query); outState.putString("sort",filter.sort.name)
        outState.putParcelableArrayList("comparisonTrips", ArrayList(comparisonTrips.map { trip -> Bundle().apply {
            putString("yard", trip.yard); putDouble("miles",trip.costs.roundTripMiles); putDouble("mpg",trip.costs.mpg)
            putDouble("fuel",trip.costs.fuelPerGallon); putDouble("minutes",trip.costs.travelMinutes)
            putDouble("labor",trip.costs.laborPerHour); putDouble("fees",trip.costs.fees)
        } }))
        super.onSaveInstanceState(outState)
    }
    override fun setContentView(view: View?) { super.setContentView(view); view?.let { applyScreenInsets(it) } }
    private fun guarded(action: () -> Unit) {
        try { action() } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Unable to complete action").setMessage(e.message ?: "Check your entries")
                .setPositiveButton("OK",null).show()
        }
    }
    private fun page(title: String, key: String): LinearLayout {
        currentPage=key
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(28,32,28,28) }
        setContentView(ScrollView(this).apply { addView(box) });label(box,title,25f);return box
    }
    private fun label(box: LinearLayout, value: String, size: Float=17f) = TextView(this).also {
        it.text=value;it.textSize=size;it.setPadding(0,10,0,10);box.addView(it)
    }
    private fun button(box: LinearLayout, text: String, action: () -> Unit) = Button(this).also {
        it.text=text;it.setOnClickListener { guarded(action) };box.addView(it)
    }
    private fun field(box: LinearLayout, title: String, value: String="", numeric: Boolean=false, signed: Boolean=false): EditText {
        label(box,title,15f)
        return EditText(this).also {
            it.hint=title;it.contentDescription=title;it.setText(value)
            it.inputType=if(numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
                (if(signed) InputType.TYPE_NUMBER_FLAG_SIGNED else 0) else InputType.TYPE_CLASS_TEXT
            if(!numeric) it.filters=arrayOf(android.text.InputFilter.LengthFilter(120))
            box.addView(it)
        }
    }
    private fun number(f: EditText, min: Double=0.0, max: Double=1_000_000.0): Double {
        val n=f.text.toString().trim().toDoubleOrNull()
        if(n==null || !n.isFinite() || n<min || n>max) { f.error="Enter $min to $max";f.requestFocus();error("Check the highlighted value") }
        return n
    }
    private fun money(n: Double)=String.format(Locale.US,"$%.2f",n)
    private fun amount(n: Double)=String.format(Locale.US,"%.4f",n).trimEnd('0').trimEnd('.')
    private fun optional(n: Double?, currency: Boolean=false)=n?.let { if(currency) money(it) else amount(it) } ?: "Unavailable"
    private fun confirm(text: String, action: () -> Unit) {
        AlertDialog.Builder(this).setMessage(text).setPositiveButton("Apply") { _,_ -> guarded(action) }.setNegativeButton("Cancel",null).show()
    }
    private fun share(text: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type="text/plain";putExtra(Intent.EXTRA_TEXT,text)
    },"Share Scrappy report")) }
    private fun home() {
        val box=page("Earnings & Profit Tools","home")
        val s=store.snapshot()
        label(box,"Load: ${s.draft.items.size}/100 items • Estimates: ${s.estimates.size}/200 • Sales: ${s.sales.size}/200")
        button(box,"Earnings reports & filtered exports") { reports() }
        button(box,"Compare yards for my current load") { compareYards() }
        button(box,"Profit targets & what-if planner") { planner() }
        button(box,"Current load tools & quote review") { loadTools() }
        button(box,"Share complete ledger summary") {
            val snapshot=store.snapshot()
            share("Scrappy ledger summary\n${snapshot.estimates.size} saved estimates\n${snapshot.sales.size} receipts\n"+
                earningsText(SalesAnalytics.totals(snapshot.sales))+"\n\nCurrent load\n"+loadText(snapshot.draft))
        }
        if(store.canUndo()) button(box,"Undo last estimate or sale deletion") {
            confirm("Recover the most recent deleted record? Undo is available until the next ledger change.") { store.undoDelete();home() }
        }
        button(box,"Back") { finish() }
    }
    private fun earningsText(t: Earnings): String = "Receipts: ${t.count}\nPayout: ${money(t.payout)}\nCash expenses: ${money(t.expenses)}\nCash net: ${money(t.net)}\n"+
        "After estimated travel labor: ${money(t.afterLabor)}\nMiles: ${amount(t.miles)}\nModeled fuel: ${money(t.fuel)}\n"+
        "Average payout: ${optional(t.averagePayout,true)} • Average cash net: ${optional(t.averageNet,true)}\n"+
        "Cash net/mile: ${optional(t.netPerMile,true)} • Cash expenses/mile: ${optional(t.expensePerMile,true)}\n"+
        "Payout vs estimate: ${money(t.payoutDifference)} (${optional(t.payoutDifferencePercent)}%)"
    private fun reports() {
        val box=page("Earnings Reports","reports")
        label(box,"Dates use this phone's time zone. Week groups start Monday. Every receipt counts, including multiple receipts for one estimate. Reports include only saved receipts, not all business income. Expenses and labor use the original recorded figures.")
        var start=filter.start;var end=filter.end
        val dates=label(box,"From: ${start ?: "Any"} • Through: ${end ?: "Any"}")
        fun pick(initial: LocalDate?, selected: (LocalDate) -> Unit) {
            val d=initial ?: LocalDate.now()
            DatePickerDialog(this,{ _,y,m,day -> selected(LocalDate.of(y,m+1,day));dates.text="From: ${start ?: "Any"} • Through: ${end ?: "Any"}" },d.year,d.monthValue-1,d.dayOfMonth).show()
        }
        button(box,"Choose start date") { pick(start) { start=it } }
        button(box,"Choose end date") { pick(end) { end=it } }
        val today=LocalDate.now()
        button(box,"Use this month") { start=today.withDayOfMonth(1);end=today;dates.text="From: $start • Through: $end" }
        button(box,"Use this week") { start=today.minusDays((today.dayOfWeek.value-1).toLong());end=today;dates.text="From: $start • Through: $end" }
        val sales=store.sales()
        val yards=listOf("All yards")+sales.map { it.yard.trim() }.distinct().sorted()
        val yard=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,yards);box.addView(it) }
        yard.setSelection((yards.indexOf(filter.yard)).coerceAtLeast(0))
        val search=field(box,"Search yard or receipt/estimate ID",filter.query)
        val sorts=listOf("Newest first","Oldest first","Highest cash net","Highest payout")
        val sort=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,sorts);it.setSelection(filter.sort.ordinal);box.addView(it) }
        button(box,"Apply filters") {
            val next=SaleFilter(start,end,if(yard.selectedItemPosition==0) null else yard.selectedItem.toString(),search.text.toString(),SaleSort.values()[sort.selectedItemPosition])
            SalesAnalytics.filter(sales,next,ZoneId.systemDefault());filter=next;reports()
        }
        button(box,"Reset filters") { filter=SaleFilter();reports() }
        val selected=SalesAnalytics.filter(sales,filter,ZoneId.systemDefault())
        val scope="Applied dates: ${filter.start ?: "Any"} through ${filter.end ?: "Any"}\nYard: ${filter.yard ?: "All"} • Search: ${filter.query.ifBlank { "None" }}"
        label(box,scope+"\n\n"+earningsText(SalesAnalytics.totals(selected)))
        if(selected.isEmpty()) label(box,"No receipts match the applied filters.")
        else {
            button(box,"Export matching receipts as CSV") { pendingCsv=ExportSnapshot(this).save(LedgerTools.csvForSpreadsheet(selected));export.launch("Scrappy-filtered-sales.csv") }
            button(box,"Share matching earnings summary") { share("Scrappy earnings\n$scope\n${earningsText(SalesAnalytics.totals(selected))}\nUSD; recorded receipts only. Cash net excludes unentered costs and taxes. Modeled fuel is informational, already included only if entered in expenses.") }
        }
        SalePeriod.values().forEach { period ->
            label(box,"${period.name.lowercase().replaceFirstChar { it.uppercase() }} totals",20f)
            SalesAnalytics.periods(selected,period,ZoneId.systemDefault()).forEach { g -> label(box,"${g.label}\n${earningsText(g.totals)}") }
        }
        label(box,"By receiving yard (highest cash net first)",20f)
        SalesAnalytics.yards(selected).forEach { g -> label(box,"${g.label}\n${earningsText(g.totals)}") }
        label(box,"Matching receipts in selected order",20f)
        selected.forEach { s -> label(box,"${s.yard} • ${SalesAnalytics.localDate(s,ZoneId.systemDefault())}\nID: ${s.id}\nPayout: ${money(s.actualPayout)} • Cash net: ${money(s.cashNet)}") }
        button(box,"Tools home") { home() }
    }
    private fun loadText(d: LoadDraft): String {
        val t=TripLedger.totals(d);val q=ProfitPlanner.quotes(d,System.currentTimeMillis())
        return "Total: ${amount(t.pounds)} lb / ${amount(t.pounds/TripLedger.LB_PER_KG)} kg\nGross: ${money(t.gross)}\n"+
            "Fuel: ${money(t.fuel)} • Labor: ${money(t.labor)} • Fees: ${money(t.fees)}\nNet: ${money(t.net)}\n"+
            ProfitPlanner.metals(d).joinToString("\n") { "${it.metal}: ${amount(it.pounds)} lb • ${money(it.value)}" }+
            "\nUnconfirmed/demo prices: ${q.demoItems} • At least 24h old: ${q.staleItems} • Future-dated quotes: ${q.futureItems} • Sources: ${q.sources}\n"+
            d.items.joinToString("\n") { i ->
                val time=i.quoteTime
                val now=System.currentTimeMillis()
                val status=when { time==null -> "Demo/unconfirmed price";time>now -> "Future date; reconfirm";
                    now-time>=86_400_000L -> "At least 24h old; reconfirm";else -> "User-entered; confirm before sale" }
                "${i.metal}: ${money(i.pricePerLb)}/lb • ${i.source}\n${time?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(it)) } ?: "No quote date"} • $status"
            }+
            "\nUSD estimates from captured prices; confirm grades and receiving-yard quotes. Vehicle wear, taxes and unentered costs excluded."
    }
    private fun planner() {
        val box=page("Profit Targets & What-if Planner","planner")
        val d=store.draft();label(box,loadText(d))
        label(box,"Preview only. Price changes affect every grade equally. Labor and fees stay fixed. Required weight assumes the same metal mix and fixed trip costs. Distance limit includes fuel only; longer travel may increase labor. Unavailable means no usable weight/price, zero fuel cost, or target already exceeds the fuel-free net.")
        val price=field(box,"Hypothetical price change % (-100 to 100)","0",true,true)
        val fuel=field(box,"Hypothetical fuel price USD/gallon",amount(d.costs.fuelPerGallon),true)
        val miles=field(box,"Hypothetical round-trip miles",amount(d.costs.roundTripMiles),true)
        val target=field(box,"Desired net USD after modeled costs","0",true)
        val capacity=field(box,"Vehicle payload capacity lb (your vehicle's rating)","2000",true)
        val output=label(box,"")
        var preview: String?=null
        button(box,"Calculate preview") {
            val scenario=ProfitPlanner.scenario(d,number(price,-100.0,100.0),number(fuel),number(miles))
            val t=TripLedger.totals(scenario);val goals=ProfitPlanner.targets(scenario,number(target,max=1_000_000_000_000.0))
            val used=ProfitPlanner.capacity(scenario,number(capacity,0.0001,10_000_000.0))
            preview="Scrappy hypothetical scenario\nGross: ${money(t.gross)} • Net: ${money(t.net)}\nNet change: ${money(t.net-TripLedger.totals(d).net)}\n"+
                "Target net: ${money(number(target,max=1_000_000_000_000.0))}\nRequired payout: ${money(goals.requiredPayout)}\nRequired average USD/lb: ${optional(goals.averagePrice,true)}\n"+
                "Required weight at scenario mix/price: ${optional(goals.requiredPounds)} lb\nMaximum round-trip miles at fixed labor/fees: ${optional(goals.maxRoundTripMiles)}\n"+
                "Load/capacity: ${amount(used)}%${if(used>100) " • OVER ENTERED CAPACITY" else ""}\nPrice change: ${amount(number(price,-100.0,100.0))}%\nFuel USD/gallon: ${amount(number(fuel))}\nRound-trip miles: ${amount(number(miles))}\n"+
                "Hypothetical only; current ledger unchanged. Check your actual vehicle limits. Unentered costs and taxes excluded."
            output.text=preview
        }
        button(box,"Share calculated preview") { share(preview ?: error("Calculate a preview first")) }
        button(box,"Tools home") { home() }
    }
    private fun compareYards() {
        val box=page("Compare Receiving Yards","compare")
        val d=store.draft()
        val quotes=Prefs(this).priceQuotes()
        label(box,"Compare your current load using each yard's saved quotes. Enter round-trip miles and costs for each yard. Estimates include fuel, travel labor and fees. Missing grades receive no assumed price. Your load and receipts stay unchanged.")
        val yards=YardComparison.yards(quotes)
        if(d.items.isEmpty() || yards.isEmpty()) {
            label(box,if(d.items.isEmpty()) "Add metal in Mixed Loads, Trips & Sales first." else "Add yard quotes in Scrap Price Guide first.")
            button(box,"Tools home") { home() };return
        }
        val yard=Spinner(this).also { it.id=R.id.comparison_yard;it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,yards);box.addView(it) }
        val miles=field(box,"Round-trip miles",amount(d.costs.roundTripMiles),true).apply { id=R.id.comparison_miles }
        val mpg=field(box,"Vehicle MPG",amount(d.costs.mpg),true).apply { id=R.id.comparison_mpg }
        val fuel=field(box,"Fuel USD/gallon",amount(d.costs.fuelPerGallon),true).apply { id=R.id.comparison_fuel }
        val minutes=field(box,"Travel minutes",amount(d.costs.travelMinutes),true).apply { id=R.id.comparison_minutes }
        val labor=field(box,"Travel labor USD/hour",amount(d.costs.laborPerHour),true).apply { id=R.id.comparison_labor }
        val fees=field(box,"Yard/trip fees USD",amount(d.costs.fees),true).apply { id=R.id.comparison_fees }
        button(box,"Add or update selected yard") {
            val name=yard.selectedItem.toString()
            val trip=YardTrip(name,TripCosts(number(miles),number(mpg,0.0001,1000.0),number(fuel),number(minutes),number(labor),number(fees)))
            val next=comparisonTrips.filterNot { it.yard.equals(name,true) }+trip
            YardComparison.compare(d,quotes,next,System.currentTimeMillis())
            comparisonTrips.clear();comparisonTrips.addAll(next);compareYards()
        }
        if(comparisonTrips.isNotEmpty()) {
            val options=YardComparison.compare(d,quotes,comparisonTrips,System.currentTimeMillis())
            val report=YardComparison.report(options)
            label(box,report)
            button(box,"Share this yard comparison") { share(report) }
            comparisonTrips.toList().forEach { trip -> button(box,"Remove ${trip.yard} from comparison") {
                comparisonTrips.remove(trip);compareYards()
            } }
            button(box,"Clear comparison") { comparisonTrips.clear();compareYards() }
        } else label(box,"No yards added yet. Add each yard to see the ranked comparison.")
        button(box,"Tools home") { home() }
    }
    private fun loadTools() {
        val box=page("Current Load Tools","load")
        val d=store.draft();label(box,loadText(d))
        val factor=field(box,"Scale every item's weight by factor", "1",true)
        button(box,"Preview scaled load") {
            val next=LedgerTools.scale(d,number(factor,0.0001,1000.0))
            confirm("${loadText(next)}\n\nApply these weights? Original prices and trip costs remain.") { store.saveDraft(next);loadTools() }
        }
        if(d.items.isNotEmpty()) {
            val item=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,d.items.map { i -> "${i.metal} • ${amount(i.pounds)} lb • ${i.source}" });box.addView(it) }
            button(box,"Duplicate selected item") {
                val next=LedgerTools.copyItem(d,d.items[item.selectedItemPosition].id)
                confirm("Add another copy of this row with the same weight and captured quote?") { store.saveDraft(next);loadTools() }
            }
            button(box,"Preview repricing from my saved quotes") {
                val next=LedgerTools.reprice(d,Prefs(this).priceQuotes())
                confirm("${loadText(next)}\n\nReplace current draft prices? Saved estimates and receipts keep their historical values.") { store.saveDraft(next);loadTools() }
            }
            button(box,"Combine rows with identical quotes") {
                val next=LedgerTools.consolidate(d)
                confirm("Combine ${d.items.size} rows into ${next.items.size}? Only identical grade, price, source and quote time are combined.") { store.saveDraft(next);loadTools() }
            }
            button(box,"Sort rows by metal name") { store.saveDraft(LedgerTools.sorted(d));loadTools() }
        }
        button(box,"Clear trip costs") { confirm("Reset miles, fuel price, travel labor and fees? Vehicle MPG returns to 20.") { store.saveDraft(LedgerTools.clearCosts(d));loadTools() } }
        button(box,"Share current applied load") { share("Scrappy current load\n${loadText(d)}") }
        button(box,"Tools home") { home() }
    }
}
