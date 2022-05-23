package my.com.codeplay.smartlightdemo.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tuya.smart.android.user.api.IRegisterCallback
import com.tuya.smart.android.user.bean.User
import com.tuya.smart.home.sdk.TuyaHomeSdk
import my.com.codeplay.smartlightdemo.BuildConfig
import my.com.codeplay.smartlightdemo.MALAYSIA_COUNTRY_CODE
import my.com.codeplay.smartlightdemo.R
import my.com.codeplay.smartlightdemo.databinding.BottomSheetValidationBinding

class ValidationBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetValidationBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetValidationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.setOnClickListener {
            val code = binding.validationCodeEditText.text?.trim()
            binding.validationCodeInputLayout.apply {
                if (code.isNullOrEmpty()
                    || code.length < resources.getInteger(R.integer.max_email_validation_code_length)) {
                    error = getString(R.string.invalid_validation_code)
                    requestFocus()
                    return@setOnClickListener
                } else {
                    error = null
                }
            }
            register(code.toString())
        }

        binding.cancelButton.setOnClickListener {
            setFragmentResult(
                Companion.tag,
                bundleOf(EXTRA_RESULT to Activity.RESULT_CANCELED))
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.validationCodeInputLayout.requestFocus()
    }

    private fun updateUI(isRegistering: Boolean) {
        binding.validationCodeInputLayout.isEnabled = !isRegistering
        binding.cancelButton.isInvisible = isRegistering
        binding.okButton.isInvisible = isRegistering
        binding.loading.isVisible = isRegistering
    }

    private fun register(code: String) {
        updateUI(true)
        TuyaHomeSdk.getUserInstance()
            .registerAccountWithEmail(
            MALAYSIA_COUNTRY_CODE,
            requireArguments().getString(ARG_EMAIL),
            requireArguments().getString(ARG_PASSWORD),
            code,
            object : IRegisterCallback {
                override fun onSuccess(user: User?) {
                    updateUI(false)
                    setFragmentResult(
                        Companion.tag,
                        bundleOf(EXTRA_RESULT to Activity.RESULT_OK))
                    dismiss()
                }

                override fun onError(code: String?, error: String?) {
                    updateUI(false)
                    binding.validationCodeInputLayout.error = error
                }
            })
            /* To reset password.
            .resetEmailPassword(
                MALAYSIA_COUNTRY_CODE,
                requireArguments().getString(ARG_EMAIL),
                code,
                requireArguments().getString(ARG_PASSWORD),
                object : IResetPasswordCallback {
                    override fun onSuccess() {
                        updateUI(false)
                        setFragmentResult(ValidationBottomSheet.tag,
                            bundleOf(EXTRA_RESULT to Activity.RESULT_OK))
                        dismiss()
                    }

                    override fun onError(code: String?, error: String?) {
                        updateUI(false)
                        binding.validationCodeInputLayout.error = error
                    }
                }
            )
            // */
    }

    companion object {
        val tag: String = ValidationBottomSheet::class.java.simpleName
        const val EXTRA_RESULT = "${BuildConfig.APPLICATION_ID}.extra.RESULT"
        private const val ARG_EMAIL = "arg_email"
        private const val ARG_PASSWORD = "arg_password"

        fun newInstance(email: String, password: String) = ValidationBottomSheet().apply {
            arguments = bundleOf(
                ARG_EMAIL to email,
                ARG_PASSWORD to password
            )
            isCancelable = false
        }
    }
}