<template>
  <NotFoundPanel :path="route.path" />
</template>

<script setup>
const route = useRoute()

if (import.meta.server) {
  setResponseStatus(404)
}

useHead({
  title: '页面不存在 - 珞珈论坛',
})
</script>
