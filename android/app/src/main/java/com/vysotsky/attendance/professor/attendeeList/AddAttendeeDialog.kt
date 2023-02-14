package com.vysotsky.attendance.professor.attendeeList

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.vysotsky.attendance.R

class AddAttendeeDialog : DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val thisView = inflater.inflate(R.layout.add_attendee_dialog, null)
            builder.setMessage("Enter student's name")
                .setView(thisView)
                .setPositiveButton("Add") { _, _ ->
                    val firstName =
                        thisView?.findViewById<EditText>(R.id.first_name_edit_text1)?.text?.toString()
                            ?: ""
                    val secondName =
                        thisView?.findViewById<EditText>(R.id.second_name_edit_text1)?.text?.toString()
                            ?: ""

                    if (firstName.isBlank() || secondName.isBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "first name and second name fields must not be empty!",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setPositiveButton
                    }

                    setFragmentResult(
                        DIALOG_REQUEST_KEY, bundleOf(
                            FIRST_NAME_KEY to firstName,
                            SECOND_NAME_KEY to secondName
                        )
                    )
                }
                .setNegativeButton("Cancel") { _, _ -> Unit }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val DIALOG_REQUEST_KEY = "add_attendee_dialog"
        const val FIRST_NAME_KEY = "first_name"
        const val SECOND_NAME_KEY = "second_name"
    }
}