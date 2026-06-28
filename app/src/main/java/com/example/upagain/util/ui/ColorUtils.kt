package com.example.upagain.util.ui

import com.example.upagain.R

fun getPostCategoryColor(category: String): Int {
    return when (category.lowercase()) {
        "tutorial" -> R.color.category_tutorial
        "project" -> R.color.category_project
        "tips" -> R.color.category_tips
        "news" -> R.color.category_news
        "case_study" -> R.color.category_case_study
        else -> R.color.category_other
    }
}