/** No look-alike characters (0/O, 1/l/I), so a password read out or retyped from SMS survives. */
const UPPER = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
const LOWER = 'abcdefghijkmnopqrstuvwxyz'
const DIGITS = '23456789'
const SYMBOLS = '!@#$%^&*-_=+?'
const ALL = UPPER + LOWER + DIGITS + SYMBOLS

/** Uniform random index below n from the browser's CSPRNG (rejection sampling avoids modulo bias). */
function randomIndex(n: number): number {
  const limit = Math.floor(0x100000000 / n) * n
  const buf = new Uint32Array(1)
  do {
    crypto.getRandomValues(buf)
  } while (buf[0] >= limit)
  return buf[0] % n
}

/**
 * A strong random password: `length` characters (16 by default, ~100 bits of entropy) with at
 * least one upper-case letter, lower-case letter, digit and symbol.
 */
export function generateStrongPassword(length = 16): string {
  const chars = [UPPER, LOWER, DIGITS, SYMBOLS].map((set) => set[randomIndex(set.length)])
  while (chars.length < length) chars.push(ALL[randomIndex(ALL.length)])
  for (let i = chars.length - 1; i > 0; i--) {
    const j = randomIndex(i + 1)
    ;[chars[i], chars[j]] = [chars[j], chars[i]]
  }
  return chars.join('')
}
