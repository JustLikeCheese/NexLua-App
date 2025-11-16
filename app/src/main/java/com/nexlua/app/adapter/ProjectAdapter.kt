package com.nexlua.app.adapter

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import com.nexlua.app.R
import com.nexlua.app.data.Project
import com.nexlua.app.databinding.ItemProjectBinding
import java.io.File
import kotlin.math.sqrt

class ProjectAdapter(
    val projects: MutableList<Project>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int, Float, Float) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        return ProjectViewHolder(
            ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val binding = holder.binding
        val itemView = binding.root
        val context = itemView.context

        // 设置图标和文本
        val iconFile = File(project.projectDirectory, project.projectIcon)
        if (iconFile.exists()) {
            binding.projectIcon.setImageBitmap(BitmapFactory.decodeFile(iconFile.absolutePath))
        } else {
            binding.projectIcon.setImageResource(R.drawable.icon)
        }
        binding.projectName.text = project.name
        binding.projectDetails.text =
            context.getString(R.string.project_info, project.versionName, project.packageName)

        // 点击 & 长按逻辑
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var initialX = 0f
        var initialY = 0f
        var isLongPress = false
        var longPressRunnable: Runnable? = null

        itemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                    initialY = event.y
                    isLongPress = false
                    longPressRunnable = Runnable {
                        isLongPress = true
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            onItemLongClick(pos, event.rawX, event.rawY)
                        }
                    }
                    itemView.postDelayed(longPressRunnable, longPressTimeout)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - initialX
                    val dy = event.y - initialY
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance > touchSlop) longPressRunnable?.let { itemView.removeCallbacks(it) }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { itemView.removeCallbacks(it) }
                    if (!isLongPress) {
                        v.performClick()
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) onItemClick(pos)
                    }
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = projects.size
}