/* Liga Not FIFA 2026 - JavaScript principal */
(function () {
  'use strict';

  // ----- Tema claro / oscuro (se recuerda en localStorage; el script temprano del <head> evita el parpadeo) -----
  var boton = document.getElementById('tema-btn');
  function aplicar(tema) {
    document.documentElement.setAttribute('data-theme', tema);
    if (boton) { boton.textContent = tema === 'dark' ? '☀️' : '🌙'; boton.setAttribute('aria-label', tema === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'); }
  }
  aplicar(document.documentElement.getAttribute('data-theme') || 'light');
  if (boton) {
    boton.addEventListener('click', function () {
      var nuevo = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      try { localStorage.setItem('tema', nuevo); } catch (e) { /* almacenamiento no disponible */ }
      aplicar(nuevo);
    });
  }

  // ----- Menú móvil -----
  var menuBtn = document.getElementById('menu-btn');
  var enlaces = document.getElementById('enlaces');
  if (menuBtn && enlaces) {
    menuBtn.addEventListener('click', function () {
      var abierto = enlaces.classList.toggle('abierto');
      menuBtn.setAttribute('aria-expanded', abierto ? 'true' : 'false');
    });
  }

  // ----- Filas dinámicas de goles (formulario de resultado) -----
  var contenedor = document.getElementById('filas-goles');
  var plantilla = document.getElementById('plantilla-gol');
  var agregar = document.getElementById('agregar-gol');
  if (contenedor && plantilla && agregar) {
    var siguiente = contenedor.querySelectorAll('.fila-gol').length;
    agregar.addEventListener('click', function () {
      var html = plantilla.innerHTML.replace(/__INDEX__/g, String(siguiente++));
      var div = document.createElement('div');
      div.innerHTML = html.trim();
      contenedor.appendChild(div.firstElementChild);
    });
    contenedor.addEventListener('click', function (e) {
      if (e.target.classList.contains('quitar-gol')) {
        var fila = e.target.closest('.fila-gol');
        if (fila) { fila.remove(); }
      }
    });
  }
})();
