package com.vysotsky.attendance.camera

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.T
import com.vysotsky.attendance.databinding.FragmentAttendeesListBinding

class AttendeesListFragment : Fragment() {
    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val bundle = arguments
        //val list = bundle?.getStringArray("list") ?: arrayOf<String>()
        val binding = FragmentAttendeesListBinding.inflate(inflater)
        //TODO: how to change color?
        val adapter = AttendeeAdapter(requireContext(), viewModel.attendeesList)
        binding.list.adapter = adapter
        binding.fab.setOnClickListener {
            //call dialog to type first and second name, then create Attendee with Status.OK
            val mock = Attendee("Example", "Example", Status.OK)
            viewModel.attendeesList += mock
            adapter.notifyDataSetChanged()
        }
        return binding.root
    }
}