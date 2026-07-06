package com.example.upagain.feat.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.upagain.R
import com.example.upagain.databinding.FragmentDashboardBinding
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_JUST_LOGGED_IN = "key_just_logged_in"

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var justLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            justLoggedIn = it.getBoolean(ARG_JUST_LOGGED_IN)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (justLoggedIn) {
            binding.main.showTopSnackbar(R.string.login_success, SnackbarLevel.SUCCESS)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param ARG_JUST_LOGGED_IN if the user just logged in or not.
         * @return A new instance of fragment DashboardFragment.
         */
        @JvmStatic
        fun newInstance(justLoggedIn: Boolean) =
            DashboardFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_JUST_LOGGED_IN, justLoggedIn)
                }
            }
    }
}