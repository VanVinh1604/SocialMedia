import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmedia.R
import com.example.socialmedia.databinding.ItemUserBinding
import com.example.socialmedia.project.Domain.Model.UserModel

class UserLikedAdapter(private val userList: List<UserModel>) :
    RecyclerView.Adapter<UserLikedAdapter.UserViewHolder>() {

    inner class UserViewHolder(val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val b = holder.binding

        b.tvFullName.text = user.fullName

        Glide.with(b.root.context)
            .load(user.profilePictureUrl ?: R.drawable.image_avata_user)
            .placeholder(R.drawable.image_avata_user)
            .circleCrop()
            .into(b.imgProfile)
    }

    override fun getItemCount(): Int = userList.size
}
