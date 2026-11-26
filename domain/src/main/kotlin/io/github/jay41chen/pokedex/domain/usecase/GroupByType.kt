package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.model.TypeSection

fun groupByType(entries: List<CatalogEntry>): List<TypeSection> {
    val byType = LinkedHashMap<TypeName, MutableMap<Int, PokemonCard>>()
    for (entry in entries) {
        for (type in entry.types) {
            byType.getOrPut(type) { LinkedHashMap() }[entry.card.id.value] = entry.card
        }
    }
    return byType.entries
        .sortedBy { it.key.value }
        .map { (type, cards) -> TypeSection(type, cards.values.sortedBy { it.id.value }) }
}
