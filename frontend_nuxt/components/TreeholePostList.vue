<template>
  <div class="treehole-list">
    <div v-if="pending" class="treehole-loading">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <BasePlaceholder v-else-if="posts.length === 0" :text="emptyText" icon="Inbox" />
    <div
      v-else
      v-for="post in posts"
      :key="post.id"
      class="treehole-item"
      @click="goPost(post)"
    >
      <div class="treehole-item-header">
        <div class="treehole-author">
          <BaseUserAvatar
            :src="post.author?.avatar"
            :user-id="post.author?.id"
            :disable-link="true"
            :width="32"
          />
          <div class="treehole-author-meta">
            <div class="treehole-author-name">{{ post.author?.username || '珞珈匿名' }}</div>
            <div class="treehole-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>
        <span
          v-if="post.treeholeReviewStatus"
          class="treehole-status"
          :class="getTreeholeStatusClass(post.treeholeReviewStatus)"
        >
          {{ getTreeholeStatusText(post.treeholeReviewStatus) }}
        </span>
      </div>

      <div class="treehole-title">{{ post.title }}</div>
      <div class="treehole-content">{{ snippet(post.content) }}</div>

      <div class="treehole-footer">
        <div class="treehole-taxonomy">
          <ArticleCategory v-if="post.category" :category="post.category" />
          <ArticleTags :tags="post.tags || []" />
        </div>
        <div class="treehole-stats">
          <span>{{ post.commentCount || 0 }} 回复</span>
          <span>{{ post.views || 0 }} 浏览</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import ArticleCategory from '~/components/ArticleCategory.vue'
import ArticleTags from '~/components/ArticleTags.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import TimeManager from '~/utils/time'
import { stripMarkdown } from '~/utils/markdown'
import { getTreeholeStatusClass, getTreeholeStatusText } from '~/utils/treehole'

defineProps({
  posts: { type: Array, default: () => [] },
  pending: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无树洞' },
})

const formatTime = (value) => TimeManager.format(value)

const snippet = (content) => {
  const text = stripMarkdown(content || '').replace(/\s+/g, ' ').trim()
  return text.length > 220 ? `${text.slice(0, 220)}...` : text
}

const goPost = (post) => {
  if (!post?.id) return
  navigateTo(`/posts/${post.id}`)
}
</script>

<style scoped>
.treehole-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.treehole-loading {
  display: flex;
  justify-content: center;
  padding: 36px 0;
}

.treehole-item {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  padding: 16px;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease;
}

.treehole-item:hover {
  border-color: var(--primary-color);
  transform: translateY(-1px);
}

.treehole-item-header,
.treehole-footer,
.treehole-taxonomy,
.treehole-stats,
.treehole-author {
  display: flex;
  align-items: center;
}

.treehole-item-header {
  justify-content: space-between;
  gap: 12px;
}

.treehole-author {
  gap: 10px;
  min-width: 0;
}

.treehole-author-meta {
  min-width: 0;
}

.treehole-author-name {
  color: var(--text-color);
  font-size: 14px;
  font-weight: 600;
}

.treehole-time {
  color: var(--text-color-secondary);
  font-size: 12px;
  margin-top: 2px;
}

.treehole-status {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  border: 1px solid var(--normal-border-color);
}

.treehole-status.public {
  color: #047857;
  background: rgba(16, 185, 129, 0.1);
}

.treehole-status.reviewing,
.treehole-status.admin {
  color: #8a5a00;
  background: rgba(245, 158, 11, 0.12);
}

.treehole-status.private {
  color: #334155;
  background: rgba(100, 116, 139, 0.12);
}

.treehole-status.restricted,
.treehole-status.reported {
  color: #b42318;
  background: rgba(244, 63, 94, 0.12);
}

.treehole-title {
  color: var(--text-color);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
  margin-top: 14px;
  overflow-wrap: anywhere;
}

.treehole-content {
  color: var(--text-color);
  font-size: 14px;
  line-height: 1.65;
  margin-top: 8px;
  overflow-wrap: anywhere;
}

.treehole-footer {
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.treehole-taxonomy {
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.treehole-stats {
  gap: 10px;
  color: var(--text-color-secondary);
  font-size: 12px;
  flex-shrink: 0;
}

@media (max-width: 700px) {
  .treehole-item {
    padding: 14px;
  }

  .treehole-item-header,
  .treehole-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
