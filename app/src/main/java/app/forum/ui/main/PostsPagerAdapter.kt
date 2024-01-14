package app.forum.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import app.forum.R

private val TAB_TITLES = arrayOf(
        R.string.latest_posts,
        R.string.popular_posts
)

private val COMMENTS_TYPES = arrayOf(
    "latest",
    "popular"
)

class PostsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return PostsFragment.newInstance(COMMENTS_TYPES[position])
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }
}