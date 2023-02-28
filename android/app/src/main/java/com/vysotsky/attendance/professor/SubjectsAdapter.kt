package com.vysotsky.attendance.professor

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vysotsky.attendance.TAG
import com.vysotsky.attendance.databinding.SubjectItemBinding

/**
 * list of subjects displayed on professor's home screen.
 */
class SubjectsAdapter(val onClick: (subjectName: String) -> Unit) : RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(val binding: SubjectItemBinding) :
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
    var subjects: List<String>
        get() = differ.currentList
        set(value) {
            differ.submitList(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        return SubjectViewHolder(SubjectItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.binding.apply {
            val subjectName = subjects[position]
            tvSubjectName.text = subjectName
        }
        //need to write the listener function from the outside, in order to have the access to the whole list of items.
        //at the same time, need to have access to the name
        //so on the outside we can write callback that takes in a name. and in here
        holder.binding.root.setOnClickListener {
            onClick(subjects[position])
            Log.d(TAG, "subjectItem clicked() $position) ${subjects[position]}")
        }
    }

    override fun getItemCount() = subjects.size
}