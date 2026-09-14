<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><path d="M12 22 s8-4 8-10 V5 l-8-3-8 3 v7 c0 6 8 10 8 10 z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
