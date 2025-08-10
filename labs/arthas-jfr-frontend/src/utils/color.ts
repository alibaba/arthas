// @ts-nocheck
// eslint-disable-next-line
export function hashCode(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = 31 * hash + (str.charCodeAt(i) & 0xff);
    hash &= 0xffffffff;
  }
  return hash;
}

export function colorForName(name, palette) {
  const idx = Math.abs(hashCode(name)) % palette.length;
  return palette[idx];
} 