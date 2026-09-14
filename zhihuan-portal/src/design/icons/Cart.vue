<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><path d="M3 4 h2 l2 12 h12 l2-8 H7" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round" stroke-linecap="round"/><circle cx="9" cy="20" r="1.5" fill="currentColor"/><circle cx="18" cy="20" r="1.5" fill="currentColor"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
