<template>
  <div class="about-page">
    <BaseTabs v-model="selectedTab" :tabs="tabs">
      <template v-if="selectedTab === 'api'">
        <div class="about-api">
          <div class="about-api-title">调试Token</div>
          <div v-if="!authState.loggedIn" class="about-api-login">
            请<NuxtLink to="/login" class="about-api-login-link">登录</NuxtLink>后查看 Token
          </div>
          <div v-else class="about-api-token">
            <div class="token-row">
              <span class="token-text">{{ shortToken }}</span>
              <span @click="copyToken"><copy class="copy-icon" /></span>
            </div>
            <div class="warning-row">
              <info-icon class="warning-icon" />
              <div class="token-warning">请不要将 Token 泄露给他人</div>
            </div>
          </div>
          <div class="about-api-title">API文档和调试入口</div>
          <a :href="apiDocsUrl" target="_blank" rel="noopener" class="about-api-link">
            API 文档与 Playground <share />
          </a>
        </div>
      </template>
      <template v-else>
        <div class="about-loading" v-if="isFetching">
          <l-hatch-spinner size="100" stroke="10" speed="1" color="var(--primary-color)" />
        </div>
        <div
          v-else
          class="about-content"
          v-html="renderMarkdown(content)"
          @click="handleContentClick"
        ></div>
      </template>
    </BaseTabs>
  </div>
</template>

<script>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from '#imports'
import { authState, getToken } from '~/utils/auth'
import { handleMarkdownClick, renderMarkdown } from '~/utils/markdown'
import BaseTabs from '~/components/BaseTabs.vue'
import { toast } from '~/composables/useToast'

export default {
  name: 'AboutPageView',
  setup() {
    const isFetching = ref(false)
    const config = useRuntimeConfig()
    const apiDocsUrl = computed(() => `${config.public.apiBaseUrl}/api/v3/api-docs`)
    const tabs = [
      {
        key: 'about',
        label: '关于',
        content: `# 关于珞珈论坛

珞珈论坛是面向武汉大学师生和校友的校园交流社区。你可以在这里讨论课程与科研、分享校园生活、发布活动信息、进行跳蚤市场交易，也可以通过匿名树洞寻求建议。

论坛仅支持使用 @whu.edu.cn 邮箱注册，登录方式为已注册邮箱与密码。`,
      },
      {
        key: 'agreement',
        label: '用户协议',
        content: `# 用户协议

使用珞珈论坛代表你同意遵守武汉大学校园社区的基本规范：尊重他人、保护隐私、理性表达，并对自己发布的内容负责。

请勿发布违法违规、侵权、骚扰、诈骗、恶意引流或泄露他人个人信息的内容。`,
      },
      {
        key: 'guideline',
        label: '创作准则',
        content: `# 创作准则

鼓励发布真实、清晰、对同学有帮助的内容。课程讨论请尽量提供背景信息，求助帖请说明已尝试的方法，交易帖请标注价格、地点与状态。

匿名发言仍需遵守社区规则。`,
      },
      {
        key: 'privacy',
        label: '隐私政策',
        content: `# 隐私政策

珞珈论坛仅收集账号注册、登录、内容发布和通知所必需的信息。邮箱用于账号识别、验证和找回密码。

请不要在公开帖子、评论或私信中泄露身份证号、学号、手机号、宿舍地址等敏感信息。`,
      },
      {
        key: 'api',
        label: 'API与调试',
      },
    ]
    const route = useRoute()
    const router = useRouter()
    const selectedTab = ref(tabs[0].key)
    const content = ref('')
    const token = computed(() => (authState.loggedIn ? getToken() : ''))

    const shortToken = computed(() => {
      if (!token.value) return ''
      if (token.value.length <= 20) return token.value
      return `${token.value.slice(0, 20)}...${token.value.slice(-10)}`
    })

    const loadContent = async (tab) => {
      if (!tab || !tab.content) return
      isFetching.value = true
      content.value = tab.content
      isFetching.value = false
    }

    onMounted(() => {
      const initTab = route.query.tab
      if (initTab && tabs.find((t) => t.key === initTab)) {
        selectedTab.value = initTab
        const tab = tabs.find((t) => t.key === initTab)
        if (tab && tab.content) loadContent(tab)
      } else {
        loadContent(tabs[0])
      }
    })

    watch(selectedTab, (name) => {
      const tab = tabs.find((t) => t.key === name)
      if (tab && tab.content) loadContent(tab)
      router.replace({ query: { ...route.query, tab: name } })
    })

    watch(
      () => route.query.tab,
      (name) => {
        if (name && name !== selectedTab.value && tabs.find((t) => t.key === name)) {
          selectedTab.value = name
        }
      },
    )

    const copyToken = async () => {
      if (import.meta.client && token.value) {
        try {
          await navigator.clipboard.writeText(token.value)
          toast.success('已复制 Token')
        } catch (e) {
          toast.error('复制失败')
        }
      }
    }

    const handleContentClick = (e) => {
      handleMarkdownClick(e)
    }

    return {
      tabs,
      selectedTab,
      content,
      renderMarkdown,
      isFetching,
      handleContentClick,
      authState,
      token,
      copyToken,
      shortToken,
      apiDocsUrl,
    }
  },
}
</script>

<style scoped>
.about-page {
  max-width: var(--page-max-width);
  background-color: var(--background-color);
  margin: 0 auto;
}

.about-content {
  line-height: 1.6;
  padding: 20px;
}

.about-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.about-api {
  padding: 20px;
}

.about-api-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
  margin-top: 30px;
  margin-bottom: 15px;
}

.about-api-login-link {
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
}

.about-api-login-link:hover {
  text-decoration: underline;
}

.warning-row {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0.7;
}

.warning-icon {
  font-size: 13px;
}

.token-warning {
  font-size: 13px;
}

.token-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font: 14px;
  margin-bottom: 10px;
  word-break: break-all;
}

.copy-btn {
  padding: 4px 8px;
  cursor: pointer;
}

.about-api-link {
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
}

.about-api-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .about-tabs {
    width: 100vw;
  }
}
</style>
