package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.ListItemTrainingBinding

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class TrainingAdapter(private val clickListener : TrainingListener) : ListAdapter<DataItem, RecyclerView.ViewHolder>(TrainingDiffCallback()) {

    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val trainingItem = getItem(position) as DataItem.TrainingItem
                holder.bind(trainingItem.training, clickListener)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType ${viewType}")
        }
    }

    fun addHeaderAndSubmitList(list: List<Training>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.TrainingItem(it) }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    class TextViewHolder(view: View): RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): TextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.header, parent, false)
                return TextViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int) : Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DataItem.TrainingItem -> ITEM_VIEW_TYPE_ITEM
        }
    }

    class ViewHolder private constructor (val binding: ListItemTrainingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: Training,
            clickListener: TrainingListener
        ) {
            binding.training = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTrainingBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

}

class TrainingDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }

}

class TrainingListener(val clickListener: (training: Training) -> Unit) {
    fun onClick(training: Training) = clickListener(training)
}

sealed class DataItem {
    data class TrainingItem(val training: Training): DataItem() {
        override val id = training.trainingId
    }
    object Header: DataItem() {
        override val id = Long.MIN_VALUE
    }
    abstract val id : Long
}