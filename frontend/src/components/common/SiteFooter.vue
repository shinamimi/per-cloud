<template>
  <footer class="site-footer">
    <a
      class="footer-icp"
      href="https://beian.miit.gov.cn/#/Integrated/index"
      target="_blank"
      rel="noopener noreferrer"
    >
      粤ICP备2022031209号-1
    </a>
    <span class="footer-divider">|</span>
    <a
      class="footer-beian"
      href="https://beian.mps.gov.cn/#/query/webSearch?code=44030702004785"
      target="_blank"
      rel="noreferrer"
    >
      粤公网安备44030702004785号
    </a>
    <img class="beian-icon" :src="beianIcon" alt="公安备案" />
  </footer>
</template>

<script setup lang="ts">
const beianIcon = import.meta.env.BASE_URL + 'beian.png'
</script>

<style scoped>
.site-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  font-size: 12px;
  color: #fff;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
}

.site-footer a {
  color: inherit;
  text-decoration: none;
}

.site-footer a:hover {
  text-decoration: underline;
}

.footer-divider {
  opacity: 0.5;
}

.beian-icon {
  height: 14px;
  width: auto;
}
</style>