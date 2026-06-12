package com.example.upagain

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.upagain.databinding.FragmentSecuritySettingBinding

class SecuritySettingFragment : Fragment() {
    private var _binding: FragmentSecuritySettingBinding? = null
    private val binding get() = _binding!!
    private var userEmail: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userEmail = it.getString(ARG_USER_EMAIL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecuritySettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etSecurityEmail.setText(userEmail)

        binding.btnSaveSecurity.setOnClickListener {
            // TODO: call viewmodel to update email + password
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_EMAIL = "ARG_USER_EMAIL"

        @JvmStatic
        fun newInstance(email: String) =
            SecuritySettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_EMAIL, email)
                }
            }
    }
}