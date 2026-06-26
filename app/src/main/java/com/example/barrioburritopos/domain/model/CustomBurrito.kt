package com.example.barrioburritopos.domain.model

import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.CustomizeStepType

object CustomBurritoPricing {
    var BASE_PRICE = 130.0
}

data class CustomBurritoSelection(
    val rice: String? = null,
    val main: String? = null,
    val bases: Set<String> = emptySet(),
    val topping: String? = null,
    val sauce: String? = null,
    val addOns: Map<String, Int> = emptyMap(),
    val addOnPrices: Map<String, Double> = emptyMap()
) {
    val addOnsTotal: Double
        get() = addOns.entries.sumOf { (name, count) ->
            (addOnPrices[name] ?: 0.0) * count
        }

    val finalPrice: Double
        get() = CustomBurritoPricing.BASE_PRICE + addOnsTotal

    val isComplete: Boolean
        get() = rice != null && main != null && bases.isNotEmpty() &&
            topping != null && sauce != null

    fun buildDetails(): String {
        val parts = mutableListOf<String>()
        rice?.let { parts.add("Rice: $it") }
        main?.let { parts.add("Main: $it") }
        if (bases.isNotEmpty()) parts.add("Base: ${bases.joinToString(", ")}")
        topping?.let { parts.add("Topping: $it") }
        sauce?.let { parts.add("Sauce: $it") }
        if (addOns.isNotEmpty()) {
            val addOnDetails = addOns.entries.map { (name, count) ->
                val price = addOnPrices[name]
                val totalPrice = (price ?: 0.0) * count
                if (totalPrice > 0) {
                    if (count > 1) "$name x$count (₱${"%.0f".format(totalPrice)})" else "$name (₱${"%.0f".format(totalPrice)})"
                } else {
                    if (count > 1) "$name x$count" else name
                }
            }
            parts.add("Add-ons: ${addOnDetails.joinToString(", ")}")
        }
        return parts.joinToString("\n")
    }
}

val DEFAULT_CUSTOMIZE_OPTIONS: List<CustomizeOptionEntity> = buildList {
    addAll(listOf(
        "Mexican Rice" to "drawable://mexican_rice",
        "Garlic Rice" to "drawable://garlic_rice"
    ).map {
        CustomizeOptionEntity(stepType = CustomizeStepType.RICE.name, name = it.first, imageUri = it.second, isDefault = true)
    })
    addAll(
        listOf(
            "Classic Beef / Mexican Beef" to "drawable://classic_beef",
            "Beef Chorizo" to "drawable://beef_chorizo",
            "Bistek" to "drawable://bistek",
            "Chicken Tenders" to "drawable://chicken_tenders"
        ).map {
            CustomizeOptionEntity(stepType = CustomizeStepType.MAIN.name, name = it.first, imageUri = it.second, isDefault = true)
        }
    )
    addAll(listOf(
        "Cabbage" to "drawable://cabbage",
        "Tomato" to "drawable://tomato",
        "Onion" to "drawable://onion",
        "Cucumber" to "drawable://cucumber",
        "Corn" to "drawable://corn"
    ).map {
        CustomizeOptionEntity(stepType = CustomizeStepType.BASE.name, name = it.first, imageUri = it.second, isDefault = true)
    })
    addAll(listOf(
        "Tomato Salsa" to "drawable://tomato_salsa",
        "Sour Cream" to "drawable://sour_cream",
        "Shredded Cheese" to "drawable://shredded_cheese"
    ).map {
        CustomizeOptionEntity(stepType = CustomizeStepType.TOPPING.name, name = it.first, imageUri = it.second, isDefault = true)
    })
    addAll(
        listOf(
            "Cheese Sauce" to "drawable://cheese_sauce",
            "Sweet Chili" to "drawable://sweet_chili",
            "Honey Mustard" to "drawable://honey_mustard",
            "Honey Sriracha" to "drawable://honey_sriracha",
            "BBQ" to "drawable://bbq"
        ).map {
            CustomizeOptionEntity(stepType = CustomizeStepType.SAUCE.name, name = it.first, imageUri = it.second, isDefault = true)
        }
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Main",
            price = 40.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Rice",
            price = 20.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Base",
            price = 10.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Topping",
            price = 25.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Tortilla",
            price = 20.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Extra Sauce Inside Burrito",
            price = 15.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "1 oz Side Sauce Cup",
            price = 20.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Jalapeño",
            price = 20.0,
            isDefault = true
        )
    )
    add(
        CustomizeOptionEntity(
            stepType = CustomizeStepType.ADDON.name,
            name = "Fries",
            price = 40.0,
            isDefault = true
        )
    )
}
