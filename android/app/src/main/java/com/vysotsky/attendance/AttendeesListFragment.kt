package com.vysotsky.attendance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.vysotsky.attendance.R
import com.vysotsky.attendance.databinding.FragmentAttendeesListBinding

class AttendeesListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bundle = arguments
        val list = bundle!!.getStringArray("list")!!
        val binding = FragmentAttendeesListBinding.inflate(inflater)
        binding.list.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, list)
        return binding.root
    }
}