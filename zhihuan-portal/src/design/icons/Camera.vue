<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><path d="M23 19 a2 2 0 0 1-2 2 H3 a2 2 0 0 1-2-2 V8 a2 2 0 0 1 2-2 h4 l2-3 h6 l2 3 h4 a2 2 0 0 1 2 2 z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/><circle cx="12" cy="13" r="4" stroke="currentColor" stroke-width="2" fill="none"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
