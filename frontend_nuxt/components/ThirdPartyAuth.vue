<template>
  <div class="third-party-auth">
    <div
      v-for="provider in providers"
      :key="provider.name"
      class="third-party-button"
      :class="provider.name"
      @click="provider.action"
    >
      <img
        class="third-party-button-icon"
        :class="provider.name"
        :src="provider.icon"
        :alt="provider.alt"
      />
      <div class="third-party-button-text" :class="provider.name">
        {{ provider.label }}
      </div>
    </div>
  </div>
</template>

<script setup>
import googleIcon from '~/assets/icons/google.svg'
import githubIcon from '~/assets/icons/github.svg'
import discordIcon from '~/assets/icons/discord.svg'
import twitterIcon from '~/assets/icons/twitter.svg'
import telegramIcon from '~/assets/icons/telegram.svg'

import { googleAuthorize } from '~/utils/google'
import { githubAuthorize } from '~/utils/github'
import { discordAuthorize } from '~/utils/discord'
import { twitterAuthorize } from '~/utils/twitter'
import { telegramAuthorize } from '~/utils/telegram'

const props = defineProps({
  mode: {
    type: String,
    default: 'login',
  },
  inviteToken: {
    type: String,
    default: '',
  },
})

const actionText = computed(() => (props.mode === 'signup' ? '注册' : '登录'))

const providers = computed(() => [
  {
    name: 'google',
    icon: googleIcon,
    action: () => googleAuthorize(props.inviteToken),
    alt: 'Google Logo',
    label: `Google ${actionText.value}`,
  },
  {
    name: 'github',
    icon: githubIcon,
    action: () => githubAuthorize(props.inviteToken),
    alt: 'GitHub Logo',
    label: `GitHub ${actionText.value}`,
  },
  {
    name: 'discord',
    icon: discordIcon,
    action: () => discordAuthorize(props.inviteToken),
    alt: 'Discord Logo',
    label: `Discord ${actionText.value}`,
  },
  {
    name: 'twitter',
    icon: twitterIcon,
    action: () => twitterAuthorize(props.inviteToken),
    alt: 'Twitter Logo',
    label: `X ${actionText.value}`,
  },
  {
    name: 'telegram',
    icon: telegramIcon,
    action: () => telegramAuthorize(props.inviteToken),
    alt: 'Telegram Logo',
    label: `Telegram ${actionText.value}`,
  },
])
</script>

<style scoped>
.third-party-auth {
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 30%;
  gap: 11px;
}

.third-party-button {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 7px 20px;
  min-width: 150px;
  background-color: var(--login-background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 20px;
  cursor: pointer;
  gap: 10px;
}

.third-party-button:hover {
  background-color: var(--login-background-color-hover);
}

.third-party-button-icon {
  width: 20px;
  height: 20px;
}

.third-party-button-text {
  font-size: 16px;
}

.third-party-button-text.twitter {
  color: rgb(182, 182, 182);
}

/* Provider specific classes for customization */
.third-party-button.google {
  background-color: var(--google-bg, var(--login-background-color));
  color: var(--google-color, inherit);
}
.third-party-button.google:hover {
  background-color: var(--google-bg-hover, var(--login-background-color-hover));
}

.third-party-button.github {
  background-color: var(--github-bg, var(--login-background-color));
  color: var(--github-color, inherit);
}
.third-party-button.github:hover {
  background-color: var(--github-bg-hover, var(--login-background-color-hover));
}

.third-party-button.discord {
  background-color: var(--discord-bg, var(--login-background-color));
  color: var(--discord-color, inherit);
}
.third-party-button.discord:hover {
  background-color: var(--discord-bg-hover, var(--login-background-color-hover));
}

.third-party-button.twitter {
  background-color: var(--twitter-bg, var(--login-background-color));
  color: var(--twitter-color, inherit);
}
.third-party-button.twitter:hover {
  background-color: var(--twitter-bg-hover, var(--login-background-color-hover));
}

.third-party-button.telegram {
  background-color: var(--telegram-bg, var(--login-background-color));
  color: var(--telegram-color, inherit);
}
.third-party-button.telegram:hover {
  background-color: var(--telegram-bg-hover, var(--login-background-color-hover));
}

@media (max-width: 768px) {
  .third-party-auth {
    margin-top: 20px;
    margin-left: 0px;
    width: calc(100% - 40px);
    gap: 10px;
  }

  .third-party-button {
    width: calc(100% - 40px);
  }
}
</style>
