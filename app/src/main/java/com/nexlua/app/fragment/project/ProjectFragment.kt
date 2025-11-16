package com.nexlua.app.fragment.project

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nexlua.app.R
import com.nexlua.app.activity.EditorActivity
import com.nexlua.app.adapter.ProjectAdapter
import com.nexlua.app.data.Project
import com.nexlua.app.databinding.FragmentProjectBinding
import com.nexlua.app.data.ProjectManager
import com.nexlua.app.widget.MyPopupMenu
import es.dmoral.toasty.Toasty

class ProjectFragment : Fragment() {
    private var _binding: FragmentProjectBinding? = null
    private val binding get() = _binding!!
    private lateinit var projects: MutableList<Project>
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projects = ProjectManager.listProjects()
        projectAdapter = ProjectAdapter(
            projects = projects,
            onItemClick = { position ->
                val project = projects[position]
                openProject(project)
            },
            onItemLongClick = { position, touchX, touchY ->
                val project = projects[position]
                val decorView = (context as Activity).window.decorView as ViewGroup
                val anchorView = View(activity).apply { x = touchX; y = touchY }
                val layoutParams = FrameLayout.LayoutParams(1, 1, Gravity.START or Gravity.TOP)
                decorView.addView(anchorView, layoutParams)

                val popupMenu = MyPopupMenu(requireContext(), anchorView)
                popupMenu.setForceShowIcon(true)
                popupMenu.add("打开", R.drawable.ic_folder_open) {
                    openProject(project)
                    true
                }
                popupMenu.add("分享", R.drawable.ic_share_outline) {
                    Toast.makeText(
                        activity,
                        "分享 ${project.name}",
                        Toast.LENGTH_SHORT
                    ).show(); true
                }
                popupMenu.add("删除", R.drawable.ic_delete_outline) {
                    val dialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle("删除项目")
                        .setMessage("确定删除 ${project.name} 吗？")
                        .setPositiveButton("确定") { _, _ ->
                            val index = projects.indexOf(project)
                            if (index != -1) {
                                project.projectDirectory.deleteRecursively()
                                projects.removeAt(index)
                                projectAdapter.notifyItemRemoved(index)
                                Toasty.success(requireContext(), "项目删除成功").show()
                            }
                        }
                        .setNegativeButton("取消", null)
                    dialog.show()
                    true
                }
                popupMenu.setOnDismissListener { decorView.removeView(anchorView) }
                popupMenu.show()
            }
        )
        binding.recyclerViewProjects.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = projectAdapter
        }
    }

    fun updateProjects() {
        projects.clear()
        projects.addAll(ProjectManager.listProjects())
        projectAdapter.notifyDataSetChanged()
    }

    fun openProject(project: Project) {
        val intent = Intent(activity, EditorActivity::class.java)
        intent.putExtra("path", project.projectFile.absolutePath)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateProjects()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
