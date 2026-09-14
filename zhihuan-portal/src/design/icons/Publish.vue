<template><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="1em" height="1em" :fill="fill"><path d="M12 5 V19 M5 12 H19" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" fill="none"/></svg></template>

<script setup lang="ts">
withDefaults(defineProps<{ fill?: string }>(), { fill: 'none' })
</script>
