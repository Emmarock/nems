package com.cyrev.nitelestate.sticker;

public enum StickerStatus {
    /** Requested, but no payment has succeeded against the linked invoice yet - covers "never
     * paid", "payment still awaiting treasurer/financial-secretary review", and "a submitted
     * receipt was rejected" alike, since all three just mean "try paying again". */
    PENDING_PAYMENT,
    ISSUED
}
