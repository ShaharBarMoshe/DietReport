package com.diet.dietreport.meals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.data.db.MealLog
import com.diet.dietreport.data.db.MealLogDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class PhotoGalleryUiState(
    val photos: List<MealLog> = emptyList(),
)

class PhotoGalleryViewModel(private val mealLogDao: MealLogDao) : ViewModel() {

    val uiState: StateFlow<PhotoGalleryUiState> = mealLogDao.allLogsFlow()
        .map { logs -> PhotoGalleryUiState(photos = logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoGalleryUiState())

    fun deletePhoto(log: MealLog) {
        viewModelScope.launch {
            try {
                val file = File(log.photoPath)
                if (file.exists()) file.delete()
                mealLogDao.deleteById(log.id)
            } catch (e: Exception) {
                Log.e("DR/Gallery", "Failed to delete photo", e)
            }
        }
    }
}
