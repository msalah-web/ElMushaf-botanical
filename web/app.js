(function () {
  const hint = document.getElementById("save-hint");
  const DURATION_MS = 5000;

  function showSaveHint() {
    if (!hint) return;
    hint.hidden = false;
    requestAnimationFrame(function () {
      hint.classList.add("is-visible");
    });
    window.setTimeout(function () {
      hint.classList.remove("is-visible");
      window.setTimeout(function () {
        hint.hidden = true;
      }, 300);
    }, DURATION_MS);
  }

  document.querySelectorAll(".tile").forEach(function (tile) {
    tile.addEventListener("click", function () {
      // Preview shell: keep the home screen interactive without leaving.
      tile.blur();
    });
  });

  showSaveHint();
})();
