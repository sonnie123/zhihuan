<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" fill="none"/><path d="M8 14 s1.5 2 4 2 4-2 4-2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/><line x1="9" y1="9" x2="9.01" y2="9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/><line x1="15" y1="9" x2="15.01" y2="9" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
