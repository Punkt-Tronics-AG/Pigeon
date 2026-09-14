package org.thoughtcrime.securesms.pigeon.activity

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.thoughtcrime.securesms.R

class ConversationSubMenuActivity : Activity(), ConversationSubMenuAdapter.ItemClickListener {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.conversation_sub_menu_view)

    val recyclerView = findViewById<RecyclerView>(R.id.conversation_sub_menu_recycler)
    val data = arrayListOf(
      resources.getString(R.string.conversation_context__menu_reply_to_message),
      resources.getString(R.string.conversation_context__menu_forward_message),
      //resources.getString(R.string.conversation_context__menu_react_to_message),
      resources.getString(R.string.conversation_context__menu_take_back_message),
    )

    val menuAdapter = ConversationSubMenuAdapter(this, this, data)
    recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    recyclerView.adapter = menuAdapter
  }

  override fun onItemClick(position: Int) {
    val result = when (position) {
      0 -> HANDLE_REPLY_MESSAGE
      1 -> HANDLE_FORWARD
      //2 -> HANDLE_REACT
      2 -> HANDLE_TAKE_BACK_MESSAGE
      else -> -1
    }
    setResult(result)
    finish()
  }

  companion object {
    const val HANDLE_REPLY_MESSAGE = 100
    const val HANDLE_FORWARD = 200
    const val HANDLE_REACT = 300
    const val HANDLE_TAKE_BACK_MESSAGE = 400
    const val HANDLE_SUBMENU = 1001
  }
}