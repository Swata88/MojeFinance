package cz.osobnifinance

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val ink=Color(0xFF142D42)
private val teal=Color(0xFF007E80)
private val pale=Color(0xFFF3F7FA)
private val red=Color(0xFFCB5263)
private val cz=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("cs-CZ"))
private fun money(x:Double)=cz.format(x)
private fun JSONObject.s(k:String)=optString(k,"")
private fun JSONObject.num(k:String)=optDouble(k,0.0)

class MainActivity:ComponentActivity(){
    private lateinit var data:JSONObject
    private fun load(){
        val prefs=getSharedPreferences("finance",MODE_PRIVATE)
        val raw=prefs.getString("data",null)?:assets.open("seed.json").bufferedReader().use{it.readText()}
        data=JSONObject(raw)
    }
    private fun save(){getSharedPreferences("finance",MODE_PRIVATE).edit().putString("data",data.toString()).apply()}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);load();setContent { FinanceUI(data,::save) }}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceUI(db:JSONObject,save:()->Unit){
    var tab by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    var year by remember { mutableStateOf("Vše") }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vše") }
    var kind by remember { mutableStateOf("Výdaje") }
    var edit by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val transactions=remember(version){db.getJSONArray("transactions").let{a->(0 until a.length()).map{a.getJSONObject(it)}}}
    val assets=remember(version){db.getJSONArray("assets").let{a->(0 until a.length()).map{a.getJSONObject(it)}}}
    val products=remember(version){db.getJSONArray("products").let{a->(0 until a.length()).map{a.getJSONObject(it)}}}
    val years=remember(version){transactions.map{it.s("date").take(4)}.distinct().sortedDescending()}
    val months=remember(version){assets.map{it.s("date")}.distinct().sortedDescending()}
    LaunchedEffect(months){if(month.isBlank())month=months.firstOrNull().orEmpty()}
    val filtered=remember(version,year,category,search,kind){transactions.filter{
        (year=="Vše"||it.s("date").startsWith(year)) && (category=="Vše"||it.s("c1")==category||it.s("c2")==category||it.s("c3")==category) && (search.isBlank()||listOf("c1","c2","c3","date").any{k->it.s(k).contains(search,true)}) && (kind=="Bilance"||if(kind=="Příjmy")it.s("kind")=="P" else it.s("kind")!="P")
    }}
    val categories=remember(version){transactions.flatMap{listOf(it.s("c1"),it.s("c2"),it.s("c3"))}.filter{it.isNotBlank()}.distinct().sorted()}
    val income=filtered.filter{it.s("kind")=="P"}.sumOf{it.num("amount")}
    val expense=filtered.filter{it.s("kind")!="P"}.sumOf{it.num("amount")}
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null){runCatching{val ctx=uri; /* URI provided by Android */
            // writing handled through context from composable
        }}
    }
    val context=androidx.compose.ui.platform.LocalContext.current
    val fileLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)runCatching{context.contentResolver.openOutputStream(uri)?.use{it.write(db.toString(2).toByteArray(Charsets.UTF_8))}}
    }
    val csvLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        if(uri!=null)runCatching{
            val header="datum;castka;kategorie_1;kategorie_2;kategorie_3;typ\n"
            val lines=transactions.joinToString("\n"){t->listOf("date","amount","c1","c2","c3","kind").joinToString(";"){k->"\""+t.optString(k).replace("\"","\"\"")+"\""}}
            context.contentResolver.openOutputStream(uri)?.use{it.write(("\uFEFF"+header+lines).toByteArray(Charsets.UTF_8))}
        }
    }
    MaterialTheme(colorScheme=lightColorScheme(primary=teal,background=pale,surface=Color.White,onSurface=ink)) {
        Scaffold(containerColor=pale,topBar={TopAppBar(title={Column {Text("Moje finance",fontWeight=FontWeight.Bold);Text(listOf("Přehled","Transakce","Analýzy","Majetek","Produkty","Nastavení")[tab],style=MaterialTheme.typography.labelSmall,color=teal)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=pale))},bottomBar={NavigationBar(containerColor=Color.White){listOf("Přehled","Položky","Grafy","Majetek","Produkty","Více").forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i;focus.clearFocus();keyboard?.hide()},icon={Icon(listOf(Icons.Default.Dashboard,Icons.Default.ReceiptLong,Icons.Default.BarChart,Icons.Default.AccountBalanceWallet,Icons.Default.Description,Icons.Default.Settings)[i],null)},label={Text(n,maxLines=1)})}}},floatingActionButton={if(tab==0||tab==1||tab==3||tab==4)FloatingActionButton(onClick={edit=when(tab){3->"asset";4->"product";else->"transaction"}},containerColor=teal,contentColor=Color.White){Icon(Icons.Default.Add,null)}}) { pad->
            Column(Modifier.fillMaxSize().padding(pad).clickable(indication=null,interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}){focus.clearFocus();keyboard?.hide()}){
                if(tab<=2){
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Choice(year,listOf("Vše")+years){year=it}
                        Choice(kind,listOf("Výdaje","Příjmy","Bilance")){kind=it}
                    }
                    Spacer(Modifier.height(8.dp))
                }
                when(tab){
                    0->{val total=if(kind=="Příjmy")income else if(kind=="Bilance")income-expense else expense
                        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            item {Summary("${kind} · ${year}",money(total),if(kind=="Výdaje")red else teal)}
                            item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.weight(1f)){Summary("Příjmy",money(if(kind=="Bilance")income else transactions.filter{year=="Vše"||it.s("date").startsWith(year)}.filter{it.s("kind")=="P"}.sumOf{it.num("amount")}),teal)};Box(Modifier.weight(1f)){Summary("Výdaje",money(if(kind=="Bilance")expense else transactions.filter{year=="Vše"||it.s("date").startsWith(year)}.filter{it.s("kind")!="P"}.sumOf{it.num("amount")}),red)}}}
                            item {Text("Největší kategorie",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
                            items(filtered.groupBy{it.s("c1")}.mapValues{it.value.sumOf{r->r.num("amount")}}.toList().sortedByDescending{it.second}.take(12)){(name,value)->CardLine(name,money(value),if(total!=0.0)(value/kotlin.math.abs(total)).toFloat().coerceIn(0f,1f) else 0f)}
                        }
                    }
                    1->{Column(Modifier.fillMaxSize()){
                        OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().padding(horizontal=14.dp),label={Text("Hledat popisek / kategorii")},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={keyboard?.hide();focus.clearFocus()}))
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(10.dp)){Choice(category,listOf("Vše")+categories){category=it}}
                        Text("${filtered.size} položek · ${money(filtered.sumOf{it.num("amount")})}",Modifier.padding(horizontal=16.dp),color=teal)
                        LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){items(filtered.asReversed().take(700)){t->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.s("c3").ifBlank{t.s("c2")},fontWeight=FontWeight.SemiBold);Text("${t.s("date")} · ${t.s("c1")} / ${t.s("c2")}",style=MaterialTheme.typography.bodySmall)};Text(money(t.num("amount")),color=if(t.s("kind")=="P")teal else red)}}}}
                        }
                    }
                    2->{val groups=filtered.groupBy{it.s("date").take(7)}.toSortedMap();val max=groups.values.maxOfOrNull{it.sumOf{r->r.num("amount")}}?:1.0
                        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                            item{Text("Vývoj po měsících",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
                            items(groups.toList().asReversed()){(m,rs)->CardLine(m,money(rs.sumOf{it.num("amount")}), (rs.sumOf{it.num("amount")}/max.coerceAtLeast(1.0)).toFloat().coerceIn(0f,1f))}
                        }
                    }
                    3->{val chosen=assets.filter{it.s("date")==month};val groups=chosen.groupBy{it.s("category")};val total=chosen.sumOf{it.num("amount")}
                        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                            item{Row(Modifier.horizontalScroll(rememberScrollState())){Choice(month,months){month=it}}}
                            item{Summary("Čisté jmění k $month",money(total),if(total>=0)teal else red)}
                            items(groups.toList().sortedBy{it.first}){(cat,records)->Card(colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth()){
                                Column(Modifier.fillMaxWidth().clickable{expanded=if(expanded==cat)"" else cat}.padding(16.dp)){
                                    Row(verticalAlignment=Alignment.CenterVertically){Text(cat,Modifier.weight(1f),fontWeight=FontWeight.Bold);Text(money(records.sumOf{it.num("amount")}),color=teal);Icon(if(expanded==cat)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)}
                                    if(expanded==cat){HorizontalDivider(Modifier.padding(vertical=8.dp));records.forEach{r->Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Text(r.s("product"),Modifier.weight(1f));Text(money(r.num("amount")))}}}
                                }
                            }}}
                        }
                    }
                    4->{LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                        item{Text("Evidence smluv a finančních produktů",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
                        items(products.sortedWith(compareBy({it.s("category")},{it.s("name")}))){p->Card(Modifier.fillMaxWidth().clickable{edit="product:${products.indexOf(p)}"},colors=CardDefaults.cardColors(containerColor=Color.White)){
                            Column(Modifier.padding(14.dp)){Text(p.s("name"),fontWeight=FontWeight.Bold);Text(p.s("category")+if(p.s("company").isNotBlank())" · ${p.s("company")}" else "",color=teal);if(p.s("validUntil").isNotBlank())Text("Platnost do ${p.s("validUntil")}",style=MaterialTheme.typography.bodySmall)}
                        }}
                    }}
                    5->{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                        Text("Data a zálohy",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Button(onClick={fileLauncher.launch("moje-finance-zaloha.json")},Modifier.fillMaxWidth()){Text("Úplná záloha JSON")}
                        OutlinedButton(onClick={csvLauncher.launch("moje-finance-transakce.csv")},Modifier.fillMaxWidth()){Text("Export transakcí CSV")}
                        Text("Zálohu ukládej do Google Drive výběrem umístění při ukládání. Automatické zálohování zatím není implementováno.",color=Color.DarkGray)
                        Text("Data jsou ukládána lokálně v telefonu. Před přeinstalací aplikace proveď export zálohy.",style=MaterialTheme.typography.bodySmall)
                    }}
                }
            }
        }
        if(edit.isNotBlank()){
            val mode=edit
            val index=mode.substringAfter(":", "-1").toIntOrNull()?:-1
            val old=if(index>=0 && mode.startsWith("product"))products.getOrNull(index) else null
            var d by remember(mode){mutableStateOf(LocalDate.now().toString())}
            var value by remember(mode){mutableStateOf("")}
            var c1 by remember(mode){mutableStateOf("")}
            var c2 by remember(mode){mutableStateOf("")}
            var c3 by remember(mode){mutableStateOf("")}
            var typ by remember(mode){mutableStateOf("V")}
            var company by remember(mode){mutableStateOf(old?.s("company")?:"")}
            var number by remember(mode){mutableStateOf(old?.s("number")?:"")}
            var until by remember(mode){mutableStateOf(old?.s("validUntil")?:"")}
            var note by remember(mode){mutableStateOf(old?.s("note")?:"")}
            var name by remember(mode){mutableStateOf(old?.s("name")?:"")}
            var status by remember(mode){mutableStateOf(old?.s("status")?:"Aktivní")}
            var error by remember(mode){mutableStateOf("")}
            AlertDialog(onDismissRequest={keyboard?.hide();focus.clearFocus();edit=""},title={Text(when{mode.startsWith("product")->"Produkt / smlouva";mode=="asset"->"Měsíční stav majetku";else->"Nová transakce"})},text={Column(Modifier.fillMaxWidth().heightIn(max=470.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
                @Composable fun Field(v:String,change:(String)->Unit,label:String,numeric:Boolean=false){OutlinedTextField(v,change,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={keyboard?.hide();focus.clearFocus()}))}
                if(mode.startsWith("product")){
                    Field(name,{name=it},"Název produktu");Field(c1.ifBlank{old?.s("category")?:""},{c1=it},"Kategorie");Field(company,{company=it},"Společnost");Field(number,{number=it},"Číslo smlouvy");Field(until,{until=it},"Platnost do (RRRR-MM-DD)");Field(status,{status=it},"Stav");Field(note,{note=it},"Poznámka")
                }else{
                    Field(d,{d=it},"Datum (RRRR-MM-DD)");Field(value,{value=it},"Částka v Kč",true);Field(c1,{c1=it},"Kategorie I");Field(c2,{c2=it},if(mode=="asset")"Produkt" else "Kategorie II");if(mode=="transaction"){Field(c3,{c3=it},"Kategorie III / popisek");Row{Choice(typ,listOf("V","P")){typ=it}}}
                }
                if(error.isNotBlank())Text(error,color=red)
            }},confirmButton={Button(onClick={
                val obj=JSONObject()
                when{
                    mode.startsWith("product")->{if(name.isBlank()){error="Vyplň název";return@Button};obj.put("name",name).put("category",c1.ifBlank{old?.s("category")?:"Ostatní"}).put("company",company).put("number",number).put("validUntil",until).put("note",note).put("status",status);if(index>=0)db.getJSONArray("products").put(index,obj) else db.getJSONArray("products").put(obj)}
                    else->{val parsed=value.replace(',','.').toDoubleOrNull();if(parsed==null||runCatching{LocalDate.parse(d)}.isFailure){error="Zkontroluj datum a částku";return@Button};if(mode=="asset"){
                        if(c1.isBlank()||c2.isBlank()){error="Vyplň kategorii a produkt";return@Button};obj.put("date",d).put("amount",parsed).put("category",c1).put("product",c2);db.getJSONArray("assets").put(obj);month=d
                    }else{obj.put("date",d).put("amount",parsed).put("c1",c1).put("c2",c2).put("c3",c3).put("kind",typ);db.getJSONArray("transactions").put(obj)}}
                }
                save();version++;keyboard?.hide();focus.clearFocus();edit=""
            }){Text("Uložit")}},dismissButton={TextButton(onClick={keyboard?.hide();focus.clearFocus();edit=""}){Text("Zrušit")}})
        }
    }
}

@Composable private fun Choice(value:String,choices:List<String>,onChange:(String)->Unit){var open by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={open=true},contentPadding=PaddingValues(horizontal=12.dp,vertical=5.dp)){Text(value,maxLines=1);Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(expanded=open,onDismissRequest={open=false},modifier=Modifier.heightIn(max=370.dp)){choices.forEach{DropdownMenuItem(text={Text(it)},onClick={onChange(it);open=false})}}}}
@Composable private fun Summary(label:String,value:String,color:Color){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(18.dp)){Text(label,color=Color.Gray,style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(7.dp));Text(value,color=color,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}}
@Composable private fun CardLine(label:String,value:String,progress:Float){Card(colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(13.dp)){Row{Text(label,Modifier.weight(1f),fontWeight=FontWeight.Medium);Text(value,color=teal)};Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(7.dp),color=teal,trackColor=pale)}}}
