-- Run once against smart_shipment_tracking before restarting.
-- Existing shipments have NULL deadlines and are excluded from the overdue report.
ALTER TABLE public.shipments
    ADD COLUMN expected_delivery_at TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX idx_shipments_expected_delivery
    ON public.shipments(expected_delivery_at, id);
