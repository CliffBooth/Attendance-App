package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.internal.ResourceUtils
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.Session
import com.vysotsky.attendance.api.Student
import com.vysotsky.attendance.database.ClassDao
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.vysotsky.attendance.database.Class
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class ProfessorHomeViewModel(
    private val dao: ClassDao
) : ViewModel() {
    val state = MutableLiveData<State>()

    val tvNoItemsVisibility = MutableLiveData(false)
    val btnRetryRequestVisibility = MutableLiveData(false)

    //to avoid making request with every screen rotation
    private var searchedOnStartUp = false

    /**
     * get session from api and from database. Add lacking sessions to database and add lacking sessions to api.
     */

    fun getSessions(email: String, token: String, force: Boolean = false) {
        if (!force && searchedOnStartUp)
            return
        searchedOnStartUp = true
        test(email, token)
        return
    }

    private suspend fun getFromDatabase(): List<Class> {
        var classes: List<Class> = listOf()
        try {
            classes = dao.getAll()
        } catch (e: Throwable) {
            Log.e(TAG, "ProfessorHomeViewModel getFromDatabase() error! ", e)
        }
        return classes
    }

    private suspend fun getFromApi(email: String, token: String): Resource<List<Session>> {
        var result: Resource<List<Session>> = Resource.Error("network error")
        try {
            val response = RetrofitInstance.api.getProfessorSessions(email, token)
            if (response.body() != null) {
                result = Resource.Success(response.body()!!)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "ProfessorHomeViewModel getFromApi() error! ", e)
        }
        return result
    }

    private fun test(email: String, token: String) {
        var databaseClasses: List<Session> = listOf()
        var apiClasses: List<Session> = listOf()

        var connectionToApi = false

        state.value = (
            State(
                databaseLoaded = false,
                apiResponse = Resource.Loading(),
                classes = listOf()
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            listOf(
                async {
                    databaseClasses =
                        getFromDatabase().map { Session(it.date, it.students, it.subjectName) }
                    //display data from api first
                    state.postValue(
                        State(
                            databaseLoaded = true,
                            apiResponse = state.value?.apiResponse ?: Resource.Loading(),
                            databaseClasses
                        )
                    )
                    Log.d(TAG, "after db()")
                },
                async {
                    val res = getFromApi(email, token)
                    if (res is Resource.Success && res.data != null) {
                        connectionToApi = true
                        apiClasses = res.data
                        //we also might need to do the sync requests, so don't display anything yet
                    } else {
                        state.postValue(
                            State(
                                databaseLoaded = state.value?.databaseLoaded ?: false,
                                apiResponse = Resource.Error(res.message ?: "Network error"),
                                classes = state.value?.classes ?: listOf()
                            )
                        )
                    }

                    Log.d(TAG, "after api()")
                }
            ).awaitAll()

            Log.d(TAG, "after awaitAll()")
            Log.d(TAG, "databaseClasses = ${databaseClasses}")
            Log.d(TAG, "apiClasses = ${apiClasses}")

            val allClasses = (databaseClasses + apiClasses).distinctBy { it.date }
            val toInsertInDatabase = allClasses - databaseClasses.toSet()
            val toSendToApi = allClasses - apiClasses.toSet()
            //insert and/or update api
            if (toInsertInDatabase.isNotEmpty()) {
                try {
                    dao.insertAll(toInsertInDatabase.map { s ->
                        Class(
                            date = s.date,
                            subjectName = s.subjectName,
                            students = ArrayList(s.students)
                        )
                    })
                } catch (e: Error) {
                    Log.e(TAG, "Can't insert into database!", e)
                }
            }

            var sendSuccess = true
            if (connectionToApi && toSendToApi.isNotEmpty()) {
                for (s in toSendToApi) {
                    try {
                        val resp = RetrofitInstance.api.addSession(email, s, token)
                        if (resp.isSuccessful) {
                            continue
                        } else {
                            sendSuccess = false
                            break
                        }
                    } catch (e: Throwable) {
                        sendSuccess = false
                        break
                    }
                }
            }

            state.postValue(State(
                databaseLoaded = true,
                apiResponse = if (sendSuccess) Resource.Success(listOf()) else Resource.Error("Cannot synchronize data with server"),
                classes = allClasses
            ))

        }
    }

}

data class State(
    var databaseLoaded: Boolean,
    var apiResponse: Resource<List<Session>>,
    var classes: List<Session>,
)