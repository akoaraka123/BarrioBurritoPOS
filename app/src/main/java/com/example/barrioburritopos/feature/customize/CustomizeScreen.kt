package com.example.barrioburritopos.feature.customize

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.CustomizeStepType
import com.example.barrioburritopos.domain.model.CustomBurritoPricing
import com.example.barrioburritopos.domain.model.CustomBurritoSelection
import com.example.barrioburritopos.feature.pos.accentRed
import com.example.barrioburritopos.feature.pos.accentYellow
import com.example.barrioburritopos.feature.pos.backgroundColor
import com.example.barrioburritopos.feature.pos.cardColor
import com.example.barrioburritopos.feature.pos.darkText
import java.io.File

private data class StepUiConfig(
    val badge: String,
    val title: String,
    val requirement: String,
    val hint: String
)

private val STEP_CONFIGS = listOf(
    StepUiConfig("STEP 1", "PICK YOUR RICE", "Choose 1", "Choose your favorite rice as the base of your burrito."),
    StepUiConfig("STEP 2", "PICK YOUR MAIN", "Choose 1", "Pick your protein main filling."),
    StepUiConfig("STEP 3", "PICK YOUR BASE", "Choose up to 5", "Select up to 5 fresh base ingredients."),
    StepUiConfig("STEP 4", "PICK YOUR TOPPING", "Choose 1", "Choose one topping to finish it off."),
    StepUiConfig("STEP 5", "PICK YOUR SAUCE", "Choose 1", "Select your sauce flavor."),
    StepUiConfig("STEP 6", "ADD-ONS", "Optional extras", "Make your burrito even better with add-ons.")
)

