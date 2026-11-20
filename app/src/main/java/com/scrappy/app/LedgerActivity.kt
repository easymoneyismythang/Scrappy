package com.scrappy.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.scrappy.app.data.*
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class LedgerActivity : AppCompatActivity() {
    private lateinit var store: LedgerStore
    private lateinit var prefs: Prefs
    private var draft=LoadDraft()
    private val workbench=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        guarded { draft=store.draft();showDraft() }
    }
    private var pendingCsv: String?=null
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val token=pendingCsv;pendingCsv=null
        if(token!=null) guarded { try { if(uri!=null) {
            val csv=ExportSnapshot(this).read(token)
            val stream=contentResolver.openOutputStream(uri,"wt") ?: error("Could not open export file")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            toast("Sales CSV exported")
        } } finally { ExportSnapshot(this).remove(token) } }
    }
    private var pendingBackup: String? = null
    private val backupExport = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val token = pendingBackup; pendingBackup = null
        if (token != null) guarded { try { if (uri != null) {
            val raw=ExportSnapshot(this).read(token)
            val stream = contentResolver.openOutputStream(uri,"wt") ?: error("Could not open backup file")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(raw) }
            toast("Ledger backup saved")
        } } finally { ExportSnapshot(this).remove(token) } }
    }
    private val backupImport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) guarded {
            val stream = contentResolver.openInputStream(uri) ?: error("Could not open backup")
            val bytes = stream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= LedgerBackup.MAX_BYTES) { "Backup is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= LedgerBackup.MAX_BYTES) { "Backup is too large" }
            val incoming = LedgerBackup.decode(LedgerTools.strictUtf8(bytes))
            chooseImport(incoming)
        }
    }
    private fun chooseImport(incoming: LedgerSnapshot) {
        AlertDialog.Builder(this).setTitle("Import ${incoming.estimates.size} estimates and ${incoming.sales.size} sales")
            .setItems(arrayOf("Merge records; keep current load", "Merge records; use imported load", "Replace entire ledger")) { _,mode -> guarded {
                val current = store.snapshot()
                if (mode == 2) {
                    confirm("Replace ${current.draft.items.size} load items, ${current.estimates.size} estimates and ${current.sales.size} sales with this backup? Export a backup first. A local pre-import recovery checkpoint will also be kept.") {
                        store.restore(incoming, keepCheckpoint = true);draft=store.draft();toast("Ledger replaced");showDraft()
                    }
                } else {
                    val preview = LedgerTools.merge(current,incoming,mode == 1)
                    confirm("Add ${preview.addedEstimates} estimates and ${preview.addedSales} sales; skip ${preview.skippedEstimates} identical estimates and ${preview.skippedSales} identical sales. ${if(mode == 1) "Replace current load with imported load." else "Keep current load."} A pre-import checkpoint will be kept.") {
                        store.restore(preview.snapshot, keepCheckpoint = true);draft=store.draft();toast("Ledger merged");showDraft()
                    }
                }
            } }.setNegativeButton("Cancel",null).show()
    }
    private fun showBackup() {
        val box = page("Ledger Backup & Restore")
        label(box,"Save a JSON file to recover your current load, saved estimates and sale receipts on another phone. Keep it somewhere you can find later. Backups contain financial information and yard names; they are not encrypted. Photos, contacts, price quotes and settings are not included.")
        button(box,"Export ledger backup") {
            pendingBackup = ExportSnapshot(this).save(LedgerBackup.encode(store.snapshot()))
            backupExport.launch("Scrappy-ledger-backup.json")
        }
        button(box,"Choose backup to restore") { backupImport.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
        if(store.hasImportCheckpoint()) button(box,"Recover ledger from before last import") {
            confirm("Replace the current ledger with its pre-import checkpoint? This will discard changes made after that import. Export a backup first. The current ledger becomes the new recovery checkpoint.") {
                store.recoverBeforeImport();draft=store.draft();showDraft()
            }
        }
        button(box,"Current load") { showDraft() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);store=LedgerStore(this);prefs=Prefs(this)
        pendingCsv=savedInstanceState?.getString("pendingCsv")
        pendingBackup=savedInstanceState?.getString("pendingBackup")
        val recovery=page("Scrappy Loads & Sales");button(recovery,"Back to home") { finish() }
        guarded { draft=store.draft();showDraft() }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pendingCsv",pendingCsv);outState.putString("pendingBackup",pendingBackup);super.onSaveInstanceState(outState) }
    override fun setContentView(view: View?) { super.setContentView(view);view?.let { applyScreenInsets(it) } }
    private fun guarded(action: () -> Unit) {
        try { action() } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Unable to complete action")
                .setMessage(e.message ?: "Saved data could not be read. Your records have been retained.")
                .setPositiveButton("OK",null).show()
        }
    }
    private fun page(title: String): LinearLayout {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(28,32,28,28) }
        val scroll=ScrollView(this);scroll.addView(box);setContentView(scroll);label(box,title,25f);return box
    }
    private fun label(box: LinearLayout, value: String, size: Float=17f) = TextView(this).also {
        it.text=value;it.textSize=size;it.setPadding(0,10,0,10);box.addView(it)
    }
    private fun button(box: LinearLayout, value: String, action: () -> Unit) = Button(this).also {
        it.text=value;it.setOnClickListener { guarded(action) };box.addView(it)
    }
    private fun input(box: LinearLayout, title: String, value: String="", numeric: Boolean=false): EditText {
        label(box,title,15f)
        return EditText(this).also {
            it.hint=title;it.setText(value);it.contentDescription=title
            it.inputType=if(numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT
            if(!numeric) it.filters=arrayOf(android.text.InputFilter.LengthFilter(120))
            box.addView(it)
        }
    }
    private fun number(field: EditText, max: Double=1_000_000.0, positive: Boolean=false): Double {
        val n=field.text.toString().trim().toDoubleOrNull()
        if(n==null || !n.isFinite() || n<0 || n>max || (positive && n==0.0)) {
            field.error="Enter ${if(positive) "more than 0" else "0 or more"}, up to $max";field.requestFocus()
            error("Check the highlighted value")
        }
        field.error=null;return n
    }
    private fun name(field: EditText): String = field.text.toString().trim().also {
        if(it.isBlank()) { field.error="Required";field.requestFocus();error("Enter a name") }
    }
    private fun money(n: Double)=String.format(Locale.US,"$%.2f",n)
    private fun amount(n: Double)=String.format(Locale.US,"%.4f",n).trimEnd('0').trimEnd('.')
    private fun date(n: Long)=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(n))
    private fun toast(s: String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun confirm(message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton("Continue") { _,_ -> guarded(action) }
            .setNegativeButton("Cancel",null).show()
    }
    private fun saveDraft(next: LoadDraft) { store.saveDraft(next);draft=next }
    private fun report(d: LoadDraft): String {
        val t=TripLedger.totals(d)
        val sources=d.items.joinToString("\n") { i ->
            val stale=i.quoteTime?.let { it>System.currentTimeMillis() || System.currentTimeMillis()-it>=86_400_000L } ?: false
            "${i.metal}: ${amount(i.pounds)} lb × ${money(i.pricePerLb)}/lb = ${money(i.value)}\n"+
                "${i.source}"+(i.quoteTime?.let { " • ${date(it)}" } ?: " • confirm with yard")+
                (if(stale) " • Reconfirm quote" else "")
        }
        return "$sources\n\nTotal weight: ${amount(t.pounds)} lb\nGross estimate: ${money(t.gross)}\nFuel: ${money(t.fuel)}\nTravel labor: ${money(t.labor)}\nFees: ${money(t.fees)}\nNet after modeled costs: ${money(t.net)}\nBreak-even payout: ${money(t.breakEvenPayout)}\n\nUSD estimates. Demo benchmarks and user-entered quotes are not live prices. Verify exact grades and receiving-yard prices. Vehicle wear, taxes and processing labor are not included."
    }
    private fun showDraft() {
        val box=page("Mixed Load & Trip Planner")
        label(box,"Add metal grades to one load. Added items and applied costs are saved on this device. Tap Apply trip costs to save cost edits. New items capture the current quote; existing items keep their original prices.")
        button(box,"Add metal to load") { showItem() }
        draft.items.forEach { i ->
            label(box,"${i.metal} • ${amount(i.pounds)} lb • ${money(i.value)}\n${i.source} @ ${money(i.pricePerLb)}/lb")
            button(box,"Edit ${i.metal} weight") { showItem(i) }
            button(box,"Remove ${i.metal}") { confirm("Remove this item?") { saveDraft(draft.copy(items=draft.items.filter { it.id!=i.id }));showDraft() } }
        }
        if(draft.items.isEmpty()) label(box,"No metal items yet.")
        val c=draft.costs
        val miles=input(box,"Round-trip miles (include return trip)",amount(c.roundTripMiles),true)
        val mpg=input(box,"Vehicle miles per gallon",amount(c.mpg),true)
        val fuel=input(box,"Fuel price USD/gallon",amount(c.fuelPerGallon),true)
        val minutes=input(box,"Total travel minutes",amount(c.travelMinutes),true)
        val labor=input(box,"Travel labor USD/hour",amount(c.laborPerHour),true)
        val fees=input(box,"Other trip fees USD",amount(c.fees),true)
        button(box,"Apply trip costs") {
            saveDraft(draft.copy(costs=TripCosts(number(miles),number(mpg,1000.0,true),number(fuel),number(minutes),number(labor),number(fees))))
            showDraft()
        }
        label(box,report(draft))
        val title=input(box,"Estimate name")
        button(box,"Save applied load as estimate") {
            require(draft.items.isNotEmpty()) { "Add at least one metal item" }
            store.saveEstimate(LoadEstimate(UUID.randomUUID().toString(),name(title),System.currentTimeMillis(),draft))
            toast("Estimate saved with applied costs and captured prices");showEstimates()
        }
        button(box,"Saved estimates") { showEstimates() }
        button(box,"Sales & earnings") { showSales() }
        button(box,"Earnings reports, profit targets & load tools") { workbench.launch(android.content.Intent(this,WorkbenchActivity::class.java)) }
        button(box,"Backup & restore ledger") { showBackup() }
        button(box,"Clear current load") { confirm("Clear the current draft and trip costs? Saved estimates and sales remain.") { saveDraft(LoadDraft());showDraft() } }
        button(box,"Back to home") { finish() }
    }
    private fun showItem(existing: LoadItem?=null) {
        val box=page(if(existing==null) "Add Metal" else "Edit Weight")
        val metal=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,ScrappyRepository.metals.map { m -> m.name });box.addView(it) }
        if(existing!=null) { metal.setSelection(ScrappyRepository.metals.indexOfFirst { it.name==existing.metal });metal.isEnabled=false }
        val unit=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("lb","kg"));box.addView(it) }
        val weight=input(box,"Weight in selected unit",existing?.pounds?.toString() ?: "",true)
        var currentUnit="lb"
        unit.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val next=unit.selectedItem.toString()
                if(next!=currentUnit) {
                    val value=weight.text.toString().toDoubleOrNull()
                    if(value!=null && value.isFinite() && value>=0) weight.setText((if(next=="kg") value/TripLedger.LB_PER_KG else value*TripLedger.LB_PER_KG).toString())
                    currentUnit=next
                }
            }
        }
        if(existing!=null) label(box,"Changing weight retains this item's original ${money(existing.pricePerLb)}/lb quote from ${existing.source}. Remove and add it again to capture a newer quote.")
        button(box,"Save item") {
            val w=number(weight,positive=true);val u=unit.selectedItem.toString()
            val i=existing?.copy(pounds=TripLedger.pounds(w,u)) ?: TripLedger.item(metal.selectedItem.toString(),w,u,prefs.priceQuotes())
            val items=if(existing==null) draft.items+i else draft.items.map { if(it.id==i.id) i else it }
            saveDraft(draft.copy(items=items));showDraft()
        }
        button(box,"Cancel") { showDraft() }
    }
    private fun showEstimates() {
        val box=page("Saved Load Estimates")
        val values=store.estimates()
        label(box,"${values.size}/200 saved estimates")
        if(store.canUndo()) button(box,"Undo last record deletion") { store.undoDelete();showEstimates() }
        if(values.isEmpty()) label(box,"No estimates saved yet.")
        values.forEach { e ->
            label(box,"${e.name} • ${date(e.savedAt)}\n${report(e.draft)}")
            button(box,"Reopen ${e.name}") { confirm("Replace the current draft with this saved estimate? Original quote snapshots will be kept.") { saveDraft(e.draft);showDraft() } }
            button(box,"Record sale for ${e.name}") { showReceipt(e) }
            button(box,"Share ${e.name}") { share("Scrappy estimate: ${e.name}\nSaved ${date(e.savedAt)}\n${report(e.draft)}") }
            button(box,"Rename ${e.name}") {
                val edit=EditText(this).apply { setText(e.name);filters=arrayOf(android.text.InputFilter.LengthFilter(120)) }
                AlertDialog.Builder(this).setTitle("Rename estimate").setView(edit).setPositiveButton("Save") { _,_ -> guarded {
                    store.updateEstimate(LedgerTools.rename(e,name(edit)));showEstimates()
                } }.setNegativeButton("Cancel",null).show()
            }
            button(box,"Duplicate ${e.name}") {
                val edit=EditText(this).apply { setText(e.name.take(113)+" (copy)");filters=arrayOf(android.text.InputFilter.LengthFilter(120)) }
                AlertDialog.Builder(this).setTitle("Name the copy").setView(edit).setPositiveButton("Save copy") { _,_ -> guarded {
                    store.saveEstimate(LedgerTools.duplicate(e,name(edit),System.currentTimeMillis()));showEstimates()
                } }.setNegativeButton("Cancel",null).show()
            }
            button(box,"Delete estimate") { confirm("Delete ${e.name}? Linked sale records retain their recorded totals.") { store.removeEstimate(e.id);showEstimates() } }
        }
        button(box,"Current load") { showDraft() };button(box,"Sales & earnings") { showSales() }
    }
    private fun showReceipt(e: LoadEstimate) {
        val box=page("Record Actual Sale")
        label(box,"${e.name} • Estimated gross ${money(TripLedger.totals(e.draft).gross)}. Enter the payout and actual cash expenses from this sale. One estimate can have multiple receipts; totals count each receipt.")
        val yard=input(box,"Receiving scrapyard")
        val contacts=prefs.contacts()
        if(contacts.isNotEmpty()) {
            val choose=Spinner(this).also { it.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,contacts.map { c -> c.name });box.addView(it) }
            button(box,"Use contact name") { yard.setText(contacts[choose.selectedItemPosition].name) }
        }
        val payout=input(box,"Actual payout USD",numeric=true)
        val expenses=input(box,"Actual cash expenses USD (include actual fuel)",numeric=true)
        val actualMiles=input(box,"Actual round-trip miles",amount(e.draft.costs.roundTripMiles),true)
        label(box,"Recorded miles are kept with the receipt. Fuel is modeled using this estimate’s MPG and gas price; enter actual fuel spending within cash expenses so it is deducted once.")
        var soldAt=System.currentTimeMillis()
        val selected=label(box,"Sale date: ${date(soldAt)}")
        button(box,"Change sale date") {
            val c=Calendar.getInstance().apply { timeInMillis=soldAt }
            DatePickerDialog(this,{ _,y,m,d ->
                val picked=Calendar.getInstance().apply { set(y,m,d,12,0,0);set(Calendar.MILLISECOND,0) }
                soldAt=picked.timeInMillis.coerceAtMost(System.currentTimeMillis());selected.text="Sale date: ${date(soldAt)}"
            },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).apply { datePicker.maxDate=System.currentTimeMillis() }.show()
        }
        button(box,"Save sale record") {
            require(soldAt<=System.currentTimeMillis()) { "Sale date cannot be in the future" }
            store.saveSale(TripLedger.receipt(e,name(yard),number(payout,1_000_000_000_000.0),number(expenses,1_000_000_000_000.0),soldAt,number(actualMiles)))
            showSales()
        }
        button(box,"Cancel") { showEstimates() }
    }
    private fun editSale(s: SaleRecord) {
        val box=page("Correct Sale Receipt")
        label(box,"This updates the existing receipt without adding a second sale. Original estimate gross and labor are retained. Cash expenses include actual fuel; modeled fuel is not deducted separately. If original mileage was zero, revised modeled fuel stays zero because no per-mile rate was stored.")
        val yard=input(box,"Receiving yard",s.yard)
        val payout=input(box,"Payout USD",s.actualPayout.toString(),true)
        val expenses=input(box,"Cash expenses USD",s.cashExpenses.toString(),true)
        val miles=input(box,"Round-trip miles",s.roundTripMiles.toString(),true)
        var soldAt=s.soldAt
        val selected=label(box,"Sale date: ${date(soldAt)}")
        button(box,"Change sale date") {
            val c=Calendar.getInstance().apply { timeInMillis=soldAt }
            DatePickerDialog(this,{ _,y,m,d ->
                soldAt=Calendar.getInstance().apply { set(y,m,d,12,0,0);set(Calendar.MILLISECOND,0) }.timeInMillis.coerceAtMost(System.currentTimeMillis())
                selected.text="Sale date: ${date(soldAt)}"
            },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).apply { datePicker.maxDate=System.currentTimeMillis() }.show()
        }
        button(box,"Review correction") {
            val next=LedgerTools.correct(s,name(yard),number(payout,1_000_000_000_000.0),number(expenses,1_000_000_000_000.0),number(miles),soldAt,System.currentTimeMillis())
            confirm("Replace this receipt with ${next.yard}, ${date(next.soldAt)}, payout ${money(next.actualPayout)}, expenses ${money(next.cashExpenses)} and ${amount(next.roundTripMiles)} miles? Cash net: ${money(next.cashNet)}.") {
                store.updateSale(next);showSales()
            }
        }
        button(box,"Cancel") { showSales() }
    }
    private fun showSales() {
        val box=page("Sales & Earnings")
        val sales=store.sales().sortedByDescending { it.soldAt }
        label(box,"${sales.size}/200 saved receipts")
        if(store.canUndo()) button(box,"Undo last record deletion") { store.undoDelete();showSales() }
        button(box,"Date filters, yard reports & CSV exports") { workbench.launch(android.content.Intent(this,WorkbenchActivity::class.java)) }
        val start=Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0) }.timeInMillis
        val now=System.currentTimeMillis()
        fun summary(title: String, items: List<SaleRecord>) {
            label(box,"$title • ${items.size} receipts\nRecorded miles: ${amount(items.sumOf { it.roundTripMiles })}\nModeled fuel: ${money(items.sumOf { it.modeledFuel })}\nPayout: ${money(items.sumOf { it.actualPayout })}\nCash expenses: ${money(items.sumOf { it.cashExpenses })}\nCash net: ${money(items.sumOf { it.cashNet })}\nAfter estimated travel labor: ${money(items.sumOf { it.afterLabor })}")
        }
        summary("This month (device local date)",sales.filter { it.soldAt in start..now })
        summary("All recorded sales",sales)
        label(box,"Cash net is payout minus entered cash expenses. The labor adjustment uses the original estimate's travel labor; do not include labor again in cash expenses. These figures exclude unentered costs and taxes.")
        if(sales.isEmpty()) label(box,"Record a sale from a saved estimate to begin.")
        else button(box,"Export all sale records as CSV") { pendingCsv=ExportSnapshot(this).save(LedgerTools.csvForSpreadsheet(sales));export.launch("Scrappy-sales.csv") }
        sales.forEach { s ->
            label(box,"${s.yard} • ${date(s.soldAt)}\nMiles: ${amount(s.roundTripMiles)} • Modeled fuel: ${money(s.modeledFuel)}\nPayout: ${money(s.actualPayout)} • Expenses: ${money(s.cashExpenses)}\nCash net: ${money(s.cashNet)}\nAfter estimated labor: ${money(s.afterLabor)}\nPayout vs estimate: ${money(s.payoutDifference)}")
            button(box,"Correct receipt") { editSale(s) }
            button(box,"Share receipt") { share("Scrappy sale receipt ${s.id}\n${s.yard} • ${date(s.soldAt)}\nPayout ${money(s.actualPayout)} • Expenses ${money(s.cashExpenses)}\nCash net ${money(s.cashNet)} • After estimated labor ${money(s.afterLabor)}\nMiles ${amount(s.roundTripMiles)} • Modeled fuel ${money(s.modeledFuel)}\nEstimated gross ${money(s.estimatedGross)} • Difference ${money(s.payoutDifference)}\nUSD. Cash net excludes unentered costs and taxes; modeled fuel is informational.") }
            button(box,"Delete sale record") { confirm("Delete this ${money(s.actualPayout)} receipt from ${s.yard}? Export first if you need a copy.") { store.removeSale(s.id);showSales() } }
        }
        button(box,"Saved estimates") { showEstimates() };button(box,"Current load") { showDraft() }
    }
    private fun share(value: String) {
        startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type="text/plain";putExtra(android.content.Intent.EXTRA_TEXT,value)
        },"Share Scrappy estimate"))
    }
}
