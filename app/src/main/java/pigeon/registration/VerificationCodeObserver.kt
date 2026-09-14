package pigeon.registration

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import androidx.core.content.ContextCompat
import java.util.regex.Pattern

class VerificationCodeObserver(private val mContext: Context, private val mHandler: Handler, received_code: Int) : ContentObserver(mHandler) {
    private var mReceivedCode = 1

    init {
        mReceivedCode = received_code
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        var code = ""
        if (uri.toString() == "content://sms/raw") {
            return
        }
        val inboxUri = Uri.parse("content://sms/inbox")
        val c = mContext.contentResolver.query(inboxUri, null, null, null, "date desc")
        if (c != null) {
            if (c.moveToFirst()) {
                val body = c.getString(c.getColumnIndexOrThrow("body"))
                val pattern = Pattern.compile("SIGNAL.+(\\d{3}-?\\d{3})")
                val matcher = pattern.matcher(body)
                if (matcher.find()) {
                    code = matcher.group(1) ?: return
                    val result = code.replace("-".toRegex(), "")
                    mHandler.obtainMessage(mReceivedCode, result).sendToTarget()
                }
            }
            c.close()
        }
    }
}