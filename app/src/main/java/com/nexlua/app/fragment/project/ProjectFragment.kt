package com.nexlua.app.fragment.project

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nexlua.app.R
import com.nexlua.app.adapter.ProjectAdapter
import com.nexlua.app.data.Project
import com.nexlua.app.databinding.FragmentProjectBinding
import com.nexlua.app.widget.MyPopupMenu

class ProjectFragment : Fragment() {
    private var _binding: FragmentProjectBinding? = null
    private val binding get() = _binding!!
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

//        projectAdapter = ProjectAdapter(
//            projects = null,
//            onItemClick = { clickedProject ->
//                Toast.makeText(context, "点击了 ${clickedProject.name}", Toast.LENGTH_SHORT).show()
//            },
//            onItemLongClick = { longClickedProject, anchorViewForMenu, rawX, rawY ->
//                showProjectContextMenu(longClickedProject, rawX, rawY)
//            }
//        )
//
//        binding.recyclerViewProjects.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = projectAdapter
//        }
    }

    fun showProjectContextMenu(
        project: Project,
        touchX: Float,
        touchY: Float
    ) {
        val activity = context as Activity
        val decorView = activity.window.decorView as ViewGroup
        val anchorView = View(activity).apply { x = touchX;y = touchY }
        val layoutParams = FrameLayout.LayoutParams(1, 1, Gravity.START or Gravity.TOP)
        decorView.addView(anchorView, layoutParams)
        val popupMenu = MyPopupMenu(activity, anchorView)
        popupMenu.setForceShowIcon(true)
        popupMenu.add("打开", R.drawable.ic_folder_open, {
            Toast.makeText(activity, "点击了 ${project.name}", Toast.LENGTH_SHORT).show()
            true
        })
        popupMenu.add("分享", R.drawable.ic_share_outline, {
            Toast.makeText(activity, "点击了 ${project.name}", Toast.LENGTH_SHORT).show()
            true
        })
        popupMenu.add("删除", R.drawable.ic_delete_outline, {
            Toast.makeText(activity, "点击了 ${project.name}", Toast.LENGTH_SHORT).show()
            true
        })
        popupMenu.setOnDismissListener { decorView.removeView(anchorView) }
        popupMenu.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}