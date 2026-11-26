package io.github.jay41chen.pokedex.data.remote

import io.github.jay41chen.pokedex.data.remote.dto.PokemonDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonListDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonSpeciesDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface PokeApi {
    @GET("pokemon")
    suspend fun list(@Query("limit") limit: Int): PokemonListDto

    @GET("pokemon/{id}")
    suspend fun pokemon(@Path("id") id: Int): PokemonDto

    @GET("pokemon-species/{id}")
    suspend fun species(@Path("id") id: Int): PokemonSpeciesDto
}

/** Full URLs used for failure reports. The paths must match the @GET annotations on [PokeApi]. */
internal object PokeApiUrls {
    const val BASE_URL = "https://pokeapi.co/api/v2/"

    fun list(limit: Int) = "${BASE_URL}pokemon?limit=$limit"
    fun pokemon(id: Int) = "${BASE_URL}pokemon/$id"
    fun species(id: Int) = "${BASE_URL}pokemon-species/$id"
}
