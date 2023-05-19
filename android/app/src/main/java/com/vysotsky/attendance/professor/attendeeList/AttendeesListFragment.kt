package com.vysotsky.attendance.professor.attendeeList

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vysotsky.attendance.R
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.FragmentAttendeesListBinding
import com.vysotsky.attendance.professor.SessionViewModel
import com.vysotsky.attendance.professor.attendeeList.AddAttendeeDialog.Companion.DIALOG_REQUEST_KEY
import com.vysotsky.attendance.professor.attendeeList.AddAttendeeDialog.Companion.FIRST_NAME_KEY
import com.vysotsky.attendance.professor.attendeeList.AddAttendeeDialog.Companion.SECOND_NAME_KEY

class AttendeesListFragment : Fragment() {
    private val viewModel: SessionViewModel by activityViewModels()
    private lateinit var adapter: AttendeeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAttendeesListBinding.inflate(inflater)
        val email = context
            ?.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
            ?.getString(getString(R.string.saved_email), "error") ?: "error"

        adapter = AttendeeAdapter(requireContext(), viewModel.attendeesList) { attendee ->
            viewModel.deleteAttendee(email, attendee)
        }

        viewModel.attendeesList.adapter = adapter
        binding.list.adapter = adapter

        childFragmentManager.setFragmentResultListener(
            DIALOG_REQUEST_KEY,
            viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            Log.d(TAG, "AttendeeListFragment: inside of FragmentResultListener")
            val firstName = bundle.getString(FIRST_NAME_KEY)
            val secondName = bundle.getString(SECOND_NAME_KEY)
            if (firstName == null || secondName == null) {
                return@setFragmentResultListener
            }
            val attendee = Attendee(firstName, secondName, null, Status.OK)

            viewModel.addAttendeeToList(attendee)
            viewModel.saveAttendee(email, attendee)
        }

        binding.fab.setOnClickListener {
            val dialog = AddAttendeeDialog()
//            setFragmentResult()
            dialog.show(childFragmentManager, "dialog")
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