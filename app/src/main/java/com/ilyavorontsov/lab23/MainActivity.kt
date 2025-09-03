package com.ilyavorontsov.lab23

import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DiffUtil.DiffResult.NO_POSITION
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.*
import okio.IOException

data class Owner (
    val login: String,
    val html_url: String,
)

data class Repository(
    val name: String,
    val owner: Owner,
    val html_url: String,
    val description: String,
    val language: String,
)

data class RequestData(
    val items: List<Repository>
)

class RepositoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tvProjectName: TextView
    var tvProjectAuthor: TextView
    var tvProjectLang: TextView
    var tvProjectDesc: TextView

    init {
        tvProjectName = itemView.findViewById(R.id.tvProjectName)
        tvProjectAuthor = itemView.findViewById(R.id.tvProjectAuthor)
        tvProjectLang = itemView.findViewById(R.id.tvProjectLang)
        tvProjectDesc = itemView.findViewById(R.id.tvProjectDesc)
    }
}

class RepositoryAdapter(val repos: MutableList<Repository>) : RecyclerView.Adapter<RepositoryHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.repo_list_item, parent, false)

        val holder = RepositoryHolder(view)
        return holder
    }

    override fun onBindViewHolder(holder: RepositoryHolder, position: Int) {

        val projectName = SpannableString(String.format(holder.itemView.context.getString(R.string.project_name), repos[position].name))
        projectName.setSpan(URLSpan(this.repos[position].html_url), 18, projectName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        projectName.setSpan(ForegroundColorSpan(Color.BLUE), 18, projectName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        holder.tvProjectName.text = projectName
        holder.tvProjectName.movementMethod = LinkMovementMethod.getInstance()

        val projectAuthor = SpannableString(String.format(holder.itemView.context.getString(R.string.project_author), repos[position].owner.login))
        projectAuthor.setSpan(URLSpan(this.repos[position].owner.html_url), 15, projectAuthor.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        projectAuthor.setSpan(ForegroundColorSpan(Color.BLUE), 15, projectAuthor.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        holder.tvProjectAuthor.text = projectAuthor
        holder.tvProjectAuthor.movementMethod = LinkMovementMethod.getInstance()

        holder.tvProjectLang.text = String.format(holder.itemView.context.getString(R.string.project_lang), repos[position].language)
        holder.tvProjectDesc.text = repos[position].description
    }

    override fun getItemCount(): Int {
        return repos.size
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etInput = findViewById(R.id.etRepoName)
        etInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (s.isNotEmpty()) {
                    val str = s.toString()
                    Log.v("URL", str)
                    val request = Request.Builder()
                        .url("https://api.github.com/search/repositories?q=${str}")
                        .build()

                    val call = client.newCall(request)

                    call.enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            // Запрос завершился с ошибкой
                        }
                        override fun onResponse(call: Call, response: Response) {
                            // Запрос завершился успешно
                            response.use {
                                if (response.isSuccessful) {
                                    val data = response.body!!.string()

                                    val t = Gson().fromJson(data, RequestData::class.java).items.toMutableList()

                                    Log.v("DATA", data)
                                    // Обработка полученных данных
                                    runOnUiThread {
                                        val adapter = RepositoryAdapter(t)

                                        val list = findViewById<RecyclerView>(R.id.rvRepoList)
                                        list.adapter = adapter
                                        list.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                                    }
                                }
                            }
                        }
                    })
                }
            }
        })
    }
}