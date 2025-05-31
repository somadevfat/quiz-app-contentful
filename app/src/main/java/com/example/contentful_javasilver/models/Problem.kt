package com.example.contentful_javasilver.models

data class Problem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val isCompleted: Boolean = false
) 