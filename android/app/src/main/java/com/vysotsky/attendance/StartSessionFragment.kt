package com.vysotsky.attendance

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.vysotsky.attendance.databinding.FragmentStartSessionBinding

class StartSessionFragment : Fragment() {
    private var _binding: FragmentStartSessionBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(T, "StartSessionFragment onCreate()")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStartSessionBinding.inflate(inflater, container, false)
        binding.startButton.setOnClickListener {
            //...
        }
        binding.startButton.setOnClickListener {
            //TODO: make API call
            //... start CameraFragment
            parentFragmentManager.commit {
                add<CameraFragment>(R.id.fragment_container_view)
                setReorderingAllowed(true)
                addToBackStack("CameraFragment")
            }
        }
        Log.d(T, "StartSessionFragment onCreateView()")
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}