package com.rehman.docscan.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rehman.docscan.R
import com.rehman.docscan.core.Utils.processFileName
import com.rehman.docscan.databinding.ItemImagesBinding
import com.rehman.docscan.interfaces.AdapterClickInterface

class ImagesAdapter(
    private val context: Context,
    private val adapterClickInterface: AdapterClickInterface,
    private val imagesList: ArrayList<Uri>,
) : RecyclerView.Adapter<ImagesAdapter.ImagesViewHolder>() {

    inner class ImagesViewHolder(private val binding: ItemImagesBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {

            val fileDetails = processFileName(context, uri)
            fileDetails?.let { (title, description) ->
                binding.itemTitle.text = title
                binding.itemDesc.text = description
            } ?: run {
                binding.itemTitle.text =context.getString(R.string.unknown)
                binding.itemDesc.text = context.getString(R.string.unknown)
            }

            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.ic_image_bg)
                .error(R.drawable.ic_image_bg)
                .into(binding.itemImage)

            binding.root.setOnClickListener {
                adapterClickInterface.itemClick(imagesList, adapterPosition, binding.itemImage)
            }

            binding.itemMore.setOnClickListener {
                menuClicked(it, uri, adapterPosition)
            }
        }

    }

    private fun menuClicked(view: View, uri: Uri, position: Int) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menu.add("Share")
        popupMenu.menu.add("Delete")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item?.title!!.toString()) {
                "Share" -> adapterClickInterface.shareClick(uri)
                "Delete" -> adapterClickInterface.deleteClick(uri, position)
            }
            false
        }
        popupMenu.show()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesViewHolder {
        val binding = ItemImagesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImagesViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ImagesViewHolder, position: Int) {
//        holder.itemView.setItemViewAnimation()
        holder.bind(imagesList[position])
    }

    override fun getItemCount(): Int = imagesList.size

    fun itemRemoved(uri: Uri, position: Int){
        notifyItemRemoved(position)
        imagesList.remove(uri)
    }
}
