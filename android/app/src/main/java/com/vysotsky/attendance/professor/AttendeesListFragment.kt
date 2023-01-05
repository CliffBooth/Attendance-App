package com.vysotsky.attendance.professor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.databinding.FragmentAttendeesListBinding

class AttendeesListFragment : Fragment() {
    private val viewModel: ProfessorViewModel by activityViewModels()

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
        viewModel.attendeesList.adapter = adapter
        binding.list.adapter = adapter
        binding.fab.setOnClickListener {
            //call dialog to type first and second name, then create Attendee with Status.OK
            val mock = Attendee("Example", "Example", Status.OK)
            viewModel.attendeesList += mock
            adapter.notifyDataSetChanged()
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        //to avoid memory leaks viewModel must not hold reference to context
        viewModel.attendeesList.adapter = null
    }
}

/**
 * list, which can notify adapter that dataset changed
 */
class AdapterList<T> : MutableList<T> by mutableListOf() {
    var adapter: ArrayAdapter<T>? = null
    fun notifyDataSetChanged() {
        adapter?.notifyDataSetChanged()
    }
}