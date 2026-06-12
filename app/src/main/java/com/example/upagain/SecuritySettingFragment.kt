package com.example.upagain

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.example.upagain.databinding.FragmentSecuritySettingBinding
import com.example.upagain.model.SecurityData

class SecuritySettingFragment : Fragment() {
    private var _binding: FragmentSecuritySettingBinding? = null
    private val binding get() = _binding!!
    private var securityData: SecurityData? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            securityData = it.getParcelable(ARG_SECURITY_DATA, SecurityData::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecuritySettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        securityData?.let { data ->
            binding.etSecurityEmail.setText(data.email)
            binding.etSecurityPassword.setText(data.password)
        }

        binding.btnSaveSecurity.setOnClickListener {
            // TODO: call viewmodel to update email + password
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SECURITY_DATA = "ARG_SECURITY_DATA"

        @JvmStatic
        fun newInstance(data: SecurityData) =
            SecuritySettingFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SECURITY_DATA, data)
                }
            }
    }
}