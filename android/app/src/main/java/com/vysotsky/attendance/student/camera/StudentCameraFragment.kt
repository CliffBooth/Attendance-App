package com.vysotsky.attendance.student.camera

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.vysotsky.attendance.R
import com.vysotsky.attendance.databinding.FragmentStudentCameraBinding
import com.vysotsky.attendance.student.StudentViewModel
import com.vysotsky.attendance.util.CameraFragment
import com.vysotsky.attendance.util.QRCodeImageAnalyzer
import com.vysotsky.attendance.util.Resource
import com.vysotsky.attendance.util.checkPermissions

class StudentCameraFragment : CameraFragment() {
    private var _binding: FragmentStudentCameraBinding? = null
    private val binding: FragmentStudentCameraBinding
        get() = _binding!!

    private val viewModel: StudentCameraViewModel by viewModels()
    private val activityViewModel: StudentViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
        )
        val permissionsGranted = checkPermissions(requireActivity(), this, permissions)
        if (!permissionsGranted) {
            Toast.makeText(
                requireContext(),
                "Can't use camera without permission!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun getListener() = object: QRCodeImageAnalyzer.QRCodeListener {
        override fun onQRCodeFound(string: String) {
            if (viewModel.status.value != Status.SEARCHING)
                return
            val data = "${activityViewModel.firstName}:${activityViewModel.secondName}:${activityViewModel.deviceID}"
            viewModel.send(string, data)
        }

        override fun onQRCodeNotFound() = Unit
    }

    override fun getSurfaceProvider(): Preview.SurfaceProvider = binding.viewFinder.surfaceProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribe()
    }

    private fun subscribe() {
        viewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.SEARCHING -> {
                    binding.statusText.text = getString(R.string.searching_for_qr_code)
                }

                Status.SENDING -> {
                    binding.statusText.text = getString(R.string.sending)
                }

                Status.SENT -> {
                    binding.statusText.text = getString(R.string.sent)
                }
            }
        }

        viewModel.requestStatus.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Loading -> {
                    viewModel.status.value = Status.SENDING
                }

                is Resource.Success -> {

                    when (it.data) {
                        200 -> {
                            viewModel.status.value = Status.SENT
                        }
                        202 -> {
                            viewModel.status.value = Status.SENT
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.you_have_already_been_scanned),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        401 -> {
                            Handler(Looper.getMainLooper()).postDelayed({
                                viewModel.status.value = Status.SEARCHING
                            }, 100)
                        }
                        else -> {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.something_went_wrong),
                                Toast.LENGTH_LONG
                            ).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                viewModel.status.value = Status.SEARCHING
                            }, 3000)
                        }
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
//                        it.message,
                        getString(R.string.network_error),
                        Toast.LENGTH_LONG
                    ).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        viewModel.status.value = Status.SEARCHING
                    }, 3000)
                }
            }
        }
    }

        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}