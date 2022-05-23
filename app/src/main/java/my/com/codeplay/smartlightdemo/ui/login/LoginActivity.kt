package my.com.codeplay.smartlightdemo.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentResultListener
import com.google.android.material.snackbar.Snackbar
import com.tuya.smart.android.user.api.ILoginCallback
import com.tuya.smart.android.user.api.IValidateCallback
import com.tuya.smart.android.user.bean.User
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.sdk.api.IResultCallback
import my.com.codeplay.smartlightdemo.BuildConfig
import my.com.codeplay.smartlightdemo.MALAYSIA_COUNTRY_CODE
import my.com.codeplay.smartlightdemo.PrefsHandler
import my.com.codeplay.smartlightdemo.R
import my.com.codeplay.smartlightdemo.databinding.ActivityLoginBinding
import my.com.codeplay.smartlightdemo.ui.home.EXTRA_USERNAME
import my.com.codeplay.smartlightdemo.ui.home.MainActivity
import java.util.regex.Pattern

const val EMAIL_EXPRESSION =
    "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"

class LoginActivity : AppCompatActivity(), FragmentResultListener {
    private lateinit var binding: ActivityLoginBinding
    private var isSignUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.versionText.text = getString(
            R.string.version,
            packageManager.getPackageInfo(packageName, 0).versionName)

        binding.button.setOnClickListener {
            val email = binding.emailEditText.text?.trim()
            binding.emailInputLayout.apply {
                if (email == null || !Pattern.matches(EMAIL_EXPRESSION, email)) {
                    error = getString(R.string.invalid_email)
                    requestFocus()
                    return@setOnClickListener
                } else {
                    error = null
                }
            }

            val password = binding.passwordEditText.text?.trim()
            binding.passwordInputLayout.apply {
                if (password.isNullOrEmpty()) {
                    error = getString(R.string.invalid_password)
                    requestFocus()
                    return@setOnClickListener
                } else {
                    error = null
                }
            }

            if (isSignUp) {
                val confirmPassword = binding.confirmPasswordEditText.text?.trim()
                binding.confirmPasswordInputLayout.apply {
                    if (confirmPassword.isNullOrEmpty() or (!confirmPassword!!.contentEquals(password))) {
                        error = getString(R.string.password_not_match)
                        requestFocus()
                        return@setOnClickListener
                    } else {
                        error = null
                    }
                }
                getValidationCode(email.toString(), password.toString())
            } else {
                signIn(email.toString(), password.toString())
            }
        }

        binding.textButton.text = generateTextButtonLabel(R.string.action_sign_up)
        binding.textButton.setOnClickListener { switchFunc() }

        @SuppressLint("SetTextI18n")
        if (BuildConfig.DEBUG) {
            binding.emailEditText.setText("developer@codeplay.com.my")
            binding.passwordEditText.setText("Pw123456")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        TuyaHomeSdk.onDestroy()
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            ValidationBottomSheet.tag -> {
                if (result.getInt(ValidationBottomSheet.EXTRA_RESULT) == Activity.RESULT_OK) {
                    binding.emailEditText.text?.clear()
                    binding.passwordEditText.text?.clear()
                    binding.confirmPasswordEditText.text?.clear()
                    switchFunc()
                    Snackbar.make(binding.root, getString(R.string.account_created), Snackbar.LENGTH_LONG).show()
                }
                supportFragmentManager.clearFragmentResultListener(ValidationBottomSheet.tag)
            }
        }
    }

    private fun switchFunc() {
        isSignUp = !isSignUp
        val (buttonLabel, textButtonLabel) =
            if (isSignUp)
                Pair(R.string.action_sign_up, R.string.action_sign_in)
            else
                Pair(R.string.action_sign_in, R.string.action_sign_up)
        binding.emailInputLayout.requestFocus()
        binding.passwordEditText.imeOptions =
            if (isSignUp) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_DONE
        binding.confirmPasswordInputLayout.isVisible = isSignUp
        binding.button.text = getString(buttonLabel)
        binding.textButton.text = generateTextButtonLabel(textButtonLabel)
    }

    private fun updateUI(isSigningIn: Boolean) {
        binding.emailInputLayout.isEnabled = !isSigningIn
        binding.passwordInputLayout.isEnabled = !isSigningIn
        binding.confirmPasswordInputLayout.isEnabled = !isSigningIn
        binding.button.isInvisible = isSigningIn
        binding.button.isEnabled = !isSigningIn
        binding.loading.isVisible = isSigningIn
    }

    private fun generateTextButtonLabel(@StringRes strId: Int) =
        HtmlCompat.fromHtml(String.format("<u>%s</u>", getString(strId)), HtmlCompat.FROM_HTML_MODE_LEGACY)

    private fun getValidationCode(email: String, password: String) {
        updateUI(true)
        TuyaHomeSdk.getUserInstance()
            .getRegisterEmailValidateCode(MALAYSIA_COUNTRY_CODE, email, object : IResultCallback { // for registration
            //.getEmailValidateCode(MALAYSIA_COUNTRY_CODE, email, object : IValidateCallback { // reset password
            override fun onSuccess() {
                updateUI(false)
                if (supportFragmentManager.findFragmentByTag(ValidationBottomSheet.tag) == null) {
                    supportFragmentManager.setFragmentResultListener(
                        ValidationBottomSheet.tag,
                        this@LoginActivity,
                        this@LoginActivity
                    )
                    ValidationBottomSheet.newInstance(email, password)
                        .show(supportFragmentManager, ValidationBottomSheet.tag)
                }
            }

            override fun onError(code: String?, error: String?) {
                updateUI(false)
                Snackbar.make(binding.root, error ?: getString(R.string.get_validation_code_failed), Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun signIn(email: String, password: String) {
        updateUI(true)
        TuyaHomeSdk.getUserInstance().loginWithEmail(MALAYSIA_COUNTRY_CODE, email, password, object : ILoginCallback {
            override fun onSuccess(user: User?) {
                updateUI(false)
                user?.let {
                    PrefsHandler(this@LoginActivity).username = it.username.substringBefore("@")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java)
                        .apply {
                            putExtra(EXTRA_USERNAME, it.username.substringBefore("@"))
                        })
                } ?: Snackbar.make(binding.root, getString(R.string.login_failed), Snackbar.LENGTH_LONG).show()
            }

            override fun onError(code: String?, error: String?) {
                updateUI(false)
                Snackbar.make(binding.root, error ?: getString(R.string.login_failed), Snackbar.LENGTH_LONG).show()
            }
        })
    }
}