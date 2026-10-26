-- Apply once to an existing main schema, after the earlier database scripts.
-- New fields are nullable to preserve all existing rows.
BEGIN;
ALTER TABLE public.shipments
    ADD COLUMN recipient_name VARCHAR(160),
    ADD COLUMN delivery_address VARCHAR(255);
ALTER TABLE public.employees
    ADD COLUMN email VARCHAR(254) UNIQUE,
    ADD COLUMN phone VARCHAR(30);
ALTER TABLE public.vehicles
    ADD COLUMN make VARCHAR(80),
    ADD COLUMN model VARCHAR(80),
    ADD COLUMN vehicle_type VARCHAR(40);
ALTER TABLE public.locations
    ADD COLUMN latitude NUMERIC(10,7) CHECK (latitude BETWEEN -90 AND 90),
    ADD COLUMN longitude NUMERIC(10,7) CHECK (longitude BETWEEN -180 AND 180),
    ADD COLUMN recorded_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN vehicle_id BIGINT REFERENCES public.vehicles(id);
CREATE INDEX idx_locations_vehicle ON public.locations(vehicle_id);
COMMIT;
