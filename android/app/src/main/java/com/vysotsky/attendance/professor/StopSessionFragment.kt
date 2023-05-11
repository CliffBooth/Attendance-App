package com.vysotsky.attendance.professor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.R
import com.vysotsky.attendance.databinding.FragmentStopSessionBinding
import com.vysotsky.attendance.util.Resource

class StopSessionFragment: Fragment() {
    private var _binding: FragmentStopSessionBinding? = null
    private val binding: FragmentStopSessionBinding
        get() = _binding!!
    private val viewModel: SessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStopSessionBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val email = requireContext()
            .getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getString(getString(R.string.saved_email), "error") ?: "error"
        val token = requireContext()
            .getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getString(getString(R.string.access_token), "") ?: ""
        binding.stopSessionButton.setOnClickListener {
            //TODO all checkboxes
            if (binding.sendEmailCheckbox.isChecked) {

            }
            //check if attendee list is empty and don't save the session then
            viewModel.postSession(email, token)
            viewModel.endSession(email)
        }
        binding.discardButton.setOnClickListener {
            viewModel.endSession(email)
        }
        subscribe()
    }

    private fun subscribe() {
        viewModel.postSessionStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    viewModel.isPBVisible.value = true
                    viewModel.isStopButtonEnabled.value = false
                }
                is Resource.Error -> {
                    viewModel.isPBVisible.value = false
                    Toast.makeText(
                        requireContext(),
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.isStopButtonEnabled.value = true
                }
                is Resource.Success -> {
                    viewModel.isPBVisible.value = false
//                    exitSession()
                }
            }
        }
        viewModel.endSessionStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    exitSession()
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                    exitSession()

                }
            }
        }
        viewModel.isStopButtonEnabled.observe(viewLifecycleOwner) {
            binding.stopSessionButton.isEnabled = it
        }
        viewModel.isPBVisible.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
    }

    private fun exitSession() {
        val intent = Intent(requireContext(), ProfessorHomeActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}