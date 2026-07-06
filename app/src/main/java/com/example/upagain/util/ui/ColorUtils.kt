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

fun getItemMaterialColor(material: String): Int {
    return when (material.lowercase()) {
        "plastic" -> R.color.material_plastic
        "metal" -> R.color.material_metal
        "glass" -> R.color.material_glass
        "wood" -> R.color.material_wood
        "textile" -> R.color.material_textile
        "other" -> R.color.material_other
        else -> R.color.material_other
    }
}