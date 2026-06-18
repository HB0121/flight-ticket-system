<template>
  <main class="route-layout">
    <header class="route-layout__header">
      <div>
        <p class="route-layout__eyebrow">Phase 1</p>
        <h1>User Flights</h1>
      </div>
      <nav class="route-layout__nav">
        <RouterLink to="/flights">Flights</RouterLink>
        <RouterLink to="/favorites">Favorites</RouterLink>
        <RouterLink to="/history">History</RouterLink>
      </nav>
    </header>

    <section class="route-layout__content">
      <RouterView />
    </section>
  </main>
</template>
