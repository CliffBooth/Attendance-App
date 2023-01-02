package com.vysotsky.attendance.professor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.databinding.FragmentStopSessionBinding

class StopSessionFragment: Fragment() {
    private var _binding: FragmentStopSessionBinding? = null
    private val binding: FragmentStopSessionBinding
        get() = _binding!!
    private val viewModel: ProfessorViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStopSessionBinding.inflate(inflater, container, false)

        binding.stopSessionButton.setOnClickListener {
            //TODO all checkboxes
            if (binding.sendEmailCheckbox.isChecked) {

            }

            val intent = Intent(requireContext(), StartSessionActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}