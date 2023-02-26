package com.vysotsky.attendance.professor

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentProfessorHomeBinding
import com.vysotsky.attendance.models.Session
import com.vysotsky.attendance.util.Resource

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
        activityViewModel.getSessions(email!!)
        subscribe()
    }

    private fun setUpRecyclerView() = binding.rv.apply {
        subjectsAdapter = SubjectsAdapter()
        adapter = subjectsAdapter
//        layoutManager = GridLayoutManager(requireContext(), 3)
        layoutManager = LinearLayoutManager(requireContext())
    }


    private fun fillRecyclerView(sessions: List<Session>) {
        if (sessions.isEmpty()) {
            activityViewModel.tvNoItemsVisibility.value = true
        } else {
            val subjects = sessions.map { s -> s.subject_name }.toSet().toList()
            subjectsAdapter.subjects = subjects
        }
    }

    private fun subscribe() {
        activityViewModel.sessions.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Error -> {
                    binding.pb.isVisible = false
                    Toast.makeText(
                        requireContext(),
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
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

        activityViewModel.tvNoItemsVisibility.observe(viewLifecycleOwner) {
            binding.tvNoItems.isVisible = it
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}