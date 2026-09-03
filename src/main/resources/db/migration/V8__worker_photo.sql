-- Lets a resident attach a photo of the worker when requesting access (spec Phase 2 §4),
-- so security can visually confirm identity against the QR pass at the gate.
-- Stored as a base64 data URI; the frontend downscales/compresses before upload to keep
-- this reasonably small (see WorkerService.request's size guard).

alter table worker add column photo text;
