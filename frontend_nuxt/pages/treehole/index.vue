<template>
  <div class="treehole-page">
    <section class="treehole-hero">
      <div>
        <div class="treehole-kicker">匿名树洞</div>
        <h1>把难以署名的话放在这里</h1>
        <p>树洞内容默认匿名。公开内容会先进入审核，通过后再对其他用户展示。</p>
      </div>
      <div class="treehole-actions">
        <NuxtLink class="treehole-action primary" to="/treehole/new">
          <edit class="treehole-action-icon" />
          发布树洞
        </NuxtLink>
        <NuxtLink class="treehole-action" to="/treehole/square">
          <message-one class="treehole-action-icon" />
          进入广场
        </NuxtLink>
        <NuxtLink v-if="isLogin" class="treehole-action" to="/treehole/mine">
          <user-icon class="treehole-action-icon" />
          我的树洞
        </NuxtLink>
      </div>
    </section>

    <section class="treehole-section">
      <div class="treehole-section-title">发布规则</div>
      <div class="treehole-rule-grid">
        <div class="treehole-rule">
          <span class="treehole-rule-index">01</span>
          <div>
            <h2>前台匿名</h2>
            <p>树洞帖统一以匿名身份展示，不显示真实账号。</p>
          </div>
        </div>
        <div class="treehole-rule">
          <span class="treehole-rule-index">02</span>
          <div>
            <h2>延迟公开</h2>
            <p>选择公开后，审核中只有你能在广场看到。</p>
          </div>
        </div>
        <div class="treehole-rule">
          <span class="treehole-rule-index">03</span>
          <div>
            <h2>可只给自己</h2>
            <p>仅自己可见的树洞会保存到“我的树洞”。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { authState } from '~/utils/auth'

useHead({ title: '匿名树洞 - 珞珈论坛' })

const isLogin = computed(() => authState.loggedIn)
</script>

<style scoped>
.treehole-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 28px 18px 48px;
}

.treehole-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid var(--normal-border-color);
  padding-bottom: 24px;
}

.treehole-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
}

.treehole-hero h1 {
  color: var(--text-color);
  font-size: 30px;
  line-height: 1.25;
  margin: 0;
}

.treehole-hero p,
.treehole-rule p {
  color: var(--text-color-secondary);
  line-height: 1.7;
  margin: 10px 0 0;
}

.treehole-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 180px;
}

.treehole-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  text-decoration: none;
  padding: 0 14px;
}

.treehole-action.primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.treehole-action-icon {
  font-size: 18px;
}

.treehole-section {
  margin-top: 26px;
}

.treehole-section-title {
  color: var(--text-color);
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.treehole-rule-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.treehole-rule {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 16px;
}

.treehole-rule-index {
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 700;
}

.treehole-rule h2 {
  color: var(--text-color);
  font-size: 16px;
  margin: 8px 0 0;
}

@media (max-width: 760px) {
  .treehole-hero {
    flex-direction: column;
  }

  .treehole-actions {
    min-width: 0;
  }

  .treehole-rule-grid {
    grid-template-columns: 1fr;
  }
}
</style>
