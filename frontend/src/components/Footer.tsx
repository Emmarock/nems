/** Shown on every page — the authenticated app shell (see Layout) and each standalone auth page
 * (login, claim account, set/reset password) alike. */
export function Footer() {
  const year = new Date().getFullYear()
  return (
    <footer className="app-footer">
      All rights reserved &copy; {year} Faceless Chronicle Ltd, built for Nitel Estate, Off Obafemi Awolowo Road, Ikorodu Lagos.
    </footer>
  )
}
