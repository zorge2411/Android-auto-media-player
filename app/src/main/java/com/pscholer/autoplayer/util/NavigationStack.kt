package com.pscholer.autoplayer.util

import com.pscholer.autoplayer.data.models.MediaItem

class NavigationStack {
    private val stack = mutableListOf<NavigationTarget>()

    sealed class NavigationTarget {
        object Root : NavigationTarget()
        data class Browse(val parentId: String?, val scrollPosition: Int = 0) : NavigationTarget()
        data class Video(val mediaItem: MediaItem) : NavigationTarget()
        data class Playlist(val playlistId: String) : NavigationTarget()
        object Favorites : NavigationTarget()
    }

    fun push(target: NavigationTarget) {
        stack.add(target)
    }

    fun pop(): NavigationTarget? {
        return if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) else null
    }

    fun peek(): NavigationTarget? {
        return stack.lastOrNull()
    }

    fun peekPrevious(): NavigationTarget? {
        return if (stack.size >= 2) stack[stack.size - 2] else null
    }

    fun clear() {
        stack.clear()
    }

    fun size(): Int = stack.size

    fun isEmpty(): Boolean = stack.isEmpty()

    fun popToRoot(): NavigationTarget? {
        var target: NavigationTarget? = null
        while (stack.isNotEmpty() && peek() !is NavigationTarget.Root) {
            target = pop()
        }
        return target
    }
}
