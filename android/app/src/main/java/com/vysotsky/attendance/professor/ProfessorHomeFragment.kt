package com.vysotsky.attendance.professor

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentProfessorHomeBinding
import com.vysotsky.attendance.api.Session
import com.vysotsky.attendance.util.Resource
import com.vysotsky.attendance.util.SubjectsAdapter
import java.io.Serializable

//first, make api request
//then have a recycler view in grid to display all subjects.

class ProfessorHomeFragment : Fragment() {
    private var _binding: FragmentProfessorHomeBinding? = null
    private val binding
        get() = _binding!!

    private val activityViewModel: ProfessorHomeViewModel by activityViewModels()

    private lateinit var subjectsAdapter: SubjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfessorHomeBinding.inflate(inflater, container, false)
        Log.d(TAG, "ProfessorHomeFragment onCreateView()")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        val sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        val email = sharedPreferences.getString(getString(R.string.saved_email), "")
        val token = sharedPreferences.getString(getString(R.string.access_token), "")
        activityViewModel.getSessions(email!!, token=token!!)
//        binding.btnRetryRequest.setOnClickListener {
//            activityViewModel.getSessions(email, token=token, force = true)
//            activityViewModel.btnRetryRequestVisibility.value = false
//        }
        binding.swipeToRefresh.setOnRefreshListener {
            Log.d(TAG, "ProfessorHomeFragment onRefresh()")
            activityViewModel.getSessions(email, token, force=true)
        }
        subscribe()
    }

    private fun setUpRecyclerView() = binding.rv.apply {
        subjectsAdapter = SubjectsAdapter { subjectName ->
            val allSessions = activityViewModel.state.value?.classes

            if (allSessions == null) {
                Log.e(TAG, "Subject onClick(): sessions list is null!", )
            } else {
                val sessions = allSessions.filter { it.subjectName == subjectName }
                val intent = Intent(requireContext(), ClassAttendanceActivity::class.java)
                intent.putExtra(ClassAttendanceActivity.EXTRA_CLASSES_KEY, sessions as Serializable) //this actually can be fetched from the database
                startActivity(intent)
            }
        }
        adapter = subjectsAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }


    private fun fillRecyclerView(sessions: List<Session>) {
        if (sessions.isEmpty()) {
            activityViewModel.tvNoItemsVisibility.value = true
        } else {
            activityViewModel.tvNoItemsVisibility.value = false
            val subjects = sessions.map { s -> s.subjectName }.toSet().toList()
            subjectsAdapter.subjects = subjects
        }
    }

    private fun subscribe() {
        activityViewModel.state.observe(viewLifecycleOwner) {
            Log.d(TAG, "observer(): state = ${it}")
            if (it.databaseLoaded) {
                binding.pb.isVisible = false
            } else {
                binding.pb.isVisible = true
                return@observe
            }

            when (it.apiResponse) {
                is Resource.Loading -> {
                    binding.textView.text = getString(R.string.updating)
                    binding.swipeToRefresh.isRefreshing = true
                }

                is Resource.Error -> {
                    binding.swipeToRefresh.isRefreshing = false
                    binding.textView.text = getString(R.string.your_classes)
                    Toast.makeText(
                        requireContext(),
                        it.apiResponse.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    binding.swipeToRefresh.isRefreshing = false
                    binding.textView.text = getString(R.string.your_classes)
                }
            }

            fillRecyclerView(it.classes)
        }

//        activityViewModel.btnRetryRequestVisibility.observe(viewLifecycleOwner) {
//            binding.btnRetryRequest.isVisible = it
//        }

        activityViewModel.tvNoItemsVisibility.observe(viewLifecycleOwner) {
            binding.tvNoItems.isVisible = it
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}