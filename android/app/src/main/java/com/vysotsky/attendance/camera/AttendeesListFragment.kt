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

const val allowedDistance = 100 //100 meters

class AttendeesListFragment : Fragment() {
    private val viewModel: CameraViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val bundle = arguments
        //val list = bundle?.getStringArray("list") ?: arrayOf<String>()
        val list = getStringList(viewModel.attendeesList)
        val binding = FragmentAttendeesListBinding.inflate(inflater)
        //TODO: how to change color?
        binding.list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, list)
        return binding.root
    }

    //TODO: represent not just like string, but like View
    private fun getStringList(attendees: List<Attendee>): List<String> {
        val res = mutableListOf<String>()
        for (attendee in attendees) {
            Log.d(T, "attendee = $attendee")
            val sb = StringBuilder()
            sb.append("${attendee.firstName} ${attendee.secondName}")
            if (viewModel.isUsingGeodata) {
                if (attendee.geoLocation !== null) {
                    val results = FloatArray(3)
                    Location.distanceBetween(
                        attendee.geoLocation.latitude,
                        attendee.geoLocation.longitude,
                        viewModel.ownLocation?.latitude ?: 0.0,
                        viewModel.ownLocation?.longitude ?: 0.0,
                        results
                    )
                    Log.d(T, "${attendee.firstName} distance = ${results[0]}")
                    if (results[0] > allowedDistance) {
                        sb.append(" - out of distance range!")
                    }
                } else {
                    sb.append(" - no location data!")
                }
            }
            res += sb.toString()
        }
        return res
    }
}