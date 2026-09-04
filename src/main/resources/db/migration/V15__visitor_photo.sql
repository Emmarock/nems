-- Lets a resident attach a photo of the visitor when creating a pass, mirroring V8__worker_photo,
-- so security can visually confirm identity against the QR pass at the gate.
-- Stored as a base64 data URI; the frontend downscales/compresses before upload to keep
-- this reasonably small (see VisitorService.create's size guard).

alter table visitor add column photo text;
