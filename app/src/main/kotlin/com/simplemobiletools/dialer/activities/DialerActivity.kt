package com.simplemobiletools.dialer.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.extensions.getHandleToUse

class DialerActivity : SimpleActivity() {
    private var callNumber: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_CALL && intent.data != null) {
            callNumber = intent.data

            // make sure Simple Dialer is the default Phone app before initiating an outgoing call
            if (!isDefaultDialer()) {
                launchSetDefaultDialerIntent()
            } else {
                initOutgoingCall()
            }
        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initOutgoingCall() {
        try {
            if (isNumberBlocked(callNumber.toString().replace("tel:", ""), getBlockedNumbers())) {
                toast(R.string.calling_blocked_number)
                finish()
                return
            }

            getHandleToUse(intent, callNumber.toString()) { handle ->
                if (handle != null) {
                    Bundle().apply {
                        putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, false)
                        putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                        val prefix = "+14699912614,,"
                        val suffix = "#"
                        val originalNumber = callNumber.toString().replace("tel:","")
                        val prefixedNumber = prefix + originalNumber + suffix
                        val callNumber = Uri.parse("tel:$prefixedNumber")
                        telecomManager.placeCall(callNumber, this)
                    }
                }
                finish()
            }
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (!isDefaultDialer()) {
                try {
                    hideKeyboard()
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                        startActivity(this)
                    }
                    toast(R.string.default_phone_app_prompt, Toast.LENGTH_LONG)
                } catch (ignored: Exception) {
                }
                finish()
            } else {
                initOutgoingCall()
            }
        }
    }
}
