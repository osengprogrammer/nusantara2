import com.azuratech.azuratime.domain.classes.usecase.*
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassRepository @Inject constructor(
    private val getClassesUseCase: GetClassesUseCase,
    private val syncClassesUseCase: SyncClassesUseCase,
    private val updateClassUseCase: UpdateClassUseCase,
    private val deleteClassUseCase: DeleteClassUseCase
) {

    @Deprecated("Route through GetClassesUseCase")
    val allClasses: Flow<List<ClassEntity>> = getClassesUseCase().map { 
        when(it) {
            is Result.Success -> it.data
            else -> emptyList()
        }
    }

    @Deprecated("Route through SyncClassesUseCase")
    suspend fun performClassDeltaSync() = syncClassesUseCase()

    @Deprecated("Route through UpdateClassUseCase")
    suspend fun upsertClass(name: String, id: String? = null) = updateClassUseCase(name, id)

    @Deprecated("Route through DeleteClassUseCase")
    suspend fun deleteClass(id: String) = deleteClassUseCase(id)
}
