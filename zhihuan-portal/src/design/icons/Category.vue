<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="2" fill="none"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="2" fill="none"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="2" fill="none"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="2" fill="none"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
