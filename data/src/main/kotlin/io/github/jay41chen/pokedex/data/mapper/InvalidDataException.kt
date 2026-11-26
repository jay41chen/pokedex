package io.github.jay41chen.pokedex.data.mapper

/** Permanent failure: retrying will not help (empty types, unparsable URL). */
internal class InvalidDataException(message: String) : Exception(message)
