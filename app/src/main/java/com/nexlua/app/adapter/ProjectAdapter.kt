package com.nexlua.app.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nexlua.app.R
import com.nexlua.app.data.Project
import android.view.ViewConfiguration
import com.nexlua.app.databinding.ItemProjectBinding
import kotlin.math.pow
import kotlin.math.sqrt

class ProjectAdapter(
    private val projects: List<Project>,
    private val onItemClick: (Project) -> Unit,
    private val onItemLongClick: (Project, View, Float, Float) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {
    class ProjectViewHolder(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        return ProjectViewHolder(
            ItemProjectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val binding = holder.binding
        val projectItemView = binding.root
        val context = projectItemView.context
        with(binding) {
            projectIcon.setImageDrawable(project.icon)
            projectName.text = project.name
            projectDetails.text = project.description
        }

        var capturedRawX = 0f // 用于长按菜单的绝对屏幕X坐标
        var capturedRawY = 0f // 用于长按菜单的绝对屏幕Y坐标
        var initialDownX = 0f // 用于点击检测的相对View的X坐标
        var initialDownY = 0f // 用于点击检测的相对View的Y坐标
        var touchDownTime = 0L // 用于点击检测的触摸按下时间
        projectItemView.setOnClickListener {
            onItemClick(project)
        }
        projectItemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    capturedRawX = event.rawX
                    capturedRawY = event.rawY
                    initialDownX = event.x
                    initialDownY = event.y
                    touchDownTime = System.currentTimeMillis()
                    false
                }

                MotionEvent.ACTION_UP -> {
                    val context = v.context
                    val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                    val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
                    val movedDistance = sqrt(
                        (event.x - initialDownX).toDouble().pow(2.0) +
                                (event.y - initialDownY).toDouble().pow(2.0)
                    ).toFloat()
                    val eventDuration = System.currentTimeMillis() - touchDownTime
                    val isClick = movedDistance < touchSlop && eventDuration < longPressTimeout
                    if (isClick) {
                        v.performClick()
                        return@setOnTouchListener true
                    }
                    false
                }
                else -> false
            }
        }

        projectItemView.setOnLongClickListener { v ->
            onItemLongClick(project, v, capturedRawX, capturedRawY)
            true
        }
    }

    override fun getItemCount(): Int = projects.size
}