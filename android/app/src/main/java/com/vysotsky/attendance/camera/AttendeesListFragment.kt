package com.vysotsky.attendance.camera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.databinding.FragmentAttendeesListBinding

class AttendeesListFragment : Fragment() {
    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bundle = arguments
        //val list = bundle?.getStringArray("list") ?: arrayOf<String>()
        val list = viewModel.attendeesList
        val binding = FragmentAttendeesListBinding.inflate(inflater)
        binding.list.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, list)
        return binding.root
    }
}