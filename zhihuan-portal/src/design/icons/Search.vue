<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" fill="none"/><line x1="16" y1="16" x2="21" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
