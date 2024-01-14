package app.forum.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import app.forum.R

private val TAB_TITLES = arrayOf(
        R.string.latest_comments,
        R.string.popular_comments
)

class CommentsPagerAdapter(private val context: Context, fm: FragmentManager, private val postId: String)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val type = if (position == 0) CommentsFragment.Types.LATEST else CommentsFragment.Types.POPULAR
        return CommentsFragment.newInstance(postId, type)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }
}