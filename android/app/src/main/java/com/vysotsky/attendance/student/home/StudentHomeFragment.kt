package com.vysotsky.attendance.student.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentStudentHomeBinding
import com.vysotsky.attendance.models.StudentClass
import com.vysotsky.attendance.professor.ClassAttendanceActivity
import com.vysotsky.attendance.student.StudentViewModel
import com.vysotsky.attendance.util.Resource
import com.vysotsky.attendance.util.SubjectsAdapter
import java.io.Serializable

/**
 * 1) on the first screen, the list of subjects.
 * 2) When any subject is clicked, there is a vertical list of dates,
 * bright color - attended, dim color - unattended.
 * or
 * 2) just a list of attended classes.
 */
class StudentHomeFragment : Fragment() {

    private var _binding: FragmentStudentHomeBinding? = null
    private val binding: FragmentStudentHomeBinding
        get() = _binding!!

    private val viewModel: StudentHomeViewModel by viewModels()
    private val activityViewModel: StudentViewModel by activityViewModels()

    private lateinit var subjectsAdapter: SubjectsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getClasses(activityViewModel.deviceID)
        binding.btnRetryRequest.setOnClickListener {
            viewModel.btnRetryRequestVisibility.value = false
            viewModel.getClasses(activityViewModel.deviceID, true)
        }
        setUpRecyclerView()
        subscribe()
    }

    private fun setUpRecyclerView() = binding.rv.apply {
        subjectsAdapter = SubjectsAdapter { subjectName ->
            val allSessions = viewModel.requestStatus.value?.data
            if (allSessions == null) {
                Log.e(TAG, "StudentHomeFragment onClick() classes list is null! ",)
            } else {
                val sessions = allSessions.filter{ it.subject_name == subjectName }
                val intent = Intent(requireContext(), StudentAttendanceActivity::class.java)
                intent.putExtra(StudentAttendanceActivity.EXTRA_CLASSES_KEY, sessions as Serializable)
                startActivity(intent)
            }
        }
        adapter = subjectsAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun subscribe() {
        viewModel.requestStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Error -> {
                    binding.pb.isVisible = false
                    Log.e(TAG, "error: ${it.message}")
                    viewModel.btnRetryRequestVisibility.value = true
                }
                is Resource.Loading -> {
                    binding.pb.isVisible = true
                }
                is Resource.Success -> {
                    binding.pb.isVisible = false
                    fillRecyclerView(it.data!!)
                }
            }
        }

        viewModel.tvNoItemsVisibility.observe(viewLifecycleOwner) {
            binding.tvNoItems.isVisible = it
        }

        viewModel.btnRetryRequestVisibility.observe(viewLifecycleOwner) {
            binding.btnRetryRequest.isVisible = it
        }
    }

    private fun fillRecyclerView(classes: List<StudentClass>) {
        Log.d(TAG, "got student classes: $classes")
        if (classes.isEmpty()) {
            viewModel.tvNoItemsVisibility.value = true
        } else {
            val subjects = classes.map { s -> s.subject_name }.toSet().toList()
            Log.d(TAG, "StudentHomeFragment fillRecyclerView() unique subjects = $subjects")
            subjectsAdapter.subjects = subjects
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}