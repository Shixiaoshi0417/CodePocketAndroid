package io.github.shixiaoshi0417.codepocketandroid.model

data class OpenCodeSession(
    val id: String,
    val title: String,
    val directory: String,
    val agent: String,
    val model: String,
    val timeUpdated: Long
)
