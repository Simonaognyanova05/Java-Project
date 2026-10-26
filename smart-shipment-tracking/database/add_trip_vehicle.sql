-- Run once after creating trips and vehicles. Existing trips can remain unassigned.
ALTER TABLE public.trips
    ADD COLUMN vehicle_id BIGINT,
    ADD CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES public.vehicles(id);

CREATE INDEX idx_trips_vehicle ON public.trips(vehicle_id);
