// Build a single-page docs site from docs/en and docs/th.
//   node tools/build-docs-page.mjs            -> build/docs-site.html
// The Markdown is embedded as JSON and rendered in the browser with `marked`, so the
// docs/ folder stays the only source. Internal .md links become in-page navigation.
import fs from 'fs';
import path from 'path';

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/(\w:)/, '$1')), '..');
const docs = path.join(root, 'docs');
const out = path.join(root, 'build', 'docs-site.html');

const chapters = fs.readdirSync(path.join(docs, 'en'))
  .filter(f => f.endsWith('.md'))
  .sort((a, b) => (a === 'index.md' ? -1 : b === 'index.md' ? 1 : a === 'reference.md' ? 1 : b === 'reference.md' ? -1 : a.localeCompare(b)));

const data = { chapters: [] };
for (const file of chapters) {
  const en = fs.readFileSync(path.join(docs, 'en', file), 'utf8');
  const thPath = path.join(docs, 'th', file);
  const th = fs.existsSync(thPath) ? fs.readFileSync(thPath, 'utf8') : null;
  const title = md => (md.match(/^# (.+)$/m) || [, file])[1].replace(/^\d+\.\s*/, '');
  data.chapters.push({
    id: file.replace(/\.md$/, ''),
    title: { en: title(en), th: th ? title(th) : title(en) },
    md: { en, th: th ?? en },
  });
}
const json = JSON.stringify(data).replace(/</g, '\\u003c');

const html = `<title>CelestiRift Docs</title>
<meta name="claude:viewport-fit-sides" content="cover">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@500;600;700&family=IBM+Plex+Sans+Thai:wght@400;500;600&family=JetBrains+Mono:wght@400;600&display=swap">
<style>
/* Layout: sticky top bar · chapter sidebar · one readable column of rendered Markdown */
:root {
  --bg: #f6f5fb; --surface: #ffffff; --fg: #1d1a2b; --muted: #5d5873; --line: #e2dfee;
  --accent: #6b3bd6; --accent-soft: #ece5ff; --code-bg: #f0eef8; --code-fg: #2a2540;
  --t1: #9ca3af; --t2: #16a34a; --t3: #1d4ed8; --t4: #7e22ce; --t5: #d97706;
  --display: "Chakra Petch", "IBM Plex Sans Thai", system-ui, sans-serif;
  --body: "IBM Plex Sans Thai", system-ui, -apple-system, "Segoe UI", sans-serif;
  --mono: "JetBrains Mono", ui-monospace, Consolas, monospace;
}
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) {
  --bg: #110f19; --surface: #191624; --fg: #ebe8f5; --muted: #a39db9; --line: #2b2739;
  --accent: #a98bff; --accent-soft: #2a2145; --code-bg: #0d0b14; --code-fg: #e3ddf7;
  --t1: #9ca3af; --t2: #4ade80; --t3: #60a5fa; --t4: #c084fc; --t5: #fbbf24; color-scheme: dark } }
:root[data-theme="dark"] {
  --bg: #110f19; --surface: #191624; --fg: #ebe8f5; --muted: #a39db9; --line: #2b2739;
  --accent: #a98bff; --accent-soft: #2a2145; --code-bg: #0d0b14; --code-fg: #e3ddf7;
  --t1: #9ca3af; --t2: #4ade80; --t3: #60a5fa; --t4: #c084fc; --t5: #fbbf24; color-scheme: dark }

* { box-sizing: border-box }
body { background: var(--bg); color: var(--fg); font: 15px/1.7 var(--body); }
body::before { content: ""; position: fixed; inset: 0 0 auto; height: env(safe-area-inset-top, 0px); z-index: 10; background: var(--surface) }
a { color: var(--accent) }
:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; border-radius: 4px }

.top { position: sticky; top: env(safe-area-inset-top, 0px); z-index: 5; background: var(--surface); border-bottom: 1px solid var(--line) }
.top-inner { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 16px; padding-block: 10px;
  padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); max-width: 1240px; margin: 0 auto }
.brand { font: 700 19px/1 var(--display); letter-spacing: .02em; color: var(--fg); text-decoration: none; display: flex; align-items: center; gap: 10px }
.gem { width: 14px; height: 14px; transform: rotate(45deg); background: linear-gradient(135deg, var(--accent), var(--t3)); border-radius: 3px }
.badges { display: flex; gap: 6px; flex-wrap: wrap }
.badge { font: 600 11px/1 var(--mono); letter-spacing: .04em; padding: 5px 7px; border: 1px solid var(--line); border-radius: 6px; color: var(--muted) }
.spacer { flex: 1 }
.search { flex: 1 1 220px; max-width: 340px; min-width: 0; font: 14px var(--body); padding: 8px 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--bg); color: var(--fg) }
.lang { display: inline-flex; border: 1px solid var(--line); border-radius: 8px; overflow: hidden }
.lang button { font: 600 13px var(--display); letter-spacing: .04em; padding: 7px 12px; border: 0; background: transparent; color: var(--muted); cursor: pointer }
.lang button[aria-pressed="true"] { background: var(--accent); color: #fff }
.tiers { display: flex; height: 3px }
.tiers span { flex: 1 } .tiers span:nth-child(1) { background: var(--t1) } .tiers span:nth-child(2) { background: var(--t2) }
.tiers span:nth-child(3) { background: var(--t3) } .tiers span:nth-child(4) { background: var(--t4) } .tiers span:nth-child(5) { background: var(--t5) }

.shell { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 36px; max-width: 1240px; margin: 0 auto;
  padding-block: 28px 64px; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)) }
nav.toc { position: sticky; top: calc(env(safe-area-inset-top, 0px) + 84px); align-self: start; max-height: calc(100vh - 110px); overflow-y: auto }
.toc-label { font: 600 11px var(--display); letter-spacing: .12em; text-transform: uppercase; color: var(--muted); margin: 0 0 8px 10px }
.toc ol { list-style: none; margin: 0; padding: 0; display: grid; gap: 2px }
.toc a { display: grid; grid-template-columns: 26px 1fr; align-items: baseline; gap: 6px; padding: 7px 10px; border-radius: 8px; color: var(--fg); text-decoration: none; font-size: 14px; line-height: 1.35 }
.toc a .n { font: 600 12px var(--mono); color: var(--muted) }
.toc a:hover { background: var(--accent-soft) }
.toc a[aria-current="page"] { background: var(--accent-soft); color: var(--accent); font-weight: 600 }
.toc a[aria-current="page"] .n { color: var(--accent) }
details.toc-mobile { display: none }

main { min-width: 0 }
article { max-width: 760px }
article h1 { font: 700 34px/1.2 var(--display); letter-spacing: .01em; margin: 0 0 6px; text-wrap: balance }
article h2 { font: 600 23px/1.3 var(--display); margin: 2.2em 0 .5em; padding-top: .4em; border-top: 1px solid var(--line); text-wrap: balance }
article h3 { font: 600 18px/1.35 var(--display); margin: 1.8em 0 .4em }
article p, article li { max-width: 68ch }
article code { font: 13px var(--mono); background: var(--code-bg); color: var(--code-fg); padding: 1px 5px; border-radius: 5px }
.pre-wrap { position: relative; margin: 14px 0 }
article pre { background: var(--code-bg); color: var(--code-fg); padding: 14px 16px; border-radius: 10px; overflow-x: auto; border: 1px solid var(--line); margin: 0; line-height: 1.55 }
article pre code { background: none; padding: 0; font-size: 13px }
.copy { position: absolute; top: 8px; right: 8px; font: 600 11px var(--display); letter-spacing: .06em; padding: 4px 8px; border-radius: 6px; border: 1px solid var(--line); background: var(--surface); color: var(--muted); cursor: pointer; opacity: .85 }
.copy:hover { color: var(--accent); opacity: 1 }
.table-wrap { overflow-x: auto; margin: 14px 0; border: 1px solid var(--line); border-radius: 10px }
article table { border-collapse: collapse; width: 100%; font-size: 14px; background: var(--surface) }
article th { text-align: left; font: 600 12px var(--display); letter-spacing: .06em; text-transform: uppercase; color: var(--muted); background: var(--bg) }
article th, article td { padding: 8px 12px; border-bottom: 1px solid var(--line); vertical-align: top }
article tr:last-child td { border-bottom: 0 }
article blockquote { margin: 14px 0; padding: 4px 16px; border-left: 3px solid var(--accent); color: var(--muted) }
.pager { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-top: 48px; padding-top: 18px; border-top: 1px solid var(--line); max-width: 760px }
.pager a { display: grid; gap: 2px; padding: 10px 14px; border: 1px solid var(--line); border-radius: 10px; text-decoration: none; color: var(--fg); min-width: 0; background: var(--surface) }
.pager small { color: var(--muted); font: 600 11px var(--display); letter-spacing: .08em; text-transform: uppercase }
.results { max-width: 760px }
.results h1 { font: 700 26px var(--display); margin: 0 0 14px }
.hit { display: block; padding: 12px 14px; border: 1px solid var(--line); border-radius: 10px; margin-bottom: 8px; text-decoration: none; color: var(--fg); background: var(--surface) }
.hit b { font-family: var(--display); font-weight: 600 } .hit small { color: var(--muted) } .hit p { margin: 4px 0 0; color: var(--muted); font-size: 14px }
mark { background: var(--accent-soft); color: var(--fg); border-radius: 3px; padding: 0 2px }

@media (max-width: 900px) {
  .shell { grid-template-columns: minmax(0, 1fr); gap: 16px; padding-block: 16px 48px }
  nav.toc { display: none }
  details.toc-mobile { display: block; border: 1px solid var(--line); border-radius: 10px; background: var(--surface) }
  details.toc-mobile summary { padding: 10px 14px; font: 600 14px var(--display); cursor: pointer }
  details.toc-mobile .toc { padding: 0 6px 8px }
  article h1 { font-size: 28px }
  .badges { display: none }
  .search { max-width: none }
}
@media (prefers-reduced-motion: no-preference) { .toc a, .pager a, .hit { transition: background .15s, border-color .15s } .pager a:hover, .hit:hover { border-color: var(--accent) } }
</style>

<header class="top">
  <div class="top-inner">
    <a class="brand" href="#en-index" id="brand"><span class="gem" aria-hidden="true"></span>CelestiRift</a>
    <div class="badges"><span class="badge">PAPER 26.3 · 252+</span><span class="badge">JAVA 25</span><span class="badge">API 2.2</span></div>
    <span class="spacer"></span>
    <input class="search" id="q" type="search" autocomplete="off" aria-label="Search the docs">
    <div class="lang" role="group" aria-label="Language">
      <button type="button" id="lang-en" data-lang="en">EN</button><button type="button" id="lang-th" data-lang="th">ไทย</button>
    </div>
  </div>
  <div class="tiers" aria-hidden="true" title="Gem tiers: Common, Uncommon, Rare, Epic, Legendary"><span></span><span></span><span></span><span></span><span></span></div>
</header>

<div class="shell">
  <nav class="toc" id="toc" aria-label="Chapters"></nav>
  <main>
    <details class="toc-mobile" id="toc-mobile"><summary id="toc-mobile-label"></summary><div class="toc" id="toc-m"></div></details>
    <article id="doc"></article>
    <div class="pager" id="pager"></div>
  </main>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/marked/12.0.2/marked.min.js"></script>
<script type="application/json" id="docs-data">${json}</script>
<script>
(function () {
  var DATA = JSON.parse(document.getElementById('docs-data').textContent);
  var UI = {
    en: { chapters: 'Chapters', search: 'Search the docs…', menu: 'Chapters', prev: 'Previous', next: 'Next', copy: 'Copy', copied: 'Copied',
          results: 'Search', none: 'No matches. Try another word, or switch language.', ref: 'reference' },
    th: { chapters: 'บทต่างๆ', search: 'ค้นหาในคู่มือ…', menu: 'บทต่างๆ', prev: 'ก่อนหน้า', next: 'ถัดไป', copy: 'คัดลอก', copied: 'คัดลอกแล้ว',
          results: 'ผลการค้นหา', none: 'ไม่พบ ลองคำอื่น หรือเปลี่ยนภาษา', ref: 'ตาราง attribute' }
  };
  var byId = {}; DATA.chapters.forEach(function (c, i) { c.index = i; byId[c.id] = c; });
  var lang = 'en';
  try { var saved = localStorage.getItem('cr-docs-lang'); if (saved === 'th' || saved === 'en') lang = saved; } catch (e) {}
  if (!saved && /^th/i.test(navigator.language || '')) lang = 'th';

  function slug(h) {
    return h.trim().toLowerCase().replace(/[\`*_]/g, '').replace(/[^\\p{L}\\p{N}\\s-]/gu, '').replace(/\\s/g, '-');
  }
  function num(c) { var m = c.id.match(/^(\\d+)/); return m ? String(+m[1]) : (c.id === 'index' ? '·' : '∗'); }
  function route() {
    var h = decodeURIComponent(location.hash.slice(1));
    var m = h.match(/^(en|th)-(.+)$/);
    if (m && byId[m[2]]) return { lang: m[1], id: m[2] };
    return { lang: lang, id: 'index' };
  }
  function go(l, id, anchor) {
    var target = '#' + l + '-' + id;
    pendingAnchor = anchor || null;
    if (location.hash === target) render(); else location.hash = target;
  }
  var pendingAnchor = null;

  function tocHtml(cur) {
    var t = UI[lang];
    var items = DATA.chapters.map(function (c) {
      return '<li><a href="#' + lang + '-' + c.id + '"' + (c.id === cur ? ' aria-current="page"' : '') + '><span class="n">' + num(c) + '</span><span>' + esc(c.title[lang]) + '</span></a></li>';
    }).join('');
    return '<p class="toc-label">' + t.chapters + '</p><ol>' + items + '</ol>';
  }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (ch) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[ch]; }); }

  function renderMarkdown(md) {
    var html = marked.parse(md, { gfm: true });
    var box = document.createElement('div');
    box.innerHTML = html;
    box.querySelectorAll('h1,h2,h3,h4').forEach(function (h) { h.id = slug(h.textContent); });
    box.querySelectorAll('table').forEach(function (tb) { var w = document.createElement('div'); w.className = 'table-wrap'; tb.parentNode.insertBefore(w, tb); w.appendChild(tb); });
    box.querySelectorAll('pre').forEach(function (pre) {
      var w = document.createElement('div'); w.className = 'pre-wrap'; pre.parentNode.insertBefore(w, pre); w.appendChild(pre);
      var b = document.createElement('button'); b.type = 'button'; b.className = 'copy'; b.textContent = UI[lang].copy; w.appendChild(b);
    });
    // Internal links: other chapters (.md), the other language folder, and #anchors.
    box.querySelectorAll('a[href]').forEach(function (a) {
      var href = a.getAttribute('href');
      if (/^https?:/.test(href)) { a.target = '_blank'; a.rel = 'noopener'; return; }
      var m = href.match(/^(?:\\.\\.\\/(en|th)\\/)?([\\w-]+)\\.md(?:#(.+))?$/);
      if (m) {
        var l = m[1] || lang; var id = m[2];
        if (byId[id]) { a.setAttribute('href', '#' + l + '-' + id); a.dataset.go = l + '|' + id + '|' + (m[3] || ''); }
      } else if (href.charAt(0) === '#') {
        a.dataset.go = lang + '|' + current().id + '|' + href.slice(1);
        a.setAttribute('href', '#' + lang + '-' + current().id);
      }
    });
    return box;
  }
  function current() { return route(); }

  function render() {
    var r = route(); lang = r.lang;
    try { localStorage.setItem('cr-docs-lang', lang); } catch (e) {}
    document.documentElement.lang = lang;
    var c = byId[r.id], t = UI[lang];
    document.getElementById('lang-en').setAttribute('aria-pressed', lang === 'en');
    document.getElementById('lang-th').setAttribute('aria-pressed', lang === 'th');
    document.getElementById('q').placeholder = t.search;
    document.getElementById('brand').setAttribute('href', '#' + lang + '-index');
    document.getElementById('toc').innerHTML = tocHtml(c.id);
    document.getElementById('toc-m').innerHTML = tocHtml(c.id);
    document.getElementById('toc-mobile-label').textContent = num(c) + '  ' + c.title[lang];
    document.getElementById('toc-mobile').open = false;
    var art = document.getElementById('doc');
    art.innerHTML = '';
    var md = c.md[lang].replace(/^\\[[^\\n]*\\]\\([^)]*\\)\\s*·\\s*\\[[^\\n]*\\n/m, ''); // drop the per-page "← Index · language" line
    art.appendChild(renderMarkdown(md));
    var prev = DATA.chapters[c.index - 1], next = DATA.chapters[c.index + 1];
    document.getElementById('pager').innerHTML =
      (prev ? '<a href="#' + lang + '-' + prev.id + '"><small>← ' + t.prev + '</small>' + esc(prev.title[lang]) + '</a>' : '<span></span>') +
      (next ? '<a href="#' + lang + '-' + next.id + '" style="text-align:right"><small>' + t.next + ' →</small>' + esc(next.title[lang]) + '</a>' : '');
    document.title = (c.id === 'index' ? '' : c.title[lang] + ' · ') + 'CelestiRift Docs';
    if (pendingAnchor) { var el = document.getElementById(pendingAnchor); pendingAnchor = null; if (el) { el.scrollIntoView(); return; } }
    window.scrollTo(0, 0);
  }

  // ---- search: sections (h2/h3) of the current language containing every word
  var index = {};
  function buildIndex(l) {
    if (index[l]) return index[l];
    var rows = [];
    DATA.chapters.forEach(function (c) {
      var parts = c.md[l].split(/\\n(?=#{2,3} )/);
      parts.forEach(function (p, i) {
        var head = i === 0 ? c.title[l] : p.split('\\n')[0].replace(/^#+\\s*/, '');
        rows.push({ c: c, head: head, anchor: i === 0 ? '' : slug(head), text: p.replace(/\`\`\`[\\s\\S]*?\`\`\`/g, ' ').replace(/[#*\`|>\\[\\]()_-]+/g, ' ') });
      });
    });
    return index[l] = rows;
  }
  function search(q) {
    var words = q.toLowerCase().split(/\\s+/).filter(Boolean);
    if (!words.length) { render(); return; }
    var t = UI[lang];
    var hits = buildIndex(lang).filter(function (r) { var s = (r.head + ' ' + r.text).toLowerCase(); return words.every(function (w) { return s.indexOf(w) >= 0; }); }).slice(0, 30);
    var art = document.getElementById('doc');
    var out = '<div class="results"><h1>' + t.results + ': “' + esc(q) + '”</h1>';
    if (!hits.length) out += '<p>' + t.none + '</p>';
    hits.forEach(function (h) {
      var s = h.text.replace(/\\s+/g, ' '); var at = s.toLowerCase().indexOf(words[0]);
      var snip = s.slice(Math.max(0, at - 60), at + 120);
      var safe = esc(snip).replace(new RegExp('(' + words.map(function (w) { return w.replace(/[.*+?^\${}()|[\\]\\\\]/g, '\\\\$&'); }).join('|') + ')', 'gi'), '<mark>$1</mark>');
      out += '<a class="hit" href="#' + lang + '-' + h.c.id + '" data-go="' + lang + '|' + h.c.id + '|' + h.anchor + '"><small>' + num(h.c) + ' · ' + esc(h.c.title[lang]) + '</small><br><b>' + esc(h.head) + '</b><p>…' + safe + '…</p></a>';
    });
    art.innerHTML = out + '</div>';
    document.getElementById('pager').innerHTML = '';
  }

  document.addEventListener('click', function (e) {
    var b = e.target.closest('.copy');
    if (b) {
      var code = b.parentNode.querySelector('pre').innerText; var t = UI[lang];
      var done = function () { b.textContent = t.copied; setTimeout(function () { b.textContent = t.copy; }, 1400); };
      try { navigator.clipboard.writeText(code).then(done, function () { selectText(b.parentNode.querySelector('pre')); }); } catch (err) { selectText(b.parentNode.querySelector('pre')); }
      return;
    }
    var a = e.target.closest('a[data-go]');
    if (a) { e.preventDefault(); var p = a.dataset.go.split('|'); document.getElementById('q').value = ''; go(p[0], p[1], p[2]); }
    var lb = e.target.closest('.lang button');
    if (lb) { var r = route(); document.getElementById('q').value = ''; go(lb.dataset.lang, r.id); }
  });
  function selectText(el) { var s = window.getSelection(), rg = document.createRange(); rg.selectNodeContents(el); s.removeAllRanges(); s.addRange(rg); }
  var timer; document.getElementById('q').addEventListener('input', function (e) { clearTimeout(timer); var v = e.target.value; timer = setTimeout(function () { search(v.trim()); }, 120); });
  window.addEventListener('hashchange', render);
  if (!location.hash) history.replaceState(null, '', '#' + lang + '-index');
  render();
})();
</script>
`;

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, html);
console.log(`Wrote ${path.relative(root, out)} (${data.chapters.length} chapters, ${Math.round(html.length / 1024)} KB)`);
