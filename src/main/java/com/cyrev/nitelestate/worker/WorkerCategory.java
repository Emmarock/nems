package com.cyrev.nitelestate.worker;

/** Chosen at request time (see WorkerRequest) — distinct from workType ("Plumbing", "Electrical", ...), which describes the work itself, not who the worker works for. */
public enum WorkerCategory {
    /** Employed directly by one household (e.g. a nanny, driver, cook, cleaner). */
    DOMESTIC_STAFF,
    /** A contractor/artisan/labourer, typically on site for a defined job rather than ongoing household employment. */
    NON_DOMESTIC_STAFF
}
