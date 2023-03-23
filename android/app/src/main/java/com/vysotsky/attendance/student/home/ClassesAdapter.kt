package com.vysotsky.attendance.student.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vysotsky.attendance.databinding.StudentClassListItemBinding

/**
 * For displaying all the subject's classes for student, after they click on subject name
 *
 * has list of strings - dates
 */
class ClassesAdapter : RecyclerView.Adapter<ClassesAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(val binding: StudentClassListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    private val diffCallback = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    var classes: List<String>
        get() = differ.currentList
        set(value) {
            differ.submitList(value)
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassViewHolder {
        return ClassViewHolder(StudentClassListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.binding.date.text = classes[position]
    }

    override fun getItemCount() = classes.size
}