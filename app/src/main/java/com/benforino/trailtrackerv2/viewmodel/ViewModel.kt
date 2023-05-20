package com.benforino.trailtrackerv2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benforino.trailtrackerv2.database.Trail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    private val repository: Repository
): ViewModel() {


    fun insertTrail(trail: Trail) = viewModelScope.launch {
        repository.insertTrail(trail)
    }
}