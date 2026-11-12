package com.example.exoplayerdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // Khởi tạo danh sách rỗng, xóa hoàn toàn 3 video mẫu mặc định
    private val videoList = mutableListOf<VideoItem>()

    private lateinit var adapter: VideoAdapter

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Thêm video nội bộ (local) vào danh sách
            val newVideo = VideoItem(
                title = "Local Video",
                thumbnailUrl = it.toString(),
                videoUrl = it.toString()
            )
            videoList.add(newVideo)
            adapter.notifyItemInserted(videoList.size - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = VideoAdapter(videoList) { video ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("VIDEO_URL", video.videoUrl)
                // Cấp quyền đọc file cục bộ
                if (video.videoUrl.startsWith("content://")) {
                    data = Uri.parse(video.videoUrl)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddVideo).setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
    }
}