package my.com.codeplay.smartlightdemo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import my.com.codeplay.smartlightdemo.BuildConfig
import my.com.codeplay.smartlightdemo.R
import my.com.codeplay.smartlightdemo.databinding.BottomSheetAddNetworkBinding

class AddNetworkBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetAddNetworkBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAddNetworkBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.setOnClickListener {
            val networkName = binding.networkNameEditText.text?.trim()
            binding.networkNameInputLayout.apply {
                if (networkName.isNullOrEmpty()) {
                    error = getString(R.string.invalid_network_name)
                    requestFocus()
                    return@setOnClickListener
                } else {
                    error = null
                }
            }

            val password = binding.passwordEditText.text?.trim()
            binding.passwordInputLayout.apply {
                if (password.isNullOrEmpty()) {
                    error = getString(R.string.invalid_network_password)
                    requestFocus()
                    return@setOnClickListener
                } else {
                    error = null
                }
            }

            setFragmentResult(
                Companion.tag, bundleOf(
                EXTRA_NETWORK_NAME to networkName.toString(),
                EXTRA_NETWORK_PASSWORD to password.toString()
            ))
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            setFragmentResult(Companion.tag, bundleOf())
            dismiss()
        }
    }

    companion object {
        val tag: String = AddNetworkBottomSheet::class.java.simpleName
        const val EXTRA_NETWORK_NAME = "${BuildConfig.APPLICATION_ID}.extra.NETWORK_NAME"
        const val EXTRA_NETWORK_PASSWORD = "${BuildConfig.APPLICATION_ID}.extra.NETWORK_PASSWORD"

        fun newInstance() = AddNetworkBottomSheet()
    }
}