<template>
  <main class="route-layout">
    <header class="route-layout__header">
      <div>
        <p class="route-layout__eyebrow">Phase 1</p>
        <h1>Admin Crawl</h1>
      </div>
      <nav class="route-layout__nav">
        <RouterLink to="/admin/crawl-jobs">Crawl Jobs</RouterLink>
        <RouterLink to="/admin/data-sources">Data Sources</RouterLink>
      </nav>
    </header>

    <section class="route-layout__content">
      <RouterView />
    </section>
  </main>
</template>
