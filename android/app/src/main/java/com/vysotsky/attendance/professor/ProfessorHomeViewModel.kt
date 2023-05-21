package com.vysotsky.attendance.professor

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Database
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.api.PredefinedClass
import com.vysotsky.attendance.api.PredefinedClassToSend
import com.vysotsky.attendance.api.RetrofitInstance
import com.vysotsky.attendance.api.Session
import com.vysotsky.attendance.database.AttendanceDatabase
import com.vysotsky.attendance.database.Class
import com.vysotsky.attendance.database.ClassDao
import com.vysotsky.attendance.database.PredefinedClassDB
import com.vysotsky.attendance.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class ProfessorHomeViewModel(
    private val db: AttendanceDatabase
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
            classes = db.classDao.getAll()
        } catch (e: Throwable) {
            Log.e(TAG, "ProfessorHomeViewModel getFromDatabase() error! ", e)
        }
        return classes
    }

    private suspend fun getClassesFromApi(email: String, token: String): Resource<List<Session>> {
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

    private suspend fun getPredefinedFromDB(): List<PredefinedClassDB> {
        var predefined: List<PredefinedClassDB> = listOf()
        try {
            predefined = db.predefinedClassDao.getAll()
        } catch (e: Throwable) {
            Log.e(TAG, "ProfessorHomeViewModel getPredefinedFromDB() error! ", e)
        }
        return predefined
    }

    private suspend fun getPredefinedFromApi(token: String): Resource<List<PredefinedClass>> {
        var result: Resource<List<PredefinedClass>> = Resource.Error("network error")
        try {
            val response = RetrofitInstance.api.getPredefinedClasses(token)
            if (response.body() != null) {
                Log.d(TAG, "getPredefinedFromApi(): response.body() = ${response.body()}")
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
        var apiPredefined: List<PredefinedClass> = listOf()
        var dbPredefined: List<PredefinedClassDB> = listOf()

        var connectionToApi = true

        state.value = (
            State(
                databaseLoaded = false,
                apiResponse = Resource.Loading(),
                classes = listOf()
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            listOf(
                launch {
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
                launch {
                    val res = getClassesFromApi(email, token)
                    if (res is Resource.Success && res.data != null) {
                        apiClasses = res.data
                        //we also might need to do the sync requests, so don't display anything yet
                    } else {
                        if (connectionToApi) {
                            state.postValue(
                                State(
                                    databaseLoaded = state.value?.databaseLoaded ?: false,
                                    apiResponse = Resource.Error(res.message ?: "Network error"),
                                    classes = state.value?.classes ?: listOf()
                                )
                            )
                            synchronized(this@ProfessorHomeViewModel) {
                                connectionToApi = false
                            }
                        }
                    }
                    Log.d(TAG, "after api()")
                },
                launch {
                    dbPredefined = getPredefinedFromDB()
                    //TODO: maybe change some state...
                },
                launch {
                    val res = getPredefinedFromApi(token)
                    if (res is Resource.Success && res.data != null) {
                        apiPredefined = res.data
                    } else {
                        if (connectionToApi) {
                            state.postValue(
                                State(
                                    databaseLoaded = state.value?.databaseLoaded ?: false,
                                    apiResponse = Resource.Error(res.message ?: "Network error"),
                                    classes = state.value?.classes ?: listOf() //TODO: make list of predefined classes!
                                )
                            )
                            synchronized(this@ProfessorHomeViewModel) {
                                connectionToApi = false
                            }
                        }
                    }
                    Log.d(TAG, "after predefinedApi()")
                },
            ).joinAll()

            Log.d(TAG, "after joinAll()")
            Log.d(TAG, "databaseClasses = ${databaseClasses}")
            Log.d(TAG, "apiClasses = ${apiClasses}")
            Log.d(TAG, "apiPredefined = ${apiPredefined}")
            Log.d(TAG, "dbPredefined = ${dbPredefined}")

            val allClasses = (databaseClasses + apiClasses).distinctBy { it.date }
            val toInsertInDatabase = allClasses - databaseClasses.toSet()
            val toSendToApi = allClasses - apiClasses.toSet()

            val allPredefined = (dbPredefined + apiPredefined.map { it.asDatabaseModel() })//just a list containing all subject names
//            Log.d(TAG, "DELETE math allPredefined = ${allPredefined.filter{it.subjectName == "math"}}")
            val predefinedToInsertInDB = mutableListOf<PredefinedClassDB>()
            val predefinedToSend = mutableListOf<PredefinedClassToSend>()
            for (p in allPredefined) {
                val inDb = dbPredefined.find { it.subjectName == p.subjectName }
                val inApi = apiPredefined.find { it.subjectName == p.subjectName }
                if (inApi != null && (inDb == null || inDb.updatedAt < inApi.updatedAt)) {
                    predefinedToInsertInDB += p
                } else if (inDb != null && (inApi == null || inApi.updatedAt < inDb.updatedAt)) {
                    predefinedToSend += p.asNetworkModel()
                }
            }

            //insert and/or update api
            if (toInsertInDatabase.isNotEmpty()) {
                try {
                    db.classDao.insertAll(toInsertInDatabase.map { s ->
                        Class(
                            date = s.date,
                            subjectName = s.subjectName,
                            students = ArrayList(s.students)
                        )
                    })
                } catch (e: Error) {
                    Log.e(TAG, "Can't insert class into database!", e)
                }
            }

            if (predefinedToInsertInDB.isNotEmpty()) {
                try {
                    db.predefinedClassDao.insertAll(predefinedToInsertInDB)
                } catch (e: Error) {
                    Log.e(TAG, "Can't insert predefined into database!", e)
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
            var sendPredefinedSuccess = true
            if (connectionToApi && predefinedToSend.isNotEmpty()) {
                for (p in predefinedToSend) {
                    try {
                        val resp = RetrofitInstance.api.updatePredefined(token, p)
                        if (resp.isSuccessful) {
                            continue
                        } else {
                            sendPredefinedSuccess = false
                            break
                        }
                    } catch (e: Throwable) {
                        sendPredefinedSuccess = false
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