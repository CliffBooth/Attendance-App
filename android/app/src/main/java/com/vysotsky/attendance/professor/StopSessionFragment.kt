package com.vysotsky.attendance.professor

import android.content.Context
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
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentStopSessionBinding
import com.vysotsky.attendance.debug
import com.vysotsky.attendance.util.Resource

/**
 * Flow:
 *  when clicking "end session": calling postSession(), on result calling endSession()
 *  when clicking "discard": calling endSessoin() right away
 *
 *  if endSession() can't make network request, data is saved in the database
 */
class StopSessionFragment: Fragment() {
    private var _binding: FragmentStopSessionBinding? = null
    private val binding: FragmentStopSessionBinding
        get() = _binding!!
    private val viewModel: SessionViewModel by activityViewModels()

    private lateinit var email: String

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
        email = requireContext()
            .getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getString(getString(R.string.saved_email), "error") ?: "error"
        val token = requireContext()
            .getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            .getString(getString(R.string.access_token), "") ?: ""
        binding.stopSessionButton.setOnClickListener {
            Log.d(TAG, "onViewCreated: k")
            //check if attendee list is empty and don't save the session then
            if (viewModel.isOffline) {
                viewModel.saveSessionToDatabase(requireContext())
            } else {
                viewModel.postSession(email, token)
            }
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
                    viewModel.isPBVisible.value = true
                    viewModel.isStopButtonEnabled.value = false
                    Toast.makeText(
                        requireContext(),
//                        it.message,
                        getString(R.string.network_error),
                        Toast.LENGTH_LONG
                    ).show()

                    try {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        viewModel.saveSessionToDatabase(requireContext())
                    } catch (e: Throwable) {
                        Log.e(TAG, "StopSessionFragment: error with context: ", e)
                    }
                }
                is Resource.Success -> {
                    viewModel.isPBVisible.value = false
                    viewModel.endSession(email)
//                    viewModel.isSessionTerminated.value = true
                }
            }
        }

        viewModel.endSessionStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    Log.d(TAG, "StopSessionFragment: observe endSessionStatus: Success")
                    exitSession()
//                    viewModel.isSessionTerminated.value = true
                }
                is Resource.Error -> {
                    Log.d(TAG, "StopSessionFragment: observe endSessionStatus: Error ${it.message}")
                    exitSession()
                }

                else -> Unit
            }
        }
        viewModel.isStopButtonEnabled.observe(viewLifecycleOwner) {
            binding.stopSessionButton.isEnabled = it
        }
        viewModel.isPBVisible.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
        }
        viewModel.databaseStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    Toast.makeText(context, "Session has been saved locally", Toast.LENGTH_SHORT).show()
                    viewModel.endSession(email)
                }
                is Resource.Error -> {
                    if (context != null)
                        Toast.makeText(context, /*it.message,*/ getString(R.string.network_error), Toast.LENGTH_LONG).show()
                    viewModel.endSession(email) //TODO: maybe add a "retry" button instead of losing all data?
                }
                else -> Unit
            }
        }
        viewModel.attendeesListSize.observe(viewLifecycleOwner) {
            binding.studentCount.text = getString(R.string.attendees_number, it)
        }
    }

    private fun exitSession() {
        Log.d(TAG, "StopSessionFragment: exitSession()")
//        val intent = Intent(requireContext(), ProfessorHomeActivity::class.java)
//        startActivity(intent)
//        activity?.finish()
        viewModel.isSessionTerminated.value = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}