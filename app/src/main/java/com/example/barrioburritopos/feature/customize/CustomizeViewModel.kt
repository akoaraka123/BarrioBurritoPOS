package com.example.barrioburritopos.feature.customize

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.CustomizeStepType
import com.example.barrioburritopos.data.repository.CustomizeOptionRepository
import com.example.barrioburritopos.domain.model.CustomBurritoSelection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomizeWizardState(
    val currentStep: Int = 0,
    val showReview: Boolean = false,
    val rice: String? = null,
    val main: String? = null,
    val bases: Map<String, Int> = emptyMap(),
    val topping: String? = null,
    val sauce: String? = null,
    val addOns: Set<String> = emptySet()
) {
    fun toSelection(addOnPrices: Map<String, Double>): CustomBurritoSelection = CustomBurritoSelection(
        rice = rice,
        main = main,
        bases = bases.flatMap { (name, count) -> List(count) { name } }.toSet(),
        topping = topping,
        sauce = sauce,
        addOns = addOns,
        addOnPrices = addOnPrices
    )
    
    val totalBaseCount: Int get() = bases.values.sum()
}

class CustomizeViewModel(
    private val repository: CustomizeOptionRepository
) : ViewModel() {

    private val _wizardState = MutableStateFlow(CustomizeWizardState())
    val wizardState: StateFlow<CustomizeWizardState> = _wizardState.asStateFlow()

    val riceOptions = repository.getByStepType(CustomizeStepType.RICE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mainOptions = repository.getByStepType(CustomizeStepType.MAIN)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val baseOptions = repository.getByStepType(CustomizeStepType.BASE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val toppingOptions = repository.getByStepType(CustomizeStepType.TOPPING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sauceOptions = repository.getByStepType(CustomizeStepType.SAUCE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val addOnOptions = repository.getByStepType(CustomizeStepType.ADDON)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addOnPrices: StateFlow<Map<String, Double>> = addOnOptions
        .map { options -> options.associate { it.name to it.price } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
    }

    fun optionsForStep(stepIndex: Int): StateFlow<List<CustomizeOptionEntity>> = when (stepIndex) {
        0 -> riceOptions
        1 -> mainOptions
        2 -> baseOptions
        3 -> toppingOptions
        4 -> sauceOptions
        5 -> addOnOptions
        else -> MutableStateFlow(emptyList())
    }

    fun updateRice(value: String) {
        _wizardState.update { it.copy(rice = value) }
    }

    fun updateMain(value: String) {
        _wizardState.update { it.copy(main = value) }
    }

    fun toggleBase(value: String) {
        _wizardState.update { state ->
            val currentCount = state.bases[value] ?: 0
            val totalCount = state.totalBaseCount
            
            val updated = if (totalCount < 5) {
                // Always increment count if under max (allows double, triple selection)
                state.bases + (value to (currentCount + 1))
            } else if (currentCount > 0) {
                // If at max, reset to unchecked (remove item completely)
                state.bases - value
            } else {
                state.bases
            }
            state.copy(bases = updated)
        }
    }

    fun updateTopping(value: String) {
        _wizardState.update { it.copy(topping = value) }
    }

    fun updateSauce(value: String) {
        _wizardState.update { it.copy(sauce = value) }
    }

    fun toggleAddOn(value: String) {
        _wizardState.update { state ->
            val updated = if (state.addOns.contains(value)) state.addOns - value else state.addOns + value
            state.copy(addOns = updated)
        }
    }

    fun goToNextStep(): Boolean {
        val state = _wizardState.value
        if (state.showReview) return true
        val stepType = CustomizeStepType.fromOrdinal(state.currentStep)
        val valid = when (stepType) {
            CustomizeStepType.RICE -> state.rice != null
            CustomizeStepType.MAIN -> state.main != null
            CustomizeStepType.BASE -> state.totalBaseCount > 0
            CustomizeStepType.TOPPING -> state.topping != null
            CustomizeStepType.SAUCE -> state.sauce != null
            CustomizeStepType.ADDON -> true
        }
        if (!valid) {
            viewModelScope.launch { _message.emit(validationMessage(stepType)) }
            return false
        }
        if (state.currentStep < CustomizeStepType.entries.size - 1) {
            _wizardState.update { it.copy(currentStep = it.currentStep + 1) }
        }
        return true
    }

    fun goToPreviousStep(): Boolean {
        val state = _wizardState.value
        if (state.showReview) {
            _wizardState.update { it.copy(showReview = false) }
            return true
        }
        if (state.currentStep > 0) {
            _wizardState.update { it.copy(currentStep = it.currentStep - 1) }
            return true
        }
        return false
    }

    fun showReview() {
        _wizardState.update { it.copy(showReview = true) }
    }

    fun resetWizard() {
        _wizardState.value = CustomizeWizardState()
    }

    fun addOption(
        stepType: CustomizeStepType,
        name: String,
        price: Double,
        imageUri: String?,
        onResult: (Boolean) -> Unit
    ) {
        if (name.isBlank()) {
            viewModelScope.launch { _message.emit("Option name is required.") }
            onResult(false)
            return
        }
        if (stepType == CustomizeStepType.ADDON && price <= 0) {
            viewModelScope.launch { _message.emit("Add-on price is required.") }
            onResult(false)
            return
        }
        viewModelScope.launch {
            repository.addOption(stepType, name, price, imageUri)
            _message.emit("Option added successfully.")
            onResult(true)
        }
    }

    private fun validationMessage(stepType: CustomizeStepType): String = when (stepType) {
        CustomizeStepType.RICE -> "Please select a rice option."
        CustomizeStepType.MAIN -> "Please select a main option."
        CustomizeStepType.BASE -> "Please select at least one base (max 5)."
        CustomizeStepType.TOPPING -> "Please select a topping."
        CustomizeStepType.SAUCE -> "Please select a sauce."
        CustomizeStepType.ADDON -> ""
    }

    companion object {
        fun factory(repository: CustomizeOptionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomizeViewModel(repository) as T
                }
            }
        }
    }
}
