/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2026 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
(function () {
  document.documentElement.classList.remove('no-js'); // without this script the page shows the finished screen
  var LOOP = 16000; // ms, the whole scene is computed from this single clock
  var started = performance.now();
  var elapsed = document.getElementById('elapsed');
  var $ = function (selector) { return document.querySelector(selector); };
  var $$ = function (selector) { return Array.prototype.slice.call(document.querySelectorAll(selector)); };

  // Pieces of the workplace screen: [selector, percent of the loop when it lands]
  var PIECES = [['.logo', 5], ['.side', 15], ['.c1', 35], ['.c2', 47], ['.w1', 59], ['.w2', 71]];
  // The carrier walks from the left and each delivery ends when its piece lands
  var WALKS = [[24.5, 35], [36.5, 47], [48.5, 59], [60.5, 71]];
  // Speech bubble: one text per startup phase, in the order of the server phases
  var PHASES = ['starting', 'deploying', 'services'];
  var phase = 'starting';
  var END = 96; // percent where the finished screen starts fading

  var pieces = PIECES.map(function (p) { return {el: $(p[0]), at: p[1]}; });
  var carrier = $('.carrier');
  var bubble = $$('.bubble span');
  var legL = $('.wk2 .legL'), legR = $('.wk2 .legR');
  var arms = {
    w1R: $('.wk1 .armR'), w1L: $('.wk1 .armL'),
    w4R: $('.wk4 .armR'), w4L: $('.wk4 .armL'),
    w3R: $('.wk3 .armR'), w3L: $('.wk3 .armL')
  };

  var clamp = function (v) { return Math.max(0, Math.min(1, v)); };
  var rotate = function (el, angle, x, y) { if (el) { el.setAttribute('transform', 'rotate(' + angle.toFixed(2) + ' ' + x + ' ' + y + ')'); } };
  var fadeOut = function (u) { return 1 - clamp((u - END) / 3); };

  function render(time) {
    var u = (time % LOOP) / LOOP * 100;

    pieces.forEach(function (piece) {
      var p = clamp((u - piece.at) / 3);
      var eased = 1 - Math.pow(1 - p, 3);
      piece.el.style.opacity = (eased * fadeOut(u)).toFixed(3);
      piece.el.setAttribute('transform', 'translate(0 ' + (-16 * (1 - eased)).toFixed(2) + ')');
    });

    var x = -80, visible = 0;
    WALKS.forEach(function (walk) {
      if (u >= walk[0] && u < walk[1]) {
        x = -80 + 330 * (u - walk[0]) / (walk[1] - walk[0]);
        visible = 1;
      }
    });
    carrier.style.opacity = visible;
    carrier.setAttribute('transform', 'translate(' + x.toFixed(1) + ' 0)');
    var step = 22 * Math.sin(time / 420 * Math.PI);
    rotate(legL, step, -4, -30);
    rotate(legR, -step, 5, -30);

    // Ladder worker reaches up every 1.6 s, the logo and the menu land at the top of a reach
    var reach = 1 - Math.cos(time / 1600 * 2 * Math.PI);
    rotate(arms.w1R, -13 * reach, 9, -54);
    rotate(arms.w1L, 6 * reach, -9, -54);
    var tug = 1 - Math.cos(time / 1600 * 2 * Math.PI);
    var tugBack = 1 - Math.cos(time / 1600 * 2 * Math.PI + Math.PI);
    rotate(arms.w4R, -9 * tug, 9, -54);
    rotate(arms.w4L, -9 * tugBack, -9, -54);
    var type = 4.5 * (1 + Math.sin(time / 350 * Math.PI));
    rotate(arms.w3L, type, -9, -54);
    rotate(arms.w3R, 9 - type, 9, -54);

    bubble.forEach(function (span, i) {
      span.style.opacity = PHASES[i] === phase ? 1 : 0;
    });
  }

  var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var began = false;
  function begin() {
    if (began) { return; }
    began = true;
    if (reduced) {
      render(LOOP * 0.9); // finished screen, nobody walking
      carrier.style.opacity = 0;
      return;
    }
    var frame = function () {
      render(performance.now() - started);
      requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
  }

  function tick() {
    var seconds = Math.floor((performance.now() - started) / 1000);
    elapsed.textContent = Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');
  }
  setInterval(tick, 1000);

  var bar = document.getElementById('bar');
  var count = document.getElementById('count');
  var error = document.getElementById('error');
  var lost = document.getElementById('lost');
  var wait = document.getElementById('wait');
  var steps = Array.prototype.slice.call(document.querySelectorAll('#steps li'));
  var status = document.querySelector('.status');
  var synced = false;
  var misses = 0;
  var lastElapsed = 0;
  var MAX_MISSES = 5; // polls without any answer before telling that the server is gone

  function showLost(isLost) {
    lost.hidden = !isLost;
    wait.hidden = isLost;
    status.classList.toggle('lost', isLost);
  }

  function showSteps(reached) {
    var current = true;
    steps.forEach(function (step) {
      var done = reached.indexOf(step.dataset.id) >= 0;
      step.classList.toggle('done', done);
      step.classList.toggle('current', !done && current);
      current = current && done;
    });
  }

  function show(data) {
    misses = 0;
    showSteps(data.checkpoints || []);
    if (data.phase === 'failed') {
      // The server is stopping instead of starting
      showLost(true);
      return;
    }
    showLost(false);
    phase = data.phase;
    if (!synced || data.elapsed < lastElapsed) {
      // The time and the progress are the ones of the server: a refresh or another tab does not restart them,
      // and a restarted server starts again from zero
      started = performance.now() - data.elapsed;
      synced = true;
      tick();
      begin();
    }
    lastElapsed = data.elapsed;
    var processed = data.deployed + data.failed;
    var determinate = data.phase === 'deploying' && data.total > 0;
    bar.classList.toggle('determinate', determinate);
    bar.style.setProperty('--p', determinate ? Math.min(100, Math.round(100 * processed / data.total)) + '%' : '0%');
    count.hidden = !(data.total > 0 && data.phase !== 'starting');
    count.textContent = count.dataset.template.replace('{0}', processed).replace('{1}', data.total);
    error.hidden = data.failed === 0;
    status.classList.toggle('failed', data.failed > 0);
  }

  // The status is answered by the loading server only. As soon as another server answers, the platform is up.
  function poll() {
    fetch('/_loading/status', {cache: 'no-store', redirect: 'manual'})
      .then(function (response) {
        if (response.type === 'opaqueredirect' || response.headers.get('X-Startup-Page') !== 'true') {
          location.reload();
          return null;
        }
        return response.json();
      })
      .then(function (data) { if (data) { show(data); } })
      .catch(function () {
        // The server is switching, or it has stopped: tell it when it stays silent
        misses += 1;
        if (synced && misses >= MAX_MISSES) { showLost(true); }
      });
  }
  poll();
  setInterval(poll, 2000);
  // Without an answer from the server (very slow start), animate from now
  setTimeout(begin, 3000);
})();
