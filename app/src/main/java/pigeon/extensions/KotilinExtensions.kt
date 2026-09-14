package pigeon.extensions

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.ComposeText
import org.thoughtcrime.securesms.components.InputPanel
import org.thoughtcrime.securesms.longmessage.TAG

fun View.focusOnLeft() {

  if (isPigeonVersion()) {
    this.alpha = if (this.isEnabled) {
      1.0f
    } else {
      0.5f
    }
    val focus = View.OnFocusChangeListener { _, hasFocus ->
      this.post {
        this.alpha = if (this.isEnabled) {
          1.0f
        } else {
          0.5f
        }
        val params = this.layoutParams as ViewGroup.MarginLayoutParams

        if (hasFocus) {
          params.marginStart = 5
        } else {
          params.marginStart = 30
        }

        this.layoutParams = params
        this.requestLayout()

        this.getAllChildren().forEach {
          if (it.tag != "header") {
            (it as? TextView)?.setupTextSize(hasFocus)
          }
          it.setupEllipsize(hasFocus)
        }
      }
    }
    this.onFocusChangeListener = focus
  }
}

fun RecyclerView.showWhenScrolledToBottom(inputPanel: InputPanel) {
  this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      super.onScrolled(recyclerView, dx, dy)
      val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      if (position== 0) {
        inputPanel.isVisible = true
        return
      }
      inputPanel.isVisible = !canScrollVertically(1)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
      super.onScrollStateChanged(recyclerView, newState)
      val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      if (position== 0) {
        inputPanel.isVisible = true
        return
      }
      inputPanel.isVisible = !canScrollVertically(1)
    }
  })
}

fun View.getAllChildren(): List<View> {
  val result = ArrayList<View>()
  if (this !is ViewGroup) {
    result.add(this)
  } else {
    for (index in 0 until this.childCount) {
      val child = this.getChildAt(index)
      result.addAll(child.getAllChildren())
    }
  }
  return result
}

fun TextView.setupConversationStyle(hasFocus: Boolean) {
//  val animationDuration: Long = 0
//
//  val animator = if (!hasFocus) {
//    ValueAnimator.ofFloat(24f, 16f)
//  } else {
//    ValueAnimator.ofFloat(16f, 24f)
//  }
//
//  animator.duration = animationDuration
//
//  animator.addUpdateListener { valueAnimator ->
//    val animatedValue = valueAnimator.animatedValue as Float
//    this.textSize = animatedValue
//  }
//
//  animator.removeUpdateListener {
//    animator.cancel()
//    this.textSize = 16f
//  }

//  animator.start()

  this.post {
    if (hasFocus) {
      maxLines = 3
      setLineSpacing(-6f, 1f)
      textSize = 24f
    } else {
      maxLines = 2
      setLineSpacing(0f, 1f)
      textSize = 16f
    }
    this.clearAnimation()
    this.clearFocus()
  }
}

fun TextView.setupTextSize(hasFocus: Boolean) {
  val animationDuration: Long = 0

  val animator = if (hasFocus) {
    ValueAnimator.ofFloat(this.textSize, (this.textSize * 1.5).toFloat())
  } else {
    ValueAnimator.ofFloat(this.textSize, (this.textSize / 1.5).toFloat())
  }

  animator.duration = animationDuration

  animator.addUpdateListener { valueAnimator ->
    val animatedValue = valueAnimator.animatedValue as Float
    this.textSize = animatedValue
  }

  animator.start()

}

fun View.setupEllipsize(hasFocus: Boolean) {
  val color = if (hasFocus) {
    R.color.white_focus
  } else {
    R.color.white_not_focus
  }
  if (this is TextView) {
    this.setTextColor(ContextCompat.getColor(this.context, color))
  }
  if (hasFocus) {
    if (this is TextView && this !is EditText) {
      this.ellipsize = TextUtils.TruncateAt.MARQUEE
      this.isSelected = true
    }
  } else {
    if (this is TextView) {
      this.ellipsize = TextUtils.TruncateAt.END
    }
  }
}

fun View.focusOnRight() {

  if (!isSignalVersion()) {
    val focus = View.OnFocusChangeListener { _, hasFocus ->
      this.getAllChildren().forEach {
        if (it.tag != "header") {
          (it as? TextView)?.setupTextSize(hasFocus)
        }
        it.setupEllipsize(hasFocus)
      }
    }
    this.onFocusChangeListener = focus
  }
}

fun EditText.animateGroup(parent: TextView) {
  if (isPigeonVersion()) {

    val focus = View.OnFocusChangeListener { _, hasFocus ->
      this.post {
        val params = this.layoutParams as ViewGroup.MarginLayoutParams

        if (hasFocus) {
          params.marginStart = 5
        } else {
          params.marginStart = 30
        }

        layoutParams = params
        requestLayout()
      }

      parent.post {
        val params = parent.layoutParams as ViewGroup.MarginLayoutParams

        if (hasFocus) {
          params.marginStart = 5
        } else {
          params.marginStart = 30
        }

        layoutParams = params
        requestLayout()
      }

      val parentColor = if (hasFocus) {
        R.color.white_focus
      } else {
        R.color.white_not_focus
      }

      if (this.tag != "header") {
        (this as? TextView)?.setupTextSize(hasFocus)
      }
      this.setupEllipsize(hasFocus)
      (this as? TextView)?.setTextColor(ContextCompat.getColor(this.context, parentColor))

      if (parent.tag != "header") {
        (parent as? TextView)?.setupTextSize(hasFocus)
      }
      parent.setupEllipsize(hasFocus)
      (parent as? TextView)?.setTextColor(ContextCompat.getColor(this.context, parentColor))
    }
    this.onFocusChangeListener = focus
  }
}

fun View.focusColor(vararg childs: TextView) {
  if (!isSignalVersion()) {
    val focus = View.OnFocusChangeListener { _, hasFocus ->
      this.setupEllipsize(hasFocus)
      childs.forEach { it.setupEllipsize(hasFocus) }
    }
    this.onFocusChangeListener = focus
  }
}

fun Context.cancelNotifications() {
  NotificationManagerCompat.from(this).cancelAll()
  val notificationManager = (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
  notificationManager.cancelAll()
  val intent = Intent()
  intent.action = "clear.notification.from.signal"
  sendBroadcast(intent)
}