@Composable
fun CustomizeScreen(
    viewModel: CustomizeViewModel,
    currency: String,
    onNavigateToPos: () -> Unit,
    onAddCustomBurrito: (CustomBurritoSelection) -> Boolean,
    onAddedToOrder: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val wizardState by viewModel.wizardState.collectAsState()
    val addOnPrices by viewModel.addOnPrices.collectAsState()
    val riceOptions by viewModel.riceOptions.collectAsState()
    val mainOptions by viewModel.mainOptions.collectAsState()
    val baseOptions by viewModel.baseOptions.collectAsState()
    val toppingOptions by viewModel.toppingOptions.collectAsState()
    val sauceOptions by viewModel.sauceOptions.collectAsState()
    val addOnOptions by viewModel.addOnOptions.collectAsState()

    var showAddOptionDialog by remember { mutableStateOf(false) }

    val selection = wizardState.toSelection(addOnPrices)
    val totalSteps = CustomizeStepType.entries.size
    val stepConfig = STEP_CONFIGS.getOrElse(wizardState.currentStep) { STEP_CONFIGS.first() }

    LaunchedEffect(Unit) {
        viewModel.message.collect { onShowMessage(it) }
    }

    BackHandler {
        if (wizardState.showReview) {
            viewModel.goToPreviousStep()
        } else if (wizardState.currentStep == 0) {
            onNavigateToPos()
        } else {
            viewModel.goToPreviousStep()
        }
    }

    if (showAddOptionDialog) {
        AddOptionDialog(
            stepType = CustomizeStepType.fromOrdinal(wizardState.currentStep),
            currency = currency,
            onDismiss = { showAddOptionDialog = false },
            onSave = { name, price, imageUri ->
                viewModel.addOption(
                    stepType = CustomizeStepType.fromOrdinal(wizardState.currentStep),
                    name = name,
                    price = price,
                    imageUri = imageUri
                ) { success ->
                    if (success) showAddOptionDialog = false
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (wizardState.showReview) {
            ReviewContent(
                selection = selection,
                currency = currency,
                onBack = { viewModel.goToPreviousStep() },
                onAddToOrder = {
                    onAddCustomBurrito(selection)
                    viewModel.resetWizard()
                    onAddedToOrder()
                }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AddOptionButton(onClick = { showAddOptionDialog = true })
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Build Your Perfect Burrito",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = darkText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "You choose. We roll.",
                style = MaterialTheme.typography.titleMedium,
                color = accentRed,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))
            StepProgressIndicator(
                currentStep = wizardState.currentStep,
                totalSteps = totalSteps
            )

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StepBadge(text = stepConfig.badge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (wizardState.currentStep == 5) "Want to add extras?" else stepConfig.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkText
                    )
                    Text(
                        text = if (wizardState.currentStep == 5) {
                            "Make your burrito even better with add-ons."
                        } else {
                            stepConfig.requirement
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    if (wizardState.currentStep == 2) {
                        Spacer(Modifier.height(8.dp))
                        SelectionCountBadge(count = wizardState.totalBaseCount, max = 5)
                    }
                }

                Spacer(Modifier.height(8.dp))

                when (wizardState.currentStep) {
                    0 -> OptionCardsRow(
                        options = riceOptions,
                        selectedNames = wizardState.rice?.let { setOf(it) } ?: emptySet(),
                        singleSelect = true,
                        onSelect = viewModel::toggleRice,
                        onToggle = {},
                        step = 0
                    )
                    1 -> OptionCardsRow(
                        options = mainOptions,
                        selectedNames = wizardState.main?.let { setOf(it) } ?: emptySet(),
                        singleSelect = true,
                        onSelect = viewModel::toggleMain,
                        onToggle = {},
                        step = 1
                    )
                    2 -> OptionCardsRow(
                        options = baseOptions,
                        selectedNames = wizardState.bases.keys,
                        selectedCounts = wizardState.bases,
                        singleSelect = false,
                        maxSelection = 5,
                        totalSelected = wizardState.totalBaseCount,
                        onSelect = {},
                        onToggle = viewModel::toggleBase,
                        step = 2
                    )
                    3 -> OptionCardsRow(
                        options = toppingOptions,
                        selectedNames = wizardState.topping?.let { setOf(it) } ?: emptySet(),
                        singleSelect = true,
                        onSelect = viewModel::toggleTopping,
                        onToggle = {},
                        step = 3
                    )
                    4 -> OptionCardsRow(
                        options = sauceOptions,
                        selectedNames = wizardState.sauce?.let { setOf(it) } ?: emptySet(),
                        singleSelect = true,
                        onSelect = viewModel::toggleSauce,
                        onToggle = {},
                        step = 4
                    )
                    5 -> AddOnsContent(
                        options = addOnOptions,
                        selected = wizardState.addOns,
                        currency = currency,
                        onToggle = viewModel::toggleAddOn
                    )
                }

                        Spacer(Modifier.height(8.dp))
                        HintBox(text = stepConfig.hint)
            }

            Spacer(Modifier.height(16.dp))

            if (wizardState.currentStep == 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showReview() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip Add-ons")
                    }
                    Button(
                        onClick = { viewModel.showReview() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Review Order", color = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            WizardBottomBar(
                showNext = wizardState.currentStep < 5,
                onBack = {
                    if (wizardState.currentStep == 0) onNavigateToPos()
                    else viewModel.goToPreviousStep()
                },
                onNext = {
                    if (!viewModel.goToNextStep()) return@WizardBottomBar
                }
            )
        }
    }
}

@Composable
private fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Step ${currentStep + 1} of $totalSteps",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index <= currentStep) 12.dp else 10.dp)
                        .clip(CircleShape)
                        .background(if (index <= currentStep) accentRed else Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
private fun StepBadge(text: String) {
    Surface(
        color = accentRed,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SelectionCountBadge(count: Int, max: Int) {
    Surface(
        color = Color(0xFFE8F5E9),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "✓ Selected $count of $max",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun HintBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = accentRed)
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = darkText)
        }
    }
}

@Composable
private fun AddOptionButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentRed),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add Option", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OptionCardsRow(
    options: List<CustomizeOptionEntity>,
    selectedNames: Set<String>,
    selectedCounts: Map<String, Int> = emptyMap(),
    singleSelect: Boolean,
    maxSelection: Int = 1,
    totalSelected: Int = 0,
    onSelect: (String) -> Unit,
    onToggle: (String) -> Unit,
    step: Int
) {
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp > 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Determine columns based on step and screen size
    val columns = when {
        isTabletOrLandscape -> when (step) {
            0 -> 2 // Step 1: 2 large cards
            1 -> 4 // Step 2: 4 large cards
            2 -> 5 // Step 3: 5 cards if fits
            3 -> 3 // Step 4: 3 large cards
            4 -> 5 // Step 5: 5 cards if fits
            else -> 3
        }
        else -> when (step) {
            0 -> 2 // Step 1: 2 cards
            1 -> 2 // Step 2: 2 cards on portrait
            2 -> 2 // Step 3: 2 cards on portrait
            3 -> 2 // Step 4: 2 cards on portrait
            4 -> 2 // Step 5: 2 cards on portrait
            else -> 2
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(options, key = { it.id }) { option ->
            val selected = selectedNames.contains(option.name)
            val count = selectedCounts[option.name] ?: 0
            val enabled = if (singleSelect) true else selected || totalSelected < maxSelection
            KioskOptionCard(
                option = option,
                selected = selected,
                count = count,
                enabled = enabled,
                onClick = {
                    if (singleSelect) onSelect(option.name)
                    else onToggle(option.name)
                }
            )
        }
    }
}

@Composable
private fun KioskOptionCard(
    option: CustomizeOptionEntity,
    selected: Boolean,
    count: Int = 0,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp > 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Controlled card dimensions to fit screen without scrolling
    val cardWidth = if (isTabletOrLandscape) 260.dp else 180.dp
    val cardHeight = if (isTabletOrLandscape) 200.dp else 180.dp
    val imageHeight = if (isTabletOrLandscape) 130.dp else 110.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFFF5EE) else Color(0xFFFAFAFA)
        ),
        border = BorderStroke(if (selected) 4.dp else 2.dp, if (selected) accentRed else Color(0xFFE8E8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OptionImage(
                    imageUri = option.imageUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) darkText else Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SelectionCheckmark(selected = selected, count = count, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun SelectionCheckmark(selected: Boolean, count: Int = 0, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp > 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val checkmarkSize = if (isTabletOrLandscape) 40.dp else 32.dp
    
    if (selected) {
        Box(
            modifier = modifier
                .padding(12.dp)
                .size(if (count > 1) checkmarkSize else checkmarkSize)
                .clip(CircleShape)
                .background(accentRed),
            contentAlignment = Alignment.Center
        ) {
            if (count > 1) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = if (isTabletOrLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isTabletOrLandscape) 24.dp else 20.dp))
            }
        }
    } else {
        Surface(
            modifier = modifier.padding(12.dp).size(checkmarkSize),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(2.dp, Color(0xFFCCCCCC))
        ) {}
    }
}

@Composable
private fun OptionImage(imageUri: String?, modifier: Modifier = Modifier) {
    if (!imageUri.isNullOrBlank()) {
        if (imageUri.startsWith("drawable://")) {
            // Handle drawable resource
            val drawableName = imageUri.removePrefix("drawable://")
            val context = LocalContext.current
            val resourceId = context.resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName
            )
            if (resourceId != 0) {
                Image(
                    painter = painterResource(id = resourceId),
                    contentDescription = null,
                    modifier = modifier.background(Color(0xFFEFEFEF)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = modifier.background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = accentRed.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        } else {
            // Handle file URI
            AsyncImage(
                model = File(imageUri),
                contentDescription = null,
                modifier = modifier.background(Color(0xFFEFEFEF)),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Box(
            modifier = modifier.background(Color(0xFFEFEFEF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = accentRed.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun AddOnsContent(
    options: List<CustomizeOptionEntity>,
    selected: Set<String>,
    currency: String,
    onToggle: (String) -> Unit
) {
    val total = selected.sumOf { name -> options.find { it.name == name }?.price ?: 0.0 }
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp > 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Determine columns based on screen size
    val columns = if (isTabletOrLandscape) 4 else 2

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options, key = { it.id }) { option ->
                val isSelected = selected.contains(option.name)
                KioskOptionCard(
                    option = option,
                    selected = isSelected,
                    enabled = true,
                    onClick = { onToggle(option.name) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Add-ons total:", fontWeight = FontWeight.Bold, color = darkText)
            Text(
                text = "$currency${"%.2f".format(total)}",
                fontWeight = FontWeight.Bold,
                color = accentRed
            )
        }
    }
}

@Composable
private fun WizardBottomBar(showNext: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Back")
        }
        if (showNext) {
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReviewContent(
    selection: CustomBurritoSelection,
    currency: String,
    onBack: () -> Unit,
    onAddToOrder: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Review Your Custom Burrito",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = darkText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ReviewCard(selection = selection, currency = currency)
        }

        if (!selection.isComplete) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Please complete all required selections (Rice, Main, Base, Topping, Sauce)",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
            Button(
                onClick = onAddToOrder,
                enabled = selection.isComplete,
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selection.isComplete) accentYellow else Color.Gray,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Custom Burrito to Order", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReviewCard(selection: CustomBurritoSelection, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewRow("Rice", selection.rice ?: "—")
            ReviewRow("Main", selection.main ?: "—")
            ReviewRow("Base", formatBasesWithCounts(selection.bases))
            ReviewRow("Topping", selection.topping ?: "—")
            ReviewRow("Sauce", selection.sauce ?: "—")
            ReviewRow(
                "Add-ons",
                if (selection.addOns.isEmpty()) "None" else selection.addOns.joinToString(", ")
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ReviewRow("Base burrito", "$currency${"%.2f".format(CustomBurritoPricing.BASE_PRICE)}")
            ReviewRow("Add-ons total", "$currency${"%.2f".format(selection.addOnsTotal)}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Final total", fontWeight = FontWeight.Bold, color = darkText)
                Text(
                    text = "$currency${"%.2f".format(selection.finalPrice)}",
                    fontWeight = FontWeight.Bold,
                    color = accentRed
                )
            }
        }
    }
}

private fun formatBasesWithCounts(bases: Set<String>): String {
    if (bases.isEmpty()) return "—"
    val counts = bases.groupingBy { it }.eachCount()
    return counts.entries.joinToString(", ") { (name, count) ->
        if (count > 1) "$name x$count" else name
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF666666))
        Text(
            text = value,
            color = darkText,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun AddOptionDialog(
    stepType: CustomizeStepType,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double, imageUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(if (stepType == CustomizeStepType.ADDON) "" else "0") }
    var selectedDrawable by remember { mutableStateOf<String?>(null) }
    
    // Available drawable images
    val drawableImages = listOf(
        "beef_chorizo", "bistek", "cabbage", "chicken_tenders", "classic_beef",
        "corn", "cucumber", "garlic_rice", "mexican_rice", "onion", "tomato",
        "bbq", "cheese_sauce", "honey_mustard", "honey_sriracha", "shredded_cheese",
        "sour_cream", "sweet_chili", "tomato_salsa"
    )
    
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Option") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Option name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = {
                        Text(
                            if (stepType == CustomizeStepType.ADDON) {
                                "Price (required)"
                            } else {
                                "Price (optional, default ₱0)"
                            }
                        )
                    },
                    prefix = { Text(currency) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Drawable image selector
                Text("Select Image:", style = MaterialTheme.typography.labelMedium)
                var showImageDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showImageDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDrawable ?: "No image selected")
                }
                if (showImageDialog) {
                    AlertDialog(
                        onDismissRequest = { showImageDialog = false },
                        title = { Text("Select Image") },
                        text = {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedDrawable = null
                                            showImageDialog = false
                                        }
                                    ) {
                                        Text("No image", modifier = Modifier.padding(12.dp))
                                    }
                                }
                                items(drawableImages) { imageName ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedDrawable = imageName
                                            showImageDialog = false
                                        }
                                    ) {
                                        Text(imageName, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showImageDialog = false }) { Text("Cancel") }
                        }
                    )
                }
                
                // Image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedDrawable != null) {
                        val context = LocalContext.current
                        val resourceId = context.resources.getIdentifier(
                            selectedDrawable,
                            "drawable",
                            context.packageName
                        )
                        if (resourceId != 0) {
                            Image(
                                painter = painterResource(id = resourceId),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Image not found", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("No image selected", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val imageUri = selectedDrawable?.let { "drawable://$it" }
                    onSave(name, priceText.toDoubleOrNull() ?: 0.0, imageUri)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentRed)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
