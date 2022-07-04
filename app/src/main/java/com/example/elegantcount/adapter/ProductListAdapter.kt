package com.example.elegantcount.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.elegantcount.R
import com.example.elegantcount.model.Product
import kotlinx.android.synthetic.main.list_item_product.view.*

class ProductListAdapter : RecyclerView.Adapter<ProductListAdapter.VenueViewHolder>(){
    
    inner class VenueViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private val differCallback = object : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.pId == newItem.pId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        return VenueViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item_product,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {

        val product = differ.currentList[position]

        holder.itemView.apply{
            t_id.text = product.pId
            t_name.text = product.name
            t_periodNo.text = product.periodNo
            t_expire.text = product.expireDate
            t_boxNumber.text = product.boxNumber
            i_close.setOnClickListener {
                onItemCLickListener?.let {
                    it(product)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onItemCLickListener: ((Product) -> Unit)? = null

    fun setOnItemClickListener(listener: (Product) -> Unit){
        onItemCLickListener = listener
    }
}