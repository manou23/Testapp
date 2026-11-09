package com.mesut23.aistudio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import java.io.File

private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private val BRAND_GRADIENT = Brush.linearGradient(listOf(Color(0xFF9B6DFF), Color(0xFF3978F6)))

@Composable
fun MesutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9B6DFF),
            secondary = Color(0xFF55A7FF),
            background = Color(0xFF080D1C),
            surface = Color(0xFF111A31),
            surfaceVariant = Color(0xFF1C2948)
        ),
        content = content
    )
}

@Composable
fun Root(act: Activity, vm: AppVM = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var subScreen by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { vm.refresh() }
    BackHandler(enabled = subScreen != null) { subScreen = null }

    val tabs = listOf(
        Icons.Filled.Home to R.string.tab_home,
        Icons.Filled.Create to R.string.tab_generate,
        Icons.Filled.Edit to R.string.tab_edit,
        Icons.Filled.Collections to R.string.tab_creations,
        Icons.Filled.Settings to R.string.tab_settings
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index && subScreen == null,
                        onClick = { selectedTab = index; subScreen = null },
                        icon = { Icon(item.first, contentDescription = null) },
                        label = { Text(stringResource(item.second)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val back: () -> Unit = { subScreen = null }
            when (subScreen) {
                "styles" -> StylesLibrary(vm, back) { selectedTab = 1; subScreen = null }
                "favorites" -> Favorites(vm, back)
                "premium" -> PremiumScreen(vm, back)
                "about" -> About(back)
                else -> when (selectedTab) {
                    0 -> Home(vm) { subScreen = it }
                    1 -> Generator(vm)
                    2 -> Editor(vm)
                    3 -> Creations(vm)
                    else -> SettingsScreen(vm) { subScreen = it }
                }
            }
        }
    }
}

@Composable
private fun Home(vm: AppVM, navigate: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(brush = BRAND_GRADIENT),
            fontWeight = FontWeight.ExtraBold
        )
        Text("Create • Imagine • Transform", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) {
                Text(stringResource(R.string.credits_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (vm.credits < 0) "—" else vm.credits.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HomeAction(stringResource(R.string.tab_generate), "✦", Modifier.fillMaxWidth()) { navigate("styles"); }
        HomeAction(stringResource(R.string.favorites_title), "♡", Modifier.fillMaxWidth()) { navigate("favorites") }
        HomeAction(stringResource(R.string.premium_title), "◇", Modifier.fillMaxWidth()) { navigate("premium") }
        if (vm.error != 0) Text(stringResource(vm.error), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(6.dp))
        Text("Your creative workspace", style = MaterialTheme.typography.titleMedium)
        if (!vm.premium) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = TEST_BANNER_ID
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HomeAction(label: String, symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun Generator(vm: AppVM) {
    var prompt by remember { mutableStateOf("") }
    var ratioIndex by remember { mutableIntStateOf(0) }
    val styleNames = stringArrayResource(R.array.style_names)
    LaunchedEffect(Unit) { vm.clear() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.tab_generate), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prompt_hint)) },
            minLines = 3,
            maxLines = 6
        )
        Text("Aspect ratio", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(RATIOS) { index, item ->
                FilterChip(selected = ratioIndex == index, onClick = { ratioIndex = index }, label = { Text(item.first) })
            }
        }
        Text("Style", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(styleNames.toList()) { index, name ->
                FilterChip(selected = vm.style == index, onClick = { vm.style = index }, label = { Text(name) })
            }
        }
        Button(
            onClick = { vm.generate(prompt, vm.style, RATIOS[ratioIndex].second) },
            enabled = prompt.isNotBlank() && !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.generate))
        }
        ResultPanel(vm)
    }
}

