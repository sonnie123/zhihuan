<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><path d="M21 11.5a8.38 8.38 0 0 1-8.5 8.5 8.5 8.5 0 0 1-3.5-.7L3 21l1.7-6A8.38 8.38 0 0 1 4 11.5a8.5 8.5 0 0 1 17 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
