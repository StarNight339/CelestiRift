// Generates the placeholder textures of the CelestiRift resource pack.
// Usage:  node tools/generate-pack-textures.mjs
// Replace any PNG in celestirift-core/src/resourcepack/ with real art at any time;
// this script only exists so the pack works out of the box.
import { writeFileSync, mkdirSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { deflateSync } from "node:zlib";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "celestirift-core", "src", "resourcepack",
  "assets", "celestirift", "textures");

// ---------------------------------------------------------------- PNG encoder
const CRC = new Uint32Array(256).map((_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
const crc32 = (buf) => {
  let c = 0xffffffff;
  for (const b of buf) c = CRC[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
};
const chunk = (type, data) => {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
};
function png(w, h, pixel) { // pixel(x,y) -> [r,g,b,a]
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    for (let x = 0; x < w; x++) {
      const [r, g, b, a] = pixel(x, y);
      const i = y * (w * 4 + 1) + 1 + x * 4;
      raw[i] = r; raw[i + 1] = g; raw[i + 2] = b; raw[i + 3] = a;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr), chunk("IDAT", deflateSync(raw)), chunk("IEND", Buffer.alloc(0))]);
}
// Real art (tools/import-gem-textures.mjs) is never overwritten unless you pass --force.
const FORCE = process.argv.includes("--force");
const save = (rel, buf) => { const p = join(root, rel); if (existsSync(p) && !FORCE) { console.log("kept", rel); return; } mkdirSync(dirname(p), { recursive: true }); writeFileSync(p, buf); console.log("wrote", rel); };

// ---------------------------------------------------------------- colour helpers
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const mix = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));
const CLEAR = [0, 0, 0, 0];

// A faceted gem: diamond silhouette, light top-left facet, dark bottom-right, outline.
function gem(from, to) {
  const A = hex(from), B = hex(to);
  return png(16, 16, (x, y) => {
    const cx = 7.5, cy = 7.5, d = Math.abs(x - cx) / 6.5 + Math.abs(y - cy) / 7.5;
    if (d > 1) return CLEAR;
    if (d > 0.82) return [...mix(B, [0, 0, 0], 0.55), 255];               // outline
    const t = (x + y) / 30;
    let c = mix(A, B, t);
    if (x < cx && y < cy) c = mix(c, [255, 255, 255], 0.25);              // lit facet
    if (x > cx && y > cy) c = mix(c, [0, 0, 0], 0.2);                     // shadow facet
    if ((x === 5 && y === 4) || (x === 4 && y === 5) || (x === 5 && y === 5)) c = [255, 255, 255];
    return [...c, 255];
  });
}

// A round rune stone with a glowing glyph.
function rune() {
  const stone = hex("#4b4560"), glow = hex("#d8b4fe");
  return png(16, 16, (x, y) => {
    const dx = x - 7.5, dy = y - 7.5, r = Math.hypot(dx, dy);
    if (r > 7) return CLEAR;
    if (r > 6) return [...mix(stone, [0, 0, 0], 0.5), 255];
    const glyph = (x === 7 || x === 8) && y >= 3 && y <= 12 || (y === 7 || y === 8) && x >= 4 && x <= 11 && Math.abs(dx) > 1.5;
    if (glyph) return [...glow, 255];
    return [...mix(stone, [255, 255, 255], (8 - r) / 40), 255];
  });
}

// A jagged void shard.
function shardvoid() {
  const A = hex("#2b0055"), B = hex("#8000ff");
  return png(16, 16, (x, y) => {
    const w = 2 + Math.round((15 - y) * 0.35);
    const cx = 8 + Math.round(Math.sin(y * 0.9) * 1.5);
    if (y < 1 || y > 14 || Math.abs(x - cx) > w) return CLEAR;
    if (Math.abs(x - cx) === w) return [20, 0, 40, 255];
    const c = mix(A, B, (15 - y) / 15);
    return [...((x + y) % 5 === 0 ? mix(c, [255, 255, 255], 0.4) : c), 255];
  });
}

// A rolled parchment scroll.
function scroll() {
  const paper = hex("#f1e2b8"), edge = hex("#8a6a34"), ink = hex("#7c3aed");
  return png(16, 16, (x, y) => {
    if (y < 2 || y > 13 || x < 2 || x > 13) return CLEAR;
    if (y === 2 || y === 13) return [...edge, 255];                     // rolled ends
    if (x === 2 || x === 13) return [...mix(paper, edge, 0.5), 255];
    if ((y === 5 || y === 8 || y === 11) && x > 3 && x < 12 - (y % 3)) return [...ink, 255];
    return [...paper, 255];
  });
}

// Nine-slice tooltip sprites (100x100, border 9) for the "legendary" tooltip style.
function tooltipBackground() {
  return png(100, 100, (x, y) => {
    const t = y / 99;
    return [...mix(hex("#1a0f00"), hex("#2b1600"), t), 236];
  });
}
function tooltipFrame() {
  const gold = hex("#f59e0b"), amber = hex("#f97316");
  return png(100, 100, (x, y) => {
    const edge = Math.min(x, y, 99 - x, 99 - y);
    if (edge === 1) return [...mix(gold, amber, (x + y) / 198), 255];
    if (edge === 2) return [...mix(gold, [0, 0, 0], 0.55), 200];
    return CLEAR;
  });
}

// ---------------------------------------------------------------- output
const tiers = [["#b0b0b0", "#e0e0e0"], ["#4ade80", "#16a34a"], ["#3b82f6", "#1d4ed8"],
  ["#a855f7", "#7e22ce"], ["#f59e0b", "#f97316"]];
tiers.forEach(([a, b], i) => save(`item/gem_tier_${i + 1}.png`, gem(a, b)));
save("item/rune.png", rune());
save("item/shardvoid.png", shardvoid());
save("item/skill_scroll.png", scroll());
save("gui/sprites/tooltip/legendary_background.png", tooltipBackground());
save("gui/sprites/tooltip/legendary_frame.png", tooltipFrame());
