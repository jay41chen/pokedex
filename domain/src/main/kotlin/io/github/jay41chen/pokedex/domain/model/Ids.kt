package io.github.jay41chen.pokedex.domain.model

@JvmInline value class PokemonId(val value: Int)
@JvmInline value class TypeName(val value: String)   // 不用 enum：type 清單來自資料，不寫死
@JvmInline value class CaptureId(val value: String)  // UUID