@Composable
fun Editor(vm: AppVM) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var instruction by remember { mutableStateOf("") }
    var showConsent by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        selectedUri = it
    }
    LaunchedEffect(Unit) { vm.clear() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tab_edit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.pick_photo)) }
        selectedUri?.let { uri ->
            AsyncImage(uri, null, Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
        }
        OutlinedTextField(
            value = instruction,
            onValueChange = { instruction = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.edit_hint)) },
            minLines = 2
        )
        Button(
            onClick = { showConsent = true },
            enabled = selectedUri != null && instruction.isNotBlank() && !vm.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.apply)) }
        ResultPanel(vm)
    }

    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            title = { Text(stringResource(R.string.consent_title)) },
            text = { Text(stringResource(R.string.consent_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showConsent = false
                    selectedUri?.let { vm.edit(it, instruction) }
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = {
                TextButton(onClick = { showConsent = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ColumnScope.ResultPanel(vm: AppVM) {
    if (vm.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    if (vm.error != 0) Text(stringResource(vm.error), color = MaterialTheme.colorScheme.error)
    vm.result?.let { file ->
        AsyncImage(file, null, Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
        ImageActions(vm, file)
    }
}

@Composable
private fun ImageActions(vm: AppVM, file: File) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = {
                val saved = vm.saveToGallery(file)
                Toast.makeText(context, if (saved) R.string.saved else R.string.error_generic, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.save)) }
        OutlinedButton(onClick = { shareImage(context, file) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.share))
        }
        IconButton(onClick = { vm.toggleFav(file) }) {
            Icon(
                if (file.name in vm.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.favorites_title)
            )
        }
    }
}

private fun shareImage(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}

@Composable
fun Creations(vm: AppVM) {
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.tab_creations), Modifier.padding(18.dp), style = MaterialTheme.typography.headlineMedium)
        Gallery(vm, vm.files)
    }
}

@Composable
private fun Gallery(vm: AppVM, images: List<File>) {
    val context = LocalContext.current
    if (images.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.favorites_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(images, key = { it.name }) { file ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Box {
                        AsyncImage(file, null, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                        Row(
                            Modifier.align(Alignment.TopEnd).background(Color(0x99070B17), RoundedCornerShape(bottomStart = 12.dp))
                        ) {
                            IconButton(onClick = { vm.toggleFav(file) }, modifier = Modifier.size(38.dp)) {
                                Icon(
                                    if (file.name in vm.favorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = { shareImage(context, file) }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                            }
                            IconButton(onClick = { vm.delete(file) }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScreen(titleId: Int, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text(stringResource(titleId), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
fun Favorites(vm: AppVM, onBack: () -> Unit) {
    SubScreen(R.string.favorites_title, onBack) {
        Gallery(vm, vm.files.filter { it.name in vm.favorites })
    }
}

@Composable
fun StylesLibrary(vm: AppVM, onBack: () -> Unit, onSelect: () -> Unit) {
    SubScreen(R.string.styles_title, onBack) {
        val names = stringArrayResource(R.array.style_names)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(names.size) { index ->
                Box(
                    Modifier.fillMaxWidth().height(116.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(
                            Color.hsv((index * 37f) % 360f, 0.62f, 0.82f),
                            Color(0xFF172341)
                        )))
                        .clickable { vm.style = index; onSelect() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(names[index], Modifier.padding(10.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun PremiumScreen(vm: AppVM, onBack: () -> Unit) {
    SubScreen(R.string.premium_title, onBack) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.premium_title),
                style = MaterialTheme.typography.headlineLarge.copy(brush = BRAND_GRADIENT),
                fontWeight = FontWeight.Bold
            )
            listOf(R.string.premium_b1, R.string.premium_b2, R.string.premium_b3).forEach {
                Text("✓  " + stringResource(it), style = MaterialTheme.typography.bodyLarge)
            }
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.premium_cta))
            }
            Text(stringResource(R.string.premium_note), style = MaterialTheme.typography.bodySmall)
            if (BuildConfig.DEBUG) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = vm.premium, onCheckedChange = vm::savePremium)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.premium_debug), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AppLogo(size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 4).dp)).background(BRAND_GRADIENT),
        contentAlignment = Alignment.Center
    ) {
        Text("M", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
fun About(onBack: () -> Unit) {
    SubScreen(R.string.about_title, onBack) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLogo(84)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_desc))
            Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Splash() {
    Box(Modifier.fillMaxSize().background(Color(0xFF080D1C)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppLogo(100)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium.copy(brush = BRAND_GRADIENT), fontWeight = FontWeight.Bold)
            Text("Mesut23", color = Color.LightGray)
        }
    }
}

@Composable
fun SettingsScreen(vm: AppVM, navigate: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        listOf("ar" to "العربية", "fr" to "Français", "en" to "English").forEach { (tag, label) ->
            OutlinedButton(
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(label) }
        }
        HorizontalDivider()
        OutlinedButton(onClick = { vm.deleteAll() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_all))
        }
        OutlinedButton(onClick = { navigate("about") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.about_title))
        }
    }
}
