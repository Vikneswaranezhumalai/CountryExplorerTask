package com.txstate.countryexplorertask.model

data class Country(
    val name: String?,
    val capital: String?,
    val code: String?,
    val flag: String?,
    val region: String?,
    val language: Language?,
    val currency: Currency?
)

data class Language(
    val code: String?,
    val name: String?
)

data class Currency(
    val code: String?,
    val name: String?,
    val symbol: String?
)
