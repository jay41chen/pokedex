package io.github.jay41chen.pokedex.domain.time

fun interface TimeSource { fun nowEpochMillis(): Long }  // 注入，測試用固定值
