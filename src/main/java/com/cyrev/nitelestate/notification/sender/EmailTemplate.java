package com.cyrev.nitelestate.notification.sender;

/**
 * Wraps every outbound email's plain-text message in a single branded HTML layout — table-based
 * and fully inline-styled (no external stylesheet, no &lt;style&gt; block) since that's what
 * actually renders consistently across email clients (Gmail/Outlook strip or ignore the rest).
 * Used by ResendEmailSender only — MockEmailSender just logs the raw text, since there's no real
 * client rendering it in dev.
 */
final class EmailTemplate {

    private static final String HTML = """
            <!DOCTYPE html>
            <html>
              <body style="margin:0;padding:0;background-color:#f1f4f2;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f1f4f2;padding:32px 16px;">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 4px rgba(16,24,16,0.08);">
                        <tr>
                          <td style="background-color:#145a32;padding:28px 32px;">
                            <span style="color:#ffffff;font-size:19px;font-weight:700;letter-spacing:0.2px;">Nitel Estate</span>
                            <div style="color:#bfe3cc;font-size:12px;margin-top:3px;">Estate Management System</div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;color:#1f2a24;font-size:15px;line-height:1.65;">
                            {{BODY}}
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:18px 32px;background-color:#f8faf9;border-top:1px solid #e6ebe8;color:#8a948f;font-size:12px;line-height:1.5;">
                            This is an automated message from Nitel Estate Management System. Please do not reply to this email.
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;

    private EmailTemplate() {
    }

    /**
     * message is plain text (possibly multi-line) — escaped, line-broken, and any bare URL
     * (e.g. MessageTemplates.accountProvisioned's login link) turned into a clickable, styled
     * link, all for safe HTML embedding.
     */
    static String render(String message) {
        String bodyHtml = linkify(escapeHtml(message).replace("\n", "<br>"));
        return HTML.replace("{{BODY}}", bodyHtml);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String linkify(String html) {
        return html.replaceAll("(https?://[^\\s<]+)",
                "<a href=\"$1\" style=\"color:#145a32;font-weight:600;text-decoration:underline;\">$1</a>");
    }
}
