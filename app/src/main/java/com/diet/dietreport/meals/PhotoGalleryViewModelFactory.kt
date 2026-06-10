package com.diet.dietreport.meals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.diet.dietreport.data.db.AppDatabase

class PhotoGalleryViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val mealLogDao = AppDatabase.getInstance(context).mealLogDao()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PhotoGalleryViewModel(mealLogDao) as T
}
