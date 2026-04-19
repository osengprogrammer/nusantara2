package com.azuratech.azuratime.data.repository

import com.azuratech.azuratime.domain.classes.usecase.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassRepository @Inject constructor(
    private val getClassesUseCase: GetClassesUseCase,
    private val syncClassesUseCase: SyncClassesUseCase,
    private val updateClassUseCase: UpdateClassUseCase,
    private val deleteClassUseCase: DeleteClassUseCase
) {
    // This repository is now a lean coordinator for UseCases if needed, 
    // or can be thinned further if ViewModels call UseCases directly.
}
